import { PGlite } from '@electric-sql/pglite';
import { readFile, readdir } from 'node:fs/promises';
import assert from 'node:assert/strict';

// V59 forms and V60 nominations: immutability, pinning and cross-domain guards.
const db = new PGlite();
let checks = 0;
const scalar = async (sql, params = []) => Object.values((await db.query(sql, params)).rows[0])[0];
const rejects = async (sql, code = '23514') => { await assert.rejects(db.exec(sql), e => e.code === code, sql); checks++; };
try {
    const directory = new URL('../../main/resources/database/migrations/', import.meta.url);
    const migrations = (await readdir(directory)).filter(n => /^V\d+__.*\.sql$/.test(n))
        .sort((a, b) => Number(a.match(/^V(\d+)/)[1]) - Number(b.match(/^V(\d+)/)[1]));
    for (const name of migrations) {
        let sql = await readFile(new URL(name, directory), 'utf8');
        if (name.startsWith('V3__') && !process.argv.includes('--full')) {
            const rows = sql.split('\n').filter(l => /^\([1-8], /.test(l)).map(l => l.trim().replace(/,$/, ''));
            sql = 'INSERT INTO electoral_area(id,name,area_code,area_type_id,parent_id,ancestor_path) OVERRIDING SYSTEM VALUE VALUES ' + rows.join(',') + ';';
        }
        await db.exec(sql);
    }
    await db.exec(`
        INSERT INTO party(id,party_type) OVERRIDING SYSTEM VALUE VALUES(-960,'PERSON'),(-961,'PERSON');
        INSERT INTO person(id,first_name,last_name) VALUES(-960,'Form','Applicant'),(-961,'Party','Manager');
        INSERT INTO user_account(id,person_id,email,password_hash,status) OVERRIDING SYSTEM VALUE
          VALUES(-960,-960,'applicant@example.test','unused','ACTIVE'),(-961,-961,'manager@example.test','unused','ACTIVE');
        INSERT INTO election_event(id,election_cycle_id,election_date,type,status) OVERRIDING SYSTEM VALUE
          VALUES(-960,1,CURRENT_DATE+365,1,1),(-961,1,CURRENT_DATE+400,1,1);
        INSERT INTO contest(id,election_event_id,seat_id) OVERRIDING SYSTEM VALUE SELECT -960,-960,min(id) FROM seat;
        INSERT INTO contest(id,election_event_id,seat_id) OVERRIDING SYSTEM VALUE SELECT -961,-961,min(id) FROM seat;
        INSERT INTO digital_asset(id,title,asset_type,created_by_account_id,owner_account_id) OVERRIDING SYSTEM VALUE
          VALUES(-960,'Resignation letter','DOCUMENT',-960,-960);
        INSERT INTO digital_asset_version(id,asset_id,version_number,source_url) OVERRIDING SYSTEM VALUE
          VALUES(-960,-960,1,'https://example.test/letter'),(-961,-960,2,'https://example.test/draft');
        UPDATE digital_asset_version SET sealed_at=CURRENT_TIMESTAMP WHERE id=-960;
        INSERT INTO forms.tenant(id,tenant_type,external_ref) OVERRIDING SYSTEM VALUE
          SELECT -960,'POLITICAL_PARTY',min(id)::text FROM political_party;
        INSERT INTO forms.form_definition(id,code,purpose_key,tenant_id,title) OVERRIDING SYSTEM VALUE VALUES
          (-960,'nomination-core','nomination.application',NULL,'Nomination core'),
          (-961,'nomination-party','nomination.application',-960,'Party nomination'),
          (-962,'membership-core','membership.application',NULL,'Membership core');
    `);

    // FormStore allocates version numbers with this statement.
    const insertVersion = `INSERT INTO forms.form_version(definition_id,version_number,base_version_id,source,resolved,created_by_account_id)
        SELECT $1,COALESCE(MAX(version_number),0)+1,CAST($2 AS bigint),CAST($3 AS jsonb),CAST($4 AS jsonb),-961
        FROM forms.form_version WHERE definition_id=$1 RETURNING id`;
    const resolved = '{"format":1,"title":"Core","sections":[],"declarations":[]}';
    const core = (await db.query(insertVersion, [-960, null, '{"format":1}', resolved])).rows[0].id;
    const core2 = (await db.query(insertVersion, [-960, null, '{"format":1}', resolved])).rows[0].id;
    assert.equal(await scalar('SELECT version_number FROM forms.form_version WHERE id=$1', [core2]), 2);
    assert.equal(await scalar('SELECT checksum FROM forms.form_version WHERE id=$1', [core]), null);

    // Tenant forms extend a platform core with the same purpose; cores stand alone.
    await assert.rejects(db.query(insertVersion, [-961, null, '{}', resolved]), e => e.code === '23514'); checks++;
    await assert.rejects(db.query(insertVersion, [-960, core, '{}', resolved]), e => e.code === '23514'); checks++;
    const membership = (await db.query(insertVersion, [-962, null, '{}', resolved])).rows[0].id;
    await assert.rejects(db.query(insertVersion, [-961, membership, '{}', resolved]), e => e.code === '23514'); checks++;
    const party = (await db.query(insertVersion, [-961, core, '{}', resolved])).rows[0].id;
    await rejects(`UPDATE forms.form_version SET status='PUBLISHED',published_at=now() WHERE id=${party}`);

    await db.exec(`UPDATE forms.form_version SET status='PUBLISHED',published_at=now() WHERE id=${core}`);
    assert.equal(await scalar('SELECT checksum FROM forms.form_version WHERE id=$1', [core]),
        await scalar("SELECT encode(sha256(convert_to(resolved::text,'UTF8')),'hex') FROM forms.form_version WHERE id=$1", [core]));
    await db.exec(`UPDATE forms.form_version SET status='PUBLISHED',published_at=now() WHERE id=${party}`);
    await rejects(`UPDATE forms.form_version SET source='{"changed":true}' WHERE id=${core}`);
    await rejects(`UPDATE forms.form_version SET status='DRAFT' WHERE id=${core}`);
    await rejects(`DELETE FROM forms.form_version WHERE id=${core}`);
    await rejects(`UPDATE forms.form_version SET status='RETIRED' WHERE id=${core2}`);
    await db.exec(`UPDATE forms.form_version SET status='RETIRED',row_version=row_version+1 WHERE id=${core}`);
    await rejects(`UPDATE forms.form_version SET status='PUBLISHED' WHERE id=${core}`);
    await db.exec(`DELETE FROM forms.form_version WHERE id=${core2}`);

    // Instances pin a published version; submissions record its checksum and never change.
    await rejects(`INSERT INTO forms.form_instance(form_version_id) VALUES(${membership})`);
    const instance = await scalar(`INSERT INTO forms.form_instance(form_version_id,tenant_id,created_by_account_id) VALUES(${party},-960,-960) RETURNING id`);
    await rejects(`UPDATE forms.form_instance SET form_version_id=${core} WHERE id=${instance}`);
    await rejects(`INSERT INTO forms.form_submission(instance_id,revision,form_version_id,definition_checksum,answers,instance_row_version,submitted_by_account_id)
        VALUES(${instance},1,${party},repeat('0',64),'{}',0,-960)`);
    const insertSubmission = `INSERT INTO forms.form_submission(instance_id,revision,form_version_id,definition_checksum,answers,context,instance_row_version,submitted_by_account_id)
        SELECT $1,COALESCE(MAX(revision),0)+1,$2,$3,CAST($4 AS jsonb),CAST($5 AS jsonb),$6,-960 FROM forms.form_submission WHERE instance_id=$1 RETURNING id`;
    const checksum = await scalar('SELECT checksum FROM forms.form_version WHERE id=$1', [party]);
    const submission = (await db.query(insertSubmission, [instance, party, checksum, '{"core":{"phone":"0700"}}', '{}', 0])).rows[0].id;
    await assert.rejects(db.query(insertSubmission, [instance, party, checksum, '{}', '{}', 0]), e => e.code === '23505'); checks++;
    const second = (await db.query(insertSubmission, [instance, party, checksum, '{}', '{}', 1])).rows[0].id;
    assert.equal(await scalar('SELECT revision FROM forms.form_submission WHERE id=$1', [second]), 2);
    await rejects(`UPDATE forms.form_submission SET answers='{}' WHERE id=${submission}`);
    await rejects(`DELETE FROM forms.form_submission WHERE id=${submission}`);
    assert.equal(await scalar(`SELECT count(*)::int FROM forms.form_submission WHERE answers @> '{"core":{"phone":"0700"}}'`), 1);

    await rejects(`INSERT INTO forms.submission_evidence VALUES(${submission},'core.resignation',-961)`);
    await db.exec(`INSERT INTO forms.submission_evidence VALUES(${submission},'core.resignation',-960)`);
    await rejects(`DELETE FROM forms.submission_evidence WHERE submission_id=${submission}`);
    await db.exec(`INSERT INTO forms.declaration_acceptance VALUES(${submission},'core.truth','The information is true.')`);
    await rejects(`UPDATE forms.declaration_acceptance SET declaration_text='Edited' WHERE submission_id=${submission}`);

    // Nominations: forms fixed once open, applications only while open, one live application per aspirant.
    const exercise = await scalar(`INSERT INTO nomination_exercise(political_party_id,election_event_id,title,opens_at,closes_at,created_by_account_id)
        SELECT min(id),-960,'Primaries',now()-interval '1 day',now()+interval '1 day',-961 FROM political_party RETURNING id`);
    await rejects(`INSERT INTO nomination_exercise_form VALUES(${exercise},'INITIAL_APPLICATION',${membership})`);
    await rejects(`INSERT INTO nomination_exercise_form VALUES(${exercise},'APPEAL',${party})`);
    await db.exec(`INSERT INTO nomination_exercise_form VALUES(${exercise},'INITIAL_APPLICATION',${party})`);
    await rejects(`INSERT INTO nomination_application(exercise_id,person_id,contest_id) VALUES(${exercise},-960,-960)`);
    await db.exec(`UPDATE nomination_exercise SET status='OPEN' WHERE id=${exercise}`);
    await rejects(`UPDATE nomination_exercise_form SET form_version_id=${party} WHERE exercise_id=${exercise}`);
    await rejects(`INSERT INTO nomination_application(exercise_id,person_id,contest_id) VALUES(${exercise},-960,-961)`);
    const application = await scalar(`INSERT INTO nomination_application(exercise_id,person_id,contest_id) VALUES(${exercise},-960,-960) RETURNING id`);
    await rejects(`INSERT INTO nomination_application(exercise_id,person_id,contest_id) VALUES(${exercise},-960,-960)`, '23505');
    await db.exec(`INSERT INTO nomination_application_form VALUES(${application},'INITIAL_APPLICATION',${instance})`);
    await rejects(`UPDATE nomination_application SET contest_id=-961 WHERE id=${application}`);
    await rejects(`UPDATE nomination_application SET status='SUBMITTED' WHERE id=${application}`);
    await db.exec(`UPDATE nomination_application SET status='WITHDRAWN',withdrawn_at=now() WHERE id=${application}`);
    await rejects(`UPDATE nomination_application SET status='DRAFT',withdrawn_at=NULL WHERE id=${application}`);
    await db.exec(`INSERT INTO nomination_application(exercise_id,person_id,contest_id) VALUES(${exercise},-960,-960)`);

    console.log(`Forms and nominations database checks passed (${checks} rejections verified).`);
} finally {
    await db.close();
}
