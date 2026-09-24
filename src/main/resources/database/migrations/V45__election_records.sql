-- Additive: legacy results and self-declared registrations are not reclassified.
ALTER TABLE election_event ADD COLUMN geography_snapshot_id BIGINT REFERENCES electoral_geography_snapshot(id);
ALTER TABLE election_event ADD COLUMN source_reference VARCHAR(2048);
ALTER TABLE election_event ADD UNIQUE (id, geography_snapshot_id);
ALTER TABLE contest ALTER COLUMN seat_id DROP NOT NULL;
ALTER TABLE contest ADD COLUMN office_id BIGINT REFERENCES office(id);
ALTER TABLE contest ADD COLUMN geography_snapshot_id BIGINT;
ALTER TABLE contest ADD COLUMN jurisdiction_id BIGINT;
UPDATE contest c SET office_id = s.office_id FROM seat s WHERE s.id = c.seat_id;
ALTER TABLE contest ADD FOREIGN KEY (election_event_id, geography_snapshot_id) REFERENCES election_event(id, geography_snapshot_id);
ALTER TABLE contest ADD FOREIGN KEY (geography_snapshot_id, jurisdiction_id) REFERENCES electoral_area_snapshot(snapshot_id, id);
ALTER TABLE contest ADD CHECK ((geography_snapshot_id IS NULL) = (jurisdiction_id IS NULL));
ALTER TABLE contest ADD CHECK (seat_id IS NOT NULL OR (office_id IS NOT NULL AND jurisdiction_id IS NOT NULL));
CREATE UNIQUE INDEX uq_contest_jurisdiction ON contest(election_event_id, office_id, jurisdiction_id) WHERE jurisdiction_id IS NOT NULL;


-- Register editions and result publications are distinct business aggregates.
CREATE TABLE voter_register (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    geography_snapshot_id BIGINT NOT NULL REFERENCES electoral_geography_snapshot(id),
    scope_area_id BIGINT NOT NULL,
    area_type_id BIGINT NOT NULL REFERENCES area_type(id),
    source_reference VARCHAR(2048) NOT NULL CHECK (btrim(source_reference) <> ''),
    source_sha256 VARCHAR(64) NOT NULL CHECK (source_sha256 ~ '^[0-9a-f]{64}$'),
    import_fingerprint VARCHAR(64) NOT NULL,
    coverage VARCHAR(10) NOT NULL CHECK (coverage IN ('PARTIAL','COMPLETE')),
    supersedes_id BIGINT REFERENCES voter_register(id),
    status VARCHAR(10) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    FOREIGN KEY (geography_snapshot_id,scope_area_id) REFERENCES electoral_area_snapshot(snapshot_id,id),
    CHECK ((status='PUBLISHED') = (published_at IS NOT NULL)),
    CHECK (supersedes_id IS DISTINCT FROM id),
    UNIQUE(geography_snapshot_id,scope_area_id,area_type_id,source_sha256)
);
CREATE UNIQUE INDEX uq_register_successor ON voter_register(supersedes_id) WHERE status='PUBLISHED';
CREATE TABLE voter_register_count (
    register_id BIGINT NOT NULL REFERENCES voter_register(id),
    area_id BIGINT NOT NULL REFERENCES electoral_area_snapshot(id),
    registered_voters BIGINT NOT NULL CHECK (registered_voters >= 0),
    source_record_reference VARCHAR(1024),
    PRIMARY KEY(register_id,area_id)
);
CREATE TABLE voter_register_import_batch (
    register_id BIGINT NOT NULL REFERENCES voter_register(id),
    fingerprint VARCHAR(64) NOT NULL,
    PRIMARY KEY(register_id,fingerprint)
);
CREATE TABLE election_register_assignment (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    election_event_id BIGINT NOT NULL REFERENCES election_event(id),
    register_id BIGINT NOT NULL REFERENCES voter_register(id),
    supersedes_id BIGINT REFERENCES election_register_assignment(id),
    source_reference VARCHAR(2048) NOT NULL CHECK (btrim(source_reference) <> ''),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(supersedes_id)
);
CREATE UNIQUE INDEX uq_register_assignment_root ON election_register_assignment(election_event_id) WHERE supersedes_id IS NULL;

