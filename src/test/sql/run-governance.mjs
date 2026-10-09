import { PGlite } from '@electric-sql/pglite';
import { readFile, readdir } from 'node:fs/promises';
import assert from 'node:assert/strict';
const db=new PGlite();
let checks=0;
async function rejects(sql,code='23514') { await assert.rejects(db.exec(sql),e=>e.code===code,sql); checks++; }
async function scalar(sql) { return Object.values((await db.query(sql)).rows[0])[0]; }
try {
    const directory=new URL('../../main/resources/database/migrations/',import.meta.url);
    const migrations=(await readdir(directory)).filter(n=>/^V\d+__.*\.sql$/.test(n))
        .sort((a,b)=>Number(a.match(/^V(\d+)/)[1])-Number(b.match(/^V(\d+)/)[1]));
    for(const migration of migrations) {
        if(migration.startsWith('V50__')) await db.exec(`
            INSERT INTO party(id,party_type) OVERRIDING SYSTEM VALUE VALUES(-950,'PERSON'),(-951,'PERSON');
            INSERT INTO person(id,first_name,last_name) VALUES(-950,'Historical','Subject'),(-951,'Future','Candidate');
            INSERT INTO leadership_term(id,seat_id,start_date,end_date) OVERRIDING SYSTEM VALUE SELECT -950,min(id),'2000-01-01','2004-12-31' FROM seat;
            INSERT INTO officeholder(id,seat_id,person_id,term_id,entry_reason_id,exit_reason_id,start_date,end_date) OVERRIDING SYSTEM VALUE
              SELECT -950,seat_id,-950,id,3,1,'2000-01-01','2004-12-31' FROM leadership_term WHERE id=-950;
            INSERT INTO performance_record(id,officeholder_id,title,description,period_start,period_end,source_reference) OVERRIDING SYSTEM VALUE
              VALUES(-950,-950,'Legacy title','Legacy description','2001-01-01','2002-12-31','archive/legacy');
            INSERT INTO performance_indicator(id,code,name,unit,methodology,direction) OVERRIDING SYSTEM VALUE
              VALUES(-950,'LEGACY_METRIC','Legacy metric','custom unit','Count operational facilities','HIGHER_IS_BETTER');
            INSERT INTO performance_target(id,officeholder_id,indicator_id,period_start,period_end,target_value,source_reference) OVERRIDING SYSTEM VALUE
              VALUES(-950,-950,-950,'2001-01-01','2002-12-31',20,'archive/target');
            INSERT INTO performance_measurement(target_id,measured_on,actual_value,source_reference) VALUES(-950,'2002-12-31',12,'archive/measurement');
        `);
        let sql=await readFile(new URL(migration,directory),'utf8');
        if(migration.startsWith('V3__') && !process.argv.includes('--full')) {
            const rows=sql.split('\n').filter(l=>/^\([1-8], /.test(l)).map(l=>l.trim().replace(/,$/,''));
            sql='INSERT INTO electoral_area(id,name,area_code,area_type_id,parent_id,ancestor_path) OVERRIDING SYSTEM VALUE VALUES '+rows.join(',')+';';
        }
        await db.exec(sql);
    }
    assert.equal(await scalar("SELECT title FROM performance_record WHERE id=-950"),'Legacy title');
    assert.equal(await scalar("SELECT count(*)::int FROM performance_observation WHERE legacy_measurement_id IS NOT NULL AND actual_value=12"),1);
    assert.equal(await scalar("SELECT count(*)::int FROM user_account WHERE person_id=-950"),0);
    assert.equal(await scalar("SELECT count(*)::int FROM performance_indicator WHERE id=-950 AND unit_id IS NOT NULL"),1);
    await db.exec(`
        INSERT INTO user_account(id,person_id,email,password_hash,status) OVERRIDING SYSTEM VALUE
          VALUES(-950,NULL,'editor@example.test','unused','ACTIVE'),(-951,-951,'issuer@example.test','unused','ACTIVE');
        INSERT INTO digital_asset(id,title,asset_type,created_by_account_id,visibility) OVERRIDING SYSTEM VALUE VALUES(-950,'Inspection report','REPORT',-950,'PUBLIC');
        INSERT INTO digital_asset_version(id,asset_id,version_number,source_url,provenance,recorded_by_account_id) OVERRIDING SYSTEM VALUE
          VALUES(-950,-950,1,'https://example.test/report.pdf','Archived institutional report',-950),(-951,-950,2,NULL,'Upload only',-950);
        UPDATE digital_asset_version SET sealed_at=CURRENT_TIMESTAMP WHERE id=-950;
        INSERT INTO measurement_scope(id,kind,population) OVERRIDING SYSTEM VALUE VALUES(-950,'GLOBAL','All households'),(-951,'GLOBAL','Other population');
        INSERT INTO work_effort(id,title,description,type_code,period_start,period_end,date_precision) OVERRIDING SYSTEM VALUE
          VALUES(-950,'Water programme','Spans administrations','PROGRAMME','2000-01-01','2004-12-31','YEAR');
        UPDATE performance_record SET recorded_by_account_id=-950,claimant_party_id=NULL,work_effort_id=-950 WHERE id=-950;
    `);
    await rejects("UPDATE digital_asset_version SET provenance='Changed' WHERE id=-950");
    await rejects("UPDATE digital_asset_version SET sealed_at=CURRENT_TIMESTAMP WHERE id=-951");
    await rejects("INSERT INTO claim_evidence(record_id,asset_version_id,relationship_code) VALUES(-950,-951,'SUPPORTS')");
    await rejects("UPDATE performance_record SET publication_status='PUBLISHED' WHERE id=-950");
    await db.exec(`INSERT INTO claim_evidence(record_id,asset_version_id,relationship_code,locator) VALUES(-950,-950,'SUPPORTS','page 12');
        UPDATE performance_record SET publication_status='PUBLISHED' WHERE id=-950;`);
    await rejects("UPDATE performance_record SET title='Rewritten history' WHERE id=-950");
    await rejects("DELETE FROM claim_evidence WHERE record_id=-950");
    await rejects("INSERT INTO claim_policy_area(record_id,policy_area_id) SELECT -950,id FROM policy_area LIMIT 1");
    await db.exec(`
        INSERT INTO work_effort_participation(work_effort_id,party_id,role_code,officeholder_id,asset_version_id,recorded_by_account_id)
          VALUES(-950,-950,'INITIATOR',-950,-950,-950);
        INSERT INTO performance_observation(id,indicator_id,actual_value,officeholder_id,scope_id,period_start,period_end,date_precision,recorded_by_account_id) OVERRIDING SYSTEM VALUE
          VALUES(-951,-950,12,-950,-950,'2002-12-31','2002-12-31','DAY',-950),(-952,-950,10,-950,-950,'2002-12-31','2002-12-31','DAY',-950);
        INSERT INTO observation_evidence(observation_id,asset_version_id,relationship_code) VALUES(-951,-950,'SUPPORTS'),(-952,-950,'CHALLENGES');
        UPDATE performance_observation SET publication_status='PUBLISHED' WHERE id IN(-951,-952);
        INSERT INTO performance_record(claim_id,officeholder_id,revision_number,title,description,date_precision,recorded_by_account_id)
          VALUES(-950,-950,2,'Uncertain dates','No exact dates in source','UNKNOWN',-950);
    `);
    await rejects("INSERT INTO work_effort_participation(work_effort_id,party_id,role_code,officeholder_id,asset_version_id,recorded_by_account_id) VALUES(-950,-951,'FUNDER',-950,-950,-950)");
    await rejects("UPDATE performance_record SET period_start='2001-01-01' WHERE claim_id=-950 AND revision_number=2");
    assert.equal(await scalar("SELECT count(*)::int FROM governance_track_record WHERE person_id=-950"),1);
    assert.equal(await scalar("SELECT count(*)::int FROM performance_observation WHERE period_start='2002-12-31' AND legacy_measurement_id IS NULL"),2);
    await db.exec(`
        INSERT INTO election_event(id,election_cycle_id,election_date,type,status) OVERRIDING SYSTEM VALUE VALUES(-950,1,CURRENT_DATE+365,1,1);
        INSERT INTO contest(id,election_event_id,seat_id) OVERRIDING SYSTEM VALUE SELECT -950,-950,min(id) FROM seat;
        INSERT INTO candidacy(id,person_id,contest_id,status) OVERRIDING SYSTEM VALUE VALUES(-951,-951,-950,1);
        INSERT INTO manifesto(id,issuer_person_id,candidacy_id,created_by_account_id) OVERRIDING SYSTEM VALUE VALUES(-950,-951,-951,-951);
        INSERT INTO manifesto_version(id,manifesto_id,version_number,title,content,recorded_by_account_id) OVERRIDING SYSTEM VALUE
          VALUES(-950,-950,1,'First edition','Original pledges',-951),(-951,-950,2,'Second edition','Revised pledges',-951);
        INSERT INTO commitment(id,manifesto_id) OVERRIDING SYSTEM VALUE VALUES(-950,-950);
        INSERT INTO commitment_revision(id,commitment_id,revision_number,wording,recorded_by_account_id) OVERRIDING SYSTEM VALUE
          VALUES(-950,-950,1,'Deliver 20 operational facilities',-951),(-951,-950,2,'Deliver 10 operational facilities',-951);
        INSERT INTO manifesto_commitment VALUES(-950,-950),(-951,-951);
        INSERT INTO commitment_target(id,commitment_revision_id,indicator_id,scope_id,target_value,period_start,period_end) OVERRIDING SYSTEM VALUE
          VALUES(-950,-950,-950,-950,20,'2001-01-01','2002-12-31');
    `);
    await rejects("INSERT INTO manifesto_commitment VALUES(-950,-951)");
    await db.exec("UPDATE manifesto_version SET publication_status='PUBLISHED' WHERE id=-950");
    await rejects("UPDATE commitment_revision SET wording='Only 10' WHERE id=-950");
    await rejects("UPDATE commitment_target SET target_value=10 WHERE id=-950");
    await rejects("DELETE FROM manifesto_commitment WHERE manifesto_version_id=-950");
    await db.exec("UPDATE manifesto_version SET publication_status='PUBLISHED' WHERE id=-951");
    assert.equal(await scalar("SELECT wording FROM commitment_revision WHERE id=-950"),'Deliver 20 operational facilities');
    await db.exec(`
        INSERT INTO performance_assessment(id,commitment_revision_id,commitment_target_id,observation_id,delivery_status,evidence_status,criteria,rationale,reviewed_by_account_id) OVERRIDING SYSTEM VALUE
          VALUES(-950,-950,-950,-951,'PARTIALLY_FULFILLED','SUPPORTED','Compare operational count','12 of 20 observed',-950);
        INSERT INTO assessment_evidence VALUES(-950,-950,'SUPPORTS','page 12','Inspection');
        UPDATE performance_assessment SET publication_status='PUBLISHED' WHERE id=-950;
    `);
    await rejects("UPDATE performance_assessment SET delivery_status='FULFILLED' WHERE id=-950");
    await rejects("INSERT INTO performance_assessment(commitment_revision_id,delivery_status,evidence_status,criteria,rationale,reviewed_by_account_id) VALUES(-950,'UNFULFILLED','INSUFFICIENT_EVIDENCE','Review sources','No evidence',-950)");
    await db.exec(`
        INSERT INTO performance_observation(id,indicator_id,actual_value,officeholder_id,scope_id,period_start,period_end,date_precision,recorded_by_account_id) OVERRIDING SYSTEM VALUE
          VALUES(-953,-950,12,-950,-951,'2002-12-31','2002-12-31','DAY',-950);
        INSERT INTO observation_evidence VALUES(-953,-950,'SUPPORTS',NULL,NULL);
        UPDATE performance_observation SET publication_status='PUBLISHED' WHERE id=-953;
    `);
    await rejects("INSERT INTO performance_assessment(commitment_revision_id,commitment_target_id,observation_id,delivery_status,evidence_status,criteria,rationale,reviewed_by_account_id) VALUES(-950,-950,-953,'FULFILLED','SUPPORTED','Compare','Wrong population',-950)");
    await db.exec("UPDATE manifesto_version SET publication_status='WITHDRAWN' WHERE id=-950");
    await rejects("UPDATE manifesto_version SET publication_status='DRAFT',published_at=NULL WHERE id=-950");
    await rejects("UPDATE performance_indicator SET methodology='Count construction instead' WHERE id=-950");
    await rejects("UPDATE measurement_scope SET population='Different denominator' WHERE id=-950");
    await rejects("UPDATE digital_asset_version SET asset_id=99 WHERE id=-951");
    await rejects("INSERT INTO digital_asset_version(asset_id,version_number,source_url,provenance) VALUES(-950,3,'javascript:alert(1)','Unsafe URL')");
    await rejects("INSERT INTO stored_file(asset_version_id,representation_type,storage_key,original_filename,media_type,byte_size,sha256) VALUES(-950,'ORIGINAL','files/a.pdf','a.pdf','application/pdf',12,repeat('a',64))");
    await db.exec(`
        INSERT INTO stored_file(asset_version_id,representation_type,storage_key,original_filename,media_type,byte_size,sha256)
          VALUES(-951,'ORIGINAL','files/upload.pdf','upload.pdf','application/pdf',12,repeat('a',64));
        UPDATE digital_asset_version SET sealed_at=CURRENT_TIMESTAMP WHERE id=-951;
    `);
    await rejects("UPDATE stored_file SET storage_key='replacement.pdf' WHERE asset_version_id=-951");
    await rejects("UPDATE claim_evidence SET record_id=(SELECT id FROM performance_record WHERE claim_id=-950 AND revision_number=2) WHERE record_id=-950");
    await rejects("INSERT INTO performance_record(claim_id,officeholder_id,revision_number,title,description,date_precision,publication_status) VALUES(-950,-950,3,'Bypass','Bypass','UNKNOWN','PUBLISHED')");
    await db.exec(`
        INSERT INTO manifesto_version(id,manifesto_id,version_number,title,content,recorded_by_account_id) OVERRIDING SYSTEM VALUE
          VALUES(-952,-950,3,'New draft','Content',-951);
        INSERT INTO manifesto_commitment VALUES(-952,-951);
        UPDATE user_account SET status='DISABLED' WHERE id=-951;
    `);
    await rejects("UPDATE manifesto_version SET publication_status='PUBLISHED' WHERE id=-952");
    await db.exec("UPDATE user_account SET status='ACTIVE' WHERE id=-951; UPDATE election_event SET election_date=CURRENT_DATE-1 WHERE id=-950");
    await rejects("UPDATE manifesto_version SET publication_status='PUBLISHED' WHERE id=-952");
    await db.exec("UPDATE election_event SET election_date=CURRENT_DATE+365 WHERE id=-950; UPDATE candidacy SET status=(SELECT id FROM candidacy_status WHERE status_name='REJECTED') WHERE id=-951");
    await rejects("UPDATE manifesto_version SET publication_status='PUBLISHED' WHERE id=-952");
    await db.exec("UPDATE candidacy SET status=1 WHERE id=-951");
    await rejects("INSERT INTO publication_event(manifesto_version_id,announced_at,venue,recorded_by_account_id) VALUES(-952,CURRENT_TIMESTAMP,'Draft announcement',-951)");
    await db.exec("INSERT INTO publication_event(manifesto_version_id,announced_at,venue,recorded_by_account_id) VALUES(-951,CURRENT_TIMESTAMP,'Public meeting',-951)");
    await rejects("UPDATE publication_event SET venue='Different place'");
    // Exercise the same parameterized JSON/RETURNING SQL used by JsonRecordStore.
    const write=await db.query(`WITH inserted AS (INSERT INTO work_effort(title,description,type_code)
      SELECT title,description,type_code FROM jsonb_populate_record(NULL::work_effort,cast($1 AS jsonb)) RETURNING *)
      SELECT cast(row_to_json(inserted) AS text) AS payload FROM inserted`,
      [JSON.stringify({title:'Repository pattern',description:'No target required',type_code:'PROJECT'})]);
    const inserted=JSON.parse(write.rows[0].payload);
    assert.equal(inserted.date_precision,'UNKNOWN');
    await db.query(`WITH changed AS (UPDATE work_effort SET (description)=(SELECT description FROM jsonb_populate_record(NULL::work_effort,cast($1 AS jsonb))) WHERE id=$2 RETURNING *) SELECT cast(row_to_json(changed) AS text) FROM changed`,[JSON.stringify({description:'Updated draft'}),inserted.id]);
    console.log(`Governance migrations, legacy preservation and ${checks} rejection checks passed.`);
} catch(error) { console.error(error);process.exitCode=1; }
finally { await db.close(); }
