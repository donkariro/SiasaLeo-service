-- All fixtures roll back. Apply after V1-V45 in a disposable database.
BEGIN;
CREATE FUNCTION pg_temp.expect_failure(statement TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    BEGIN EXECUTE statement; EXCEPTION WHEN OTHERS THEN RETURN; END;
    RAISE EXCEPTION 'Expected rejection: %',statement;
END $$;
DO $$
DECLARE
    geo BIGINT; draft_geo BIGINT; node BIGINT; parent BIGINT; country BIGINT; station BIGINT; station2 BIGINT;
    center BIGINT; center_path TEXT; path TEXT:='/'; t RECORD;
    past_event BIGINT; current_event BIGINT; future_event BIGINT; race BIGINT; future_race BIGINT;
    register_id BIGINT; official BIGINT; provisional BIGINT; revision BIGINT; future_publication BIGINT;
    candidate_a BIGINT; candidate_b BIGINT; future_candidate BIGINT; person_a BIGINT; person_b BIGINT;
    station_type BIGINT; country_type BIGINT; assignment BIGINT;
BEGIN
    ASSERT NOT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name LIKE 'historical_%'), 'No separate historical tables';
    INSERT INTO electoral_geography_snapshot(name,source_reference,source_sha256) VALUES('Election geography','fixture',repeat('a',64)) RETURNING id INTO geo;
    FOR t IN SELECT * FROM area_type ORDER BY id LOOP
        INSERT INTO electoral_area_snapshot(snapshot_id,area_type_id,name,area_code,parent_id,ancestor_path)
            VALUES(geo,t.id,t.name,'1',parent,path) RETURNING id INTO node;
        IF t.name='COUNTRY' THEN country:=node;country_type:=t.id;END IF;
        IF t.name='REGISTRATION_CENTER' THEN center:=node;center_path:=path;END IF;
        IF t.name='POLLING_STATION' THEN station:=node;station_type:=t.id;END IF;
        parent:=node;path:=path||node||'/';
    END LOOP;
    INSERT INTO electoral_area_snapshot(snapshot_id,area_type_id,name,area_code,parent_id,ancestor_path)
        VALUES(geo,station_type,'Second station','2',center,center_path||center||'/') RETURNING id INTO station2;
    UPDATE electoral_geography_snapshot SET status='PUBLISHED',published_at=now() WHERE id=geo;
    INSERT INTO electoral_geography_snapshot(name) VALUES('Draft') RETURNING id INTO draft_geo;
    PERFORM pg_temp.expect_failure(format('INSERT INTO election_event(election_cycle_id,election_date,type,status,geography_snapshot_id) VALUES(1,''2013-03-04'',1,3,%s)',draft_geo));
    INSERT INTO election_event(election_cycle_id,election_date,type,status,geography_snapshot_id) VALUES(1,'2013-03-04',1,3,geo) RETURNING id INTO past_event;
    INSERT INTO election_event(election_cycle_id,election_date,type,status,geography_snapshot_id) VALUES(3,'2022-08-09',1,3,geo) RETURNING id INTO current_event;
    INSERT INTO election_event(election_cycle_id,election_date,type,status,geography_snapshot_id) VALUES(4,'2027-08-10',1,1,geo) RETURNING id INTO future_event;
    INSERT INTO contest(election_event_id,office_id,geography_snapshot_id,jurisdiction_id) VALUES(past_event,1,geo,country) RETURNING id INTO race;
    INSERT INTO contest(election_event_id,office_id,geography_snapshot_id,jurisdiction_id) VALUES(future_event,1,geo,country) RETURNING id INTO future_race;
    PERFORM pg_temp.expect_failure(format('UPDATE election_event SET geography_snapshot_id=NULL WHERE id=%s',past_event));
    PERFORM pg_temp.expect_failure(format('UPDATE contest SET jurisdiction_id=%s WHERE id=%s',station,race));
    PERFORM pg_temp.expect_failure(format('INSERT INTO contest(election_event_id,office_id,geography_snapshot_id,jurisdiction_id) VALUES(%s,4,%s,%s)',past_event,geo,country));

    INSERT INTO voter_register(geography_snapshot_id,scope_area_id,area_type_id,source_reference,source_sha256,coverage,import_fingerprint)
        VALUES(geo,country,station_type,'register',repeat('b',64),'COMPLETE',repeat('b',64)) RETURNING id INTO register_id;
    INSERT INTO voter_register_count VALUES(register_id,station,100,'row1');
    ASSERT EXISTS(SELECT 1 FROM voter_register_errors(register_id)), 'Missing counts cannot publish as complete';
    PERFORM pg_temp.expect_failure(format('UPDATE voter_register SET status=''PUBLISHED'',published_at=now() WHERE id=%s',register_id));
    PERFORM pg_temp.expect_failure(format('INSERT INTO voter_register_count VALUES(%s,%s,100,NULL)',register_id,country));
    INSERT INTO voter_register_count VALUES(register_id,station2,200,'row2');
    UPDATE voter_register SET status='PUBLISHED',published_at=now() WHERE id=register_id;
    PERFORM pg_temp.expect_failure(format('UPDATE voter_register_count SET registered_voters=999 WHERE register_id=%s',register_id));
    PERFORM pg_temp.expect_failure(format('DELETE FROM voter_register_count WHERE register_id=%s',register_id));
    INSERT INTO election_register_assignment(election_event_id,register_id,source_reference) VALUES(past_event,register_id,'fixture') RETURNING id INTO assignment;
    INSERT INTO election_register_assignment(election_event_id,register_id,source_reference) VALUES(current_event,register_id,'fixture'),(future_event,register_id,'fixture');
    PERFORM pg_temp.expect_failure(format('DELETE FROM election_register_assignment WHERE id=%s',assignment));

    INSERT INTO party(party_type) VALUES('PERSON') RETURNING id INTO person_a;
    INSERT INTO person(id,first_name,last_name) VALUES(person_a,'Candidate','A');
    INSERT INTO party(party_type) VALUES('PERSON') RETURNING id INTO person_b;
    INSERT INTO person(id,first_name,last_name) VALUES(person_b,'Candidate','B');
    INSERT INTO candidacy(person_id,contest_id,status,ballot_name) VALUES(person_a,race,9,'Candidate A') RETURNING id INTO candidate_a;
    INSERT INTO candidacy(person_id,contest_id,status,ballot_name) VALUES(person_b,race,9,'Candidate B') RETURNING id INTO candidate_b;
    INSERT INTO candidacy(person_id,contest_id,status,ballot_name) VALUES(person_a,future_race,1,'Candidate A') RETURNING id INTO future_candidate;

    INSERT INTO result_publication(contest_id,geography_snapshot_id,scope_area_id,area_type_id,stage,source_reference,source_sha256,coverage,import_fingerprint)
        VALUES(race,geo,country,station_type,'OFFICIAL','official',repeat('c',64),'COMPLETE',repeat('c',64)) RETURNING id INTO official;
    INSERT INTO result_candidate VALUES(official,candidate_a,'Candidate A',NULL,true),(official,candidate_b,'Candidate B',NULL,true);
    PERFORM pg_temp.expect_failure(format('INSERT INTO result_candidate VALUES(%s,%s,''Wrong contest'',NULL,true)',official,future_candidate));
    INSERT INTO contest_result(publication_id,candidacy_id,area_id,vote_count) VALUES(official,candidate_a,station,60),(official,candidate_b,station,30),(official,candidate_a,station2,100);
    ASSERT EXISTS(SELECT 1 FROM result_publication_errors(official)), 'Candidate matrix must be complete';
    INSERT INTO contest_result(publication_id,candidacy_id,area_id,vote_count) VALUES(official,candidate_b,station2,80);
    INSERT INTO result_ballot VALUES(official,station,91,1,NULL),(official,station2,180,0,NULL);
    ASSERT NOT EXISTS(SELECT 1 FROM result_publication_errors(official)), 'Reconciled data is valid';
    UPDATE result_ballot SET ballots_cast=92 WHERE publication_id=official AND area_id=station;
    ASSERT EXISTS(SELECT 1 FROM result_publication_errors(official)), 'Bad ballot sum must fail';
    UPDATE result_ballot SET ballots_cast=91 WHERE publication_id=official AND area_id=station;
    UPDATE result_publication SET status='PUBLISHED',published_at=now() WHERE id=official;
    PERFORM pg_temp.expect_failure(format('UPDATE contest_result SET vote_count=0 WHERE publication_id=%s',official));
    PERFORM pg_temp.expect_failure(format('UPDATE contest_result SET publication_id=NULL,area_id=NULL,polling_station_id=8 WHERE publication_id=%s',official));
    PERFORM pg_temp.expect_failure(format('UPDATE result_candidate SET candidate_name=''Changed'' WHERE publication_id=%s',official));
    UPDATE person SET first_name='Later profile name' WHERE id=person_a;
    UPDATE candidacy SET ballot_name='Corrected ballot label' WHERE id=candidate_a;
    ASSERT (SELECT candidate_name FROM result_candidate WHERE publication_id=official AND candidacy_id=candidate_a)='Candidate A', 'Publication labels are frozen';

    INSERT INTO result_publication(contest_id,geography_snapshot_id,scope_area_id,area_type_id,stage,source_reference,source_sha256,coverage,import_fingerprint)
        VALUES(race,geo,country,country_type,'PROVISIONAL','provisional',repeat('d',64),'COMPLETE',repeat('d',64)) RETURNING id INTO provisional;
    INSERT INTO result_candidate VALUES(provisional,candidate_a,'Candidate A',NULL,true);
    INSERT INTO contest_result(publication_id,candidacy_id,area_id,vote_count,stage) VALUES(provisional,candidate_a,country,250,'PROVISIONAL');
    UPDATE result_publication SET status='PUBLISHED',published_at=now() WHERE id=provisional;
    ASSERT (SELECT total_votes FROM contest_result_summary WHERE contest_id=race AND candidacy_id=candidate_a)=160, 'Official view excludes provisional votes';
    ASSERT (SELECT total_votes FROM provisional_contest_result_summary WHERE contest_id=race AND candidacy_id=candidate_a)=250, 'Provisional view uses same core storage';

    INSERT INTO result_publication(contest_id,geography_snapshot_id,scope_area_id,area_type_id,stage,source_reference,source_sha256,coverage,supersedes_id,import_fingerprint)
        VALUES(race,geo,country,country_type,'OFFICIAL','correction',repeat('e',64),'COMPLETE',official,repeat('e',64)) RETURNING id INTO revision;
    INSERT INTO result_candidate VALUES(revision,candidate_a,'Corrected ballot label',NULL,true),(revision,candidate_b,'Candidate B',NULL,true);
    INSERT INTO contest_result(publication_id,candidacy_id,area_id,vote_count) VALUES(revision,candidate_a,country,161),(revision,candidate_b,country,109);
    UPDATE result_publication SET status='PUBLISHED',published_at=now() WHERE id=revision;
    ASSERT (SELECT sum(vote_count) FROM contest_result WHERE publication_id=official)=270, 'Original result remains';
    ASSERT (SELECT total_votes FROM contest_result_summary WHERE contest_id=race AND candidacy_id=candidate_a)=161, 'Summary selects one revision, not their sum';
    ASSERT (SELECT total_votes FROM provisional_contest_result_summary WHERE contest_id=race AND candidacy_id=candidate_a)=250, 'Official correction leaves provisional version intact';

    INSERT INTO result_publication(contest_id,geography_snapshot_id,scope_area_id,area_type_id,stage,source_reference,source_sha256,coverage,import_fingerprint)
        VALUES(future_race,geo,country,country_type,'PROVISIONAL','future fixture',repeat('f',64),'COMPLETE',repeat('f',64)) RETURNING id INTO future_publication;
    INSERT INTO result_candidate VALUES(future_publication,future_candidate,'Candidate A',NULL,true);
    INSERT INTO contest_result(publication_id,candidacy_id,area_id,vote_count,stage) VALUES(future_publication,future_candidate,country,1,'PROVISIONAL');
    UPDATE result_publication SET status='PUBLISHED',published_at=now() WHERE id=future_publication;
    ASSERT EXISTS(SELECT 1 FROM contest_result WHERE publication_id=future_publication), 'Future event uses the same result table and publication lifecycle';
    ASSERT (SELECT count(*) FROM voter_registration)=0, 'Official register capture does not create personal registrations';
END $$;
ROLLBACK;