-- Ballot identity belongs to the core candidacy, regardless of election date.
ALTER TABLE candidacy ADD COLUMN ballot_name VARCHAR(255);
ALTER TABLE candidacy ADD COLUMN ballot_party_name VARCHAR(255);
ALTER TABLE candidacy ADD COLUMN source_reference VARCHAR(2048);
ALTER TABLE candidacy ADD COLUMN source_record_reference VARCHAR(1024);
ALTER TABLE candidacy ADD COLUMN import_fingerprint VARCHAR(64);
CREATE UNIQUE INDEX uq_candidacy_source ON candidacy(contest_id,source_reference,source_record_reference)
    WHERE source_record_reference IS NOT NULL;
-- Existing names/affiliations require verification; do not infer past ballot labels.

CREATE TABLE result_publication (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contest_id BIGINT NOT NULL REFERENCES contest(id),
    geography_snapshot_id BIGINT NOT NULL REFERENCES electoral_geography_snapshot(id),
    scope_area_id BIGINT NOT NULL,
    area_type_id BIGINT NOT NULL REFERENCES area_type(id),
    stage VARCHAR(12) NOT NULL CHECK (stage IN ('PROVISIONAL','OFFICIAL')),
    source_reference VARCHAR(2048) NOT NULL CHECK (btrim(source_reference) <> ''),
    source_sha256 VARCHAR(64) NOT NULL CHECK (source_sha256 ~ '^[0-9a-f]{64}$'),
    import_fingerprint VARCHAR(64) NOT NULL,
    coverage VARCHAR(10) NOT NULL CHECK (coverage IN ('PARTIAL','COMPLETE')),
    supersedes_id BIGINT REFERENCES result_publication(id),
    status VARCHAR(10) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    FOREIGN KEY (geography_snapshot_id,scope_area_id) REFERENCES electoral_area_snapshot(snapshot_id,id),
    CHECK ((status='PUBLISHED') = (published_at IS NOT NULL)),
    CHECK (supersedes_id IS DISTINCT FROM id),
    UNIQUE(contest_id,stage,area_type_id,source_sha256)
);
CREATE UNIQUE INDEX uq_result_root ON result_publication(contest_id,stage) WHERE supersedes_id IS NULL AND status='PUBLISHED';
CREATE UNIQUE INDEX uq_result_successor ON result_publication(supersedes_id) WHERE status='PUBLISHED';
CREATE INDEX idx_result_publication_contest ON result_publication(contest_id,id);
-- A publication roster references real candidacies and freezes their ballot labels.
CREATE TABLE result_candidate (
    publication_id BIGINT NOT NULL REFERENCES result_publication(id),
    candidacy_id BIGINT NOT NULL REFERENCES candidacy(id),
    candidate_name VARCHAR(255) NOT NULL CHECK (btrim(candidate_name) <> ''),
    party_name VARCHAR(255),
    independent BOOLEAN NOT NULL,
    PRIMARY KEY(publication_id,candidacy_id),
    CHECK ((independent AND party_name IS NULL) OR (NOT independent AND party_name IS NOT NULL AND btrim(party_name) <> ''))
);
CREATE INDEX idx_result_candidate_candidacy ON result_candidate(candidacy_id);

-- Evolve the existing result table; preserve unmapped records without guessing geography.
DROP VIEW officeholder_mandate;
DROP VIEW contest_result_summary;
DROP VIEW provisional_contest_result_summary;
ALTER TABLE contest_result DROP CONSTRAINT contest_result_candidacy_id_polling_station_id_key;
ALTER TABLE contest_result ALTER COLUMN polling_station_id DROP NOT NULL;
ALTER TABLE contest_result ALTER COLUMN vote_count TYPE BIGINT;
ALTER TABLE contest_result ADD COLUMN publication_id BIGINT REFERENCES result_publication(id);
ALTER TABLE contest_result ADD COLUMN area_id BIGINT REFERENCES electoral_area_snapshot(id);
ALTER TABLE contest_result ADD COLUMN stage VARCHAR(12) NOT NULL DEFAULT 'OFFICIAL' CHECK (stage IN ('PROVISIONAL','OFFICIAL'));
ALTER TABLE contest_result ADD COLUMN recorded_at TIMESTAMPTZ;
ALTER TABLE contest_result ADD COLUMN source_record_reference VARCHAR(1024);
ALTER TABLE contest_result ADD FOREIGN KEY(publication_id,candidacy_id) REFERENCES result_candidate(publication_id,candidacy_id);
ALTER TABLE contest_result ADD CHECK (
    (publication_id IS NULL AND area_id IS NULL AND polling_station_id IS NOT NULL) OR
    (publication_id IS NOT NULL AND area_id IS NOT NULL AND polling_station_id IS NULL));
CREATE UNIQUE INDEX uq_result_legacy_station ON contest_result(candidacy_id,polling_station_id,stage) WHERE publication_id IS NULL;
CREATE UNIQUE INDEX uq_result_publication_area ON contest_result(publication_id,candidacy_id,area_id) WHERE publication_id IS NOT NULL;
CREATE INDEX idx_result_area ON contest_result(publication_id,area_id);
-- Retain provisional values and timestamps in the same core result table.
INSERT INTO contest_result(candidacy_id,polling_station_id,vote_count,stage,recorded_at,source_record_reference)
    SELECT candidacy_id,polling_station_id,vote_count,'PROVISIONAL',recorded_at,'provisional_contest_result:' || id
    FROM provisional_contest_result;
DROP TABLE provisional_contest_result;
-- Compatibility read projection; all new writes use contest_result/publications.
CREATE VIEW provisional_contest_result AS
    SELECT id,candidacy_id,polling_station_id,vote_count,recorded_at FROM contest_result
    WHERE stage='PROVISIONAL' AND publication_id IS NULL;

CREATE TABLE result_ballot (
    publication_id BIGINT NOT NULL REFERENCES result_publication(id),
    area_id BIGINT NOT NULL REFERENCES electoral_area_snapshot(id),
    ballots_cast BIGINT NOT NULL CHECK (ballots_cast >= 0),
    rejected_ballots BIGINT CHECK (rejected_ballots >= 0 AND rejected_ballots <= ballots_cast),
    source_record_reference VARCHAR(1024), PRIMARY KEY(publication_id,area_id)
);
CREATE TABLE result_import_batch (
    publication_id BIGINT NOT NULL REFERENCES result_publication(id),
    fingerprint VARCHAR(64) NOT NULL, PRIMARY KEY(publication_id,fingerprint)
);
CREATE FUNCTION area_in_snapshot_scope(area BIGINT, scope BIGINT) RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
    SELECT EXISTS (SELECT 1 FROM electoral_area_snapshot a JOIN electoral_area_snapshot s ON s.id=scope
        WHERE a.id=area AND a.snapshot_id=s.snapshot_id AND
        (a.id=s.id OR a.ancestor_path LIKE s.ancestor_path || s.id || '/%'))
$$;

CREATE FUNCTION guard_event() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.geography_snapshot_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM electoral_geography_snapshot WHERE id = NEW.geography_snapshot_id AND status = 'PUBLISHED'
    ) THEN RAISE EXCEPTION 'Election geography must be published'; END IF;
    IF TG_OP = 'UPDATE' AND OLD.geography_snapshot_id IS NOT NULL AND
       (NEW.geography_snapshot_id IS DISTINCT FROM OLD.geography_snapshot_id OR
        NEW.election_date IS DISTINCT FROM OLD.election_date OR NEW.election_cycle_id IS DISTINCT FROM OLD.election_cycle_id OR
        NEW.type IS DISTINCT FROM OLD.type OR NEW.source_reference IS DISTINCT FROM OLD.source_reference) THEN
        RAISE EXCEPTION 'Election election identity and geography are fixed';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_event BEFORE INSERT OR UPDATE ON election_event FOR EACH ROW EXECUTE FUNCTION guard_event();

CREATE FUNCTION guard_contest() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE expected_type TEXT; actual_type TEXT;
BEGIN
    IF NEW.office_id IS NULL AND NEW.seat_id IS NOT NULL THEN
        SELECT office_id INTO NEW.office_id FROM seat WHERE id = NEW.seat_id;
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.jurisdiction_id IS NOT NULL AND
        (NEW.election_event_id IS DISTINCT FROM OLD.election_event_id OR NEW.office_id IS DISTINCT FROM OLD.office_id OR
         NEW.geography_snapshot_id IS DISTINCT FROM OLD.geography_snapshot_id OR NEW.jurisdiction_id IS DISTINCT FROM OLD.jurisdiction_id OR
         NEW.seat_id IS DISTINCT FROM OLD.seat_id) THEN RAISE EXCEPTION 'Election contest identity is fixed'; END IF;
    IF NEW.jurisdiction_id IS NOT NULL THEN
        SELECT CASE abbreviation WHEN 'PRES' THEN 'COUNTRY' WHEN 'DP' THEN 'COUNTRY'
            WHEN 'SEN' THEN 'COUNTY' WHEN 'WR' THEN 'COUNTY' WHEN 'GOV' THEN 'COUNTY' WHEN 'DG' THEN 'COUNTY'
            WHEN 'MP' THEN 'CONSTITUENCY' WHEN 'MCA' THEN 'WARD' END INTO expected_type FROM office WHERE id = NEW.office_id;
        SELECT t.name INTO actual_type FROM electoral_area_snapshot a JOIN area_type t ON t.id = a.area_type_id WHERE a.id = NEW.jurisdiction_id;
        IF expected_type IS NULL OR expected_type IS DISTINCT FROM actual_type THEN RAISE EXCEPTION 'Office and election-time jurisdiction type do not match'; END IF;
        IF NEW.seat_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM seat s JOIN electoral_area_correspondence c ON c.electoral_area_id = s.electoral_area_id
            WHERE s.id = NEW.seat_id AND s.office_id = NEW.office_id AND c.area_snapshot_id = NEW.jurisdiction_id AND c.reviewed_at IS NOT NULL
        ) THEN RAISE EXCEPTION 'Seat needs a reviewed election-time correspondence'; END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_contest BEFORE INSERT OR UPDATE ON contest FOR EACH ROW EXECUTE FUNCTION guard_contest();

CREATE FUNCTION guard_office() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.name IS DISTINCT FROM OLD.name OR NEW.abbreviation IS DISTINCT FROM OLD.abbreviation OR NEW.level IS DISTINCT FROM OLD.level)
       AND EXISTS (SELECT 1 FROM contest WHERE office_id=OLD.id AND jurisdiction_id IS NOT NULL) THEN
        RAISE EXCEPTION 'Cannot redefine an office used by a election-time contest'; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_office BEFORE INSERT OR UPDATE ON office FOR EACH ROW EXECUTE FUNCTION guard_office();

CREATE FUNCTION voter_register_errors(rid BIGINT) RETURNS TABLE(error TEXT) LANGUAGE plpgsql AS $$
DECLARE r voter_register; actual BIGINT; expected BIGINT;
BEGIN
    SELECT * INTO r FROM voter_register WHERE id=rid;
    IF NOT FOUND THEN RETURN QUERY SELECT 'Register not found'::TEXT; RETURN; END IF;
    SELECT count(*) INTO expected FROM electoral_area_snapshot a WHERE a.snapshot_id=r.geography_snapshot_id
        AND a.area_type_id=r.area_type_id AND area_in_snapshot_scope(a.id,r.scope_area_id);
    SELECT count(*) INTO actual FROM voter_register_count WHERE register_id=rid;
    IF actual=0 THEN RETURN QUERY SELECT 'At least one reported area is required'::TEXT; END IF;
    IF r.coverage='COMPLETE' AND actual<>expected THEN RETURN QUERY SELECT 'Complete coverage requires every area at the declared granularity'::TEXT; END IF;
END $$;
CREATE FUNCTION result_publication_errors(pid BIGINT) RETURNS TABLE(error TEXT) LANGUAGE plpgsql AS $$
DECLARE p result_publication; actual BIGINT; expected BIGINT; candidates BIGINT;
BEGIN
    SELECT * INTO p FROM result_publication WHERE id=pid;
    IF NOT FOUND THEN RETURN QUERY SELECT 'Result publication not found'::TEXT; RETURN; END IF;
    SELECT count(*) INTO expected FROM electoral_area_snapshot a WHERE a.snapshot_id=p.geography_snapshot_id
        AND a.area_type_id=p.area_type_id AND area_in_snapshot_scope(a.id,p.scope_area_id);
    SELECT count(DISTINCT area_id) INTO actual FROM contest_result WHERE publication_id=pid;
    SELECT count(*) INTO candidates FROM result_candidate WHERE publication_id=pid;
    IF candidates=0 THEN RETURN QUERY SELECT 'At least one candidacy is required'::TEXT; END IF;
    IF EXISTS (SELECT area_id FROM contest_result WHERE publication_id=pid GROUP BY area_id HAVING count(*)<>candidates)
        THEN RETURN QUERY SELECT 'Every reported area needs an explicit vote count for every candidacy'::TEXT; END IF;
    IF EXISTS (SELECT 1 FROM result_ballot b LEFT JOIN
        (SELECT area_id,sum(vote_count) votes FROM contest_result WHERE publication_id=pid GROUP BY area_id) v USING(area_id)
        WHERE b.publication_id=pid AND (v.area_id IS NULL OR v.votes>b.ballots_cast OR
            (b.rejected_ballots IS NOT NULL AND v.votes+b.rejected_ballots<>b.ballots_cast)))
        THEN RETURN QUERY SELECT 'Ballot totals do not reconcile with candidate votes'::TEXT; END IF;
    IF actual=0 THEN RETURN QUERY SELECT 'At least one reported area is required'::TEXT; END IF;
    IF p.coverage='COMPLETE' AND actual<>expected THEN RETURN QUERY SELECT 'Complete coverage requires every area at the declared granularity'::TEXT; END IF;
END $$;

CREATE FUNCTION guard_voter_register() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE predecessor voter_register; validation_error TEXT;
BEGIN
    IF TG_OP <> 'INSERT' AND OLD.status = 'PUBLISHED' THEN RAISE EXCEPTION 'Published records is immutable'; END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    IF TG_OP = 'INSERT' AND NEW.status <> 'DRAFT' THEN RAISE EXCEPTION 'Import a draft before publishing'; END IF;
    IF TG_OP = 'UPDATE' AND (to_jsonb(NEW) - 'status' - 'published_at') IS DISTINCT FROM (to_jsonb(OLD) - 'status' - 'published_at') THEN
        RAISE EXCEPTION 'Publication metadata is fixed; replace the draft or create a revision'; END IF;
    IF NOT EXISTS (SELECT 1 FROM electoral_geography_snapshot WHERE id = NEW.geography_snapshot_id AND status = 'PUBLISHED')
        THEN RAISE EXCEPTION 'Published data requires published geography'; END IF;
    IF NOT EXISTS (SELECT 1 FROM electoral_area_snapshot WHERE snapshot_id = NEW.geography_snapshot_id AND area_type_id = NEW.area_type_id
        AND area_in_snapshot_scope(id,NEW.scope_area_id)) THEN RAISE EXCEPTION 'No areas at requested granularity within scope'; END IF;
    IF NEW.supersedes_id IS NOT NULL THEN
        SELECT * INTO predecessor FROM voter_register WHERE id = NEW.supersedes_id FOR UPDATE;
        IF predecessor.status IS DISTINCT FROM 'PUBLISHED' OR 
            predecessor.geography_snapshot_id <> NEW.geography_snapshot_id OR predecessor.scope_area_id <> NEW.scope_area_id THEN RAISE EXCEPTION 'Revision must replace a published dataset in the same context'; END IF;
    END IF;
    IF NEW.status = 'PUBLISHED' THEN
        SELECT error INTO validation_error FROM voter_register_errors(NEW.id) LIMIT 1;
        IF validation_error IS NOT NULL THEN RAISE EXCEPTION '%', validation_error; END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_voter_register BEFORE INSERT OR UPDATE OR DELETE ON voter_register FOR EACH ROW EXECUTE FUNCTION guard_voter_register();

CREATE FUNCTION guard_result_publication() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE predecessor result_publication; validation_error TEXT;
BEGIN
    IF TG_OP <> 'INSERT' AND OLD.status = 'PUBLISHED' THEN RAISE EXCEPTION 'Published records is immutable'; END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    IF TG_OP = 'INSERT' AND NEW.status <> 'DRAFT' THEN RAISE EXCEPTION 'Import a draft before publishing'; END IF;
    IF TG_OP = 'UPDATE' AND (to_jsonb(NEW) - 'status' - 'published_at') IS DISTINCT FROM (to_jsonb(OLD) - 'status' - 'published_at') THEN
        RAISE EXCEPTION 'Publication metadata is fixed; replace the draft or create a revision'; END IF;
    IF NOT EXISTS (SELECT 1 FROM electoral_geography_snapshot WHERE id = NEW.geography_snapshot_id AND status = 'PUBLISHED')
        THEN RAISE EXCEPTION 'Published data requires published geography'; END IF;
    IF NOT EXISTS (SELECT 1 FROM electoral_area_snapshot WHERE snapshot_id = NEW.geography_snapshot_id AND area_type_id = NEW.area_type_id
        AND area_in_snapshot_scope(id,NEW.scope_area_id)) THEN RAISE EXCEPTION 'No areas at requested granularity within scope'; END IF;
    IF NOT EXISTS (SELECT 1 FROM contest WHERE id = NEW.contest_id
        AND geography_snapshot_id = NEW.geography_snapshot_id AND jurisdiction_id = NEW.scope_area_id)
        THEN RAISE EXCEPTION 'Results must use their contest jurisdiction'; END IF;
    IF NEW.supersedes_id IS NOT NULL THEN
        SELECT * INTO predecessor FROM result_publication WHERE id = NEW.supersedes_id FOR UPDATE;
        IF predecessor.status IS DISTINCT FROM 'PUBLISHED' OR predecessor.stage <> NEW.stage OR
            predecessor.geography_snapshot_id <> NEW.geography_snapshot_id OR predecessor.scope_area_id <> NEW.scope_area_id OR
            predecessor.contest_id IS DISTINCT FROM NEW.contest_id THEN RAISE EXCEPTION 'Revision must replace a published dataset in the same context'; END IF;
    END IF;
    IF NEW.status = 'PUBLISHED' THEN
        SELECT error INTO validation_error FROM result_publication_errors(NEW.id) LIMIT 1;
        IF validation_error IS NOT NULL THEN RAISE EXCEPTION '%', validation_error; END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_result_publication BEFORE INSERT OR UPDATE OR DELETE ON result_publication FOR EACH ROW EXECUTE FUNCTION guard_result_publication();

CREATE FUNCTION guard_register_fact() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE r voter_register; target BIGINT;
BEGIN
    target:=CASE WHEN TG_OP='DELETE' THEN OLD.register_id ELSE NEW.register_id END;
    SELECT * INTO r FROM voter_register WHERE id=target FOR UPDATE;
    IF r.status IS DISTINCT FROM 'DRAFT' THEN RAISE EXCEPTION 'Only draft register facts can change'; END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; END IF;
    IF TG_OP='UPDATE' AND NEW.register_id<>OLD.register_id THEN RAISE EXCEPTION 'Cannot move register facts'; END IF;
    IF TG_TABLE_NAME='voter_register_count' THEN
        IF NOT EXISTS (SELECT 1 FROM electoral_area_snapshot WHERE id=NEW.area_id AND snapshot_id=r.geography_snapshot_id AND area_type_id=r.area_type_id)
            OR NOT area_in_snapshot_scope(NEW.area_id,r.scope_area_id) THEN RAISE EXCEPTION 'Register area outside geography, granularity or scope'; END IF;
    ELSIF TG_OP='UPDATE' THEN RAISE EXCEPTION 'Import receipts are immutable'; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_register_count BEFORE INSERT OR UPDATE OR DELETE ON voter_register_count FOR EACH ROW EXECUTE FUNCTION guard_register_fact();
CREATE TRIGGER guard_register_batch BEFORE INSERT OR UPDATE OR DELETE ON voter_register_import_batch FOR EACH ROW EXECUTE FUNCTION guard_register_fact();

CREATE FUNCTION guard_result_fact() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE p result_publication; target BIGINT;
BEGIN
    target:=CASE WHEN TG_OP='DELETE' THEN OLD.publication_id ELSE NEW.publication_id END;
    IF TG_TABLE_NAME='contest_result' AND target IS NULL THEN
        IF TG_OP='UPDATE' AND OLD.publication_id IS NOT NULL THEN RAISE EXCEPTION 'Cannot detach a result from its publication'; END IF;
        -- Only grandfathered rows can lack an explicit publication. They can be
        -- attached to a draft after verified geography mapping, never duplicated.
        IF TG_OP='INSERT' THEN RAISE EXCEPTION 'New results require a publication'; END IF;
        IF TG_OP='DELETE' THEN RETURN OLD; END IF;
        RETURN NEW;
    END IF;
    SELECT * INTO p FROM result_publication WHERE id=target FOR UPDATE;
    IF p.status IS DISTINCT FROM 'DRAFT' THEN RAISE EXCEPTION 'Only draft result facts can change'; END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; END IF;
    IF TG_OP='UPDATE' AND OLD.publication_id IS NOT NULL AND NEW.publication_id IS DISTINCT FROM OLD.publication_id THEN
        RAISE EXCEPTION 'Cannot move facts between result publications'; END IF;
    IF TG_TABLE_NAME='result_candidate' THEN
        IF NOT EXISTS (SELECT 1 FROM candidacy WHERE id=NEW.candidacy_id AND contest_id=p.contest_id)
            THEN RAISE EXCEPTION 'Candidacy belongs to another contest'; END IF;
    ELSIF TG_TABLE_NAME='result_import_batch' THEN
        IF TG_OP='UPDATE' THEN RAISE EXCEPTION 'Import receipts are immutable'; END IF;
    ELSE
        IF NOT EXISTS (SELECT 1 FROM electoral_area_snapshot WHERE id=NEW.area_id AND snapshot_id=p.geography_snapshot_id AND area_type_id=p.area_type_id)
            OR NOT area_in_snapshot_scope(NEW.area_id,p.scope_area_id) THEN RAISE EXCEPTION 'Result area outside geography, granularity or scope'; END IF;
        IF TG_TABLE_NAME='contest_result' THEN
            IF NEW.stage<>p.stage THEN RAISE EXCEPTION 'Result stage differs from publication'; END IF;
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_contest_result BEFORE INSERT OR UPDATE OR DELETE ON contest_result FOR EACH ROW EXECUTE FUNCTION guard_result_fact();
CREATE TRIGGER guard_result_candidate BEFORE INSERT OR UPDATE OR DELETE ON result_candidate FOR EACH ROW EXECUTE FUNCTION guard_result_fact();
CREATE TRIGGER guard_result_ballot BEFORE INSERT OR UPDATE OR DELETE ON result_ballot FOR EACH ROW EXECUTE FUNCTION guard_result_fact();
CREATE TRIGGER guard_result_batch BEFORE INSERT OR UPDATE OR DELETE ON result_import_batch FOR EACH ROW EXECUTE FUNCTION guard_result_fact();

CREATE FUNCTION guard_register_assignment() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE event_snapshot BIGINT;
BEGIN
    IF TG_OP <> 'INSERT' THEN RAISE EXCEPTION 'Register assignments are append-only'; END IF;
    SELECT geography_snapshot_id INTO event_snapshot FROM election_event WHERE id = NEW.election_event_id FOR UPDATE;
    IF NOT EXISTS (SELECT 1 FROM voter_register WHERE id = NEW.register_id
        AND status = 'PUBLISHED' AND geography_snapshot_id = event_snapshot) THEN RAISE EXCEPTION 'Register must be published in the election geography'; END IF;
    IF NEW.supersedes_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM election_register_assignment
        WHERE id = NEW.supersedes_id AND election_event_id = NEW.election_event_id)
        THEN RAISE EXCEPTION 'Assignment predecessor belongs to another election'; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_register_assignment BEFORE INSERT OR UPDATE OR DELETE ON election_register_assignment FOR EACH ROW EXECUTE FUNCTION guard_register_assignment();

CREATE FUNCTION guard_published_candidacy() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.contest_id IS DISTINCT FROM OLD.contest_id OR NEW.person_id IS DISTINCT FROM OLD.person_id)
       AND EXISTS (SELECT 1 FROM result_candidate WHERE candidacy_id=OLD.id) THEN
        RAISE EXCEPTION 'Cannot reassign a candidacy referenced by results'; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_published_candidacy BEFORE UPDATE ON candidacy FOR EACH ROW EXECUTE FUNCTION guard_published_candidacy();

-- Compatibility summaries choose exactly one published revision, falling back
-- to unmapped legacy data only when no publication exists for that contest/stage.
CREATE VIEW selected_contest_result AS
SELECT cr.* FROM contest_result cr JOIN candidacy ca ON ca.id=cr.candidacy_id
WHERE (cr.publication_id IN (SELECT p.id FROM result_publication p WHERE p.status='PUBLISHED'
    AND NOT EXISTS (SELECT 1 FROM result_publication n WHERE n.supersedes_id=p.id AND n.status='PUBLISHED')))
OR (cr.publication_id IS NULL AND NOT EXISTS (SELECT 1 FROM result_publication p
    WHERE p.contest_id=ca.contest_id AND p.stage=cr.stage AND p.status='PUBLISHED'));
CREATE VIEW contest_result_summary AS
SELECT ca.contest_id,cr.candidacy_id,SUM(cr.vote_count) AS total_votes,
    ROUND(100.0*SUM(cr.vote_count)/NULLIF(SUM(SUM(cr.vote_count)) OVER(PARTITION BY ca.contest_id),0),2) AS percentage,
    RANK() OVER(PARTITION BY ca.contest_id ORDER BY SUM(cr.vote_count) DESC) AS rank
FROM selected_contest_result cr JOIN candidacy ca ON ca.id=cr.candidacy_id
WHERE cr.stage='OFFICIAL' GROUP BY ca.contest_id,cr.candidacy_id;
CREATE VIEW provisional_contest_result_summary AS
SELECT ca.contest_id,cr.candidacy_id,SUM(cr.vote_count) AS total_votes,
    ROUND(100.0*SUM(cr.vote_count)/NULLIF(SUM(SUM(cr.vote_count)) OVER(PARTITION BY ca.contest_id),0),2) AS percentage,
    RANK() OVER(PARTITION BY ca.contest_id ORDER BY SUM(cr.vote_count) DESC) AS rank
FROM selected_contest_result cr JOIN candidacy ca ON ca.id=cr.candidacy_id
WHERE cr.stage='PROVISIONAL' GROUP BY ca.contest_id,cr.candidacy_id;
CREATE OR REPLACE VIEW officeholder_mandate AS
SELECT oh.id AS officeholder_id,
       oh.seat_id,
       oh.person_id,
       oh.term_id,
       oh.entry_reason_id,
       oh.start_date,
       oh.end_date,
       ca.contest_id,
       ca.political_party_id,
       s.total_votes,
       s.percentage,
       s.rank,
       oh.exit_reason_id
FROM officeholder oh
LEFT JOIN candidacy ca ON ca.id = oh.candidacy_id
LEFT JOIN contest_result_summary s ON s.candidacy_id = ca.id;

