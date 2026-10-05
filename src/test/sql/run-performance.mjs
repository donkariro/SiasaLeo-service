// Isolated PostgreSQL/WASM checks; never connects to the application database.
import { PGlite } from '@electric-sql/pglite';
import { readFile, readdir } from 'node:fs/promises';
import assert from 'node:assert/strict';

const db = new PGlite();
try {
    const directory = new URL('../../main/resources/database/migrations/', import.meta.url);
    const migrations = (await readdir(directory)).filter(n => /^V\d+__.*\.sql$/.test(n))
        .sort((a, b) => Number(a.match(/^V(\d+)/)[1]) - Number(b.match(/^V(\d+)/)[1]));
    for (const migration of migrations) {
        let sql = await readFile(new URL(migration, directory), 'utf8');
        if (migration.startsWith('V3__') && !process.argv.includes('--full')) {
            const rows = sql.split('\n').filter(line => /^\([1-8], /.test(line))
                .map(line => line.trim().replace(/,$/, ''));
            sql = 'INSERT INTO electoral_area (id,name,area_code,area_type_id,parent_id,ancestor_path) OVERRIDING SYSTEM VALUE VALUES '
                + rows.join(',') + ';';
        }
        await db.exec(sql);
    }

    await db.exec(`
        INSERT INTO party(id,party_type) OVERRIDING SYSTEM VALUE VALUES(-910,'PERSON');
        INSERT INTO person(id,first_name,last_name) VALUES(-910,'Performance','Fixture');
        INSERT INTO leadership_term(id,seat_id,start_date,end_date) OVERRIDING SYSTEM VALUE
            SELECT -910,min(id),'2025-01-01','2029-12-31' FROM seat;
        INSERT INTO officeholder(id,seat_id,person_id,term_id,entry_reason_id,start_date)
            OVERRIDING SYSTEM VALUE
            SELECT -910,seat_id,-910,id,
                (SELECT id FROM office_entry_reason WHERE reason_name='APPOINTMENT'),'2025-01-01'
            FROM leadership_term WHERE id=-910;
        INSERT INTO performance_indicator(id,code,name,unit,methodology,direction)
            OVERRIDING SYSTEM VALUE VALUES(-910,'NET_CHANGE','Net change','count','Change since baseline','HIGHER_IS_BETTER');
        INSERT INTO performance_target(id,officeholder_id,indicator_id,period_start,period_end,baseline_value,target_value,source_reference)
            OVERRIDING SYSTEM VALUE VALUES(-910,-910,-910,'2026-01-01','2026-12-31',0,100,'archive/plan.pdf');
        INSERT INTO performance_measurement(target_id,measured_on,actual_value,source_reference)
            VALUES(-910,'2026-01-01',-2,'archive/start.pdf'),(-910,'2026-12-31',105,'archive/end.pdf');
    `);
    async function rejects(sql, code) {
        await assert.rejects(db.exec(sql), error => error.code === code, sql);
    }
    await rejects("UPDATE performance_indicator SET code='   ' WHERE id=-910", '23514');
    await rejects("UPDATE performance_indicator SET methodology='' WHERE id=-910", '23514');
    await rejects("UPDATE performance_indicator SET direction='UNKNOWN' WHERE id=-910", '23514');
    await rejects("UPDATE performance_target SET period_end='2025-12-31' WHERE id=-910", '23514');
    await rejects("UPDATE performance_target SET period_start='2026-02-01' WHERE id=-910", '23514');
    await rejects("UPDATE performance_target SET target_value='NaN' WHERE id=-910", '23514');
    await rejects("UPDATE performance_target SET baseline_value='NaN' WHERE id=-910", '23514');
    await rejects("UPDATE performance_target SET officeholder_id=-999 WHERE id=-910", '23503');
    await rejects("UPDATE performance_target SET indicator_id=-999 WHERE id=-910", '23503');
    await rejects(`INSERT INTO performance_target(officeholder_id,indicator_id,period_start,period_end,target_value,source_reference)
        VALUES(-910,-910,'2026-01-01','2026-12-31',50,'archive/duplicate.pdf')`, '23505');
    await rejects(`INSERT INTO performance_measurement(target_id,measured_on,actual_value,source_reference)
        VALUES(-910,'2027-01-01',1,'archive/outside.pdf')`, '23514');
    await rejects(`INSERT INTO performance_measurement(target_id,measured_on,actual_value,source_reference)
        VALUES(-910,'2026-01-01',1,'archive/duplicate.pdf')`, '23505');
    await rejects("UPDATE performance_measurement SET measured_on='2025-01-01' WHERE target_id=-910", '23514');
    await rejects("UPDATE performance_measurement SET actual_value='NaN' WHERE target_id=-910", '23514');
    await rejects("UPDATE performance_measurement SET source_reference=' ' WHERE target_id=-910", '23514');
    await rejects("UPDATE performance_measurement SET target_id=-999 WHERE target_id=-910", '23503');
    await rejects('DELETE FROM performance_target WHERE id=-910', '23503');
    await rejects('DELETE FROM performance_indicator WHERE id=-910', '23514');
    await rejects('DELETE FROM officeholder WHERE id=-910', '23503');
    assert.equal((await db.query('SELECT count(*)::int AS count FROM performance_measurement')).rows[0].count, 2);

    // A former holder can be documented without registration, claims or targets.
    await db.exec(`
        INSERT INTO party(id,party_type) OVERRIDING SYSTEM VALUE VALUES(-911,'PERSON');
        INSERT INTO person(id,first_name,last_name) VALUES(-911,'Historical','Officeholder');
        INSERT INTO leadership_term(id,seat_id,start_date,end_date) OVERRIDING SYSTEM VALUE
            SELECT -911,min(id),'2017-01-01','2021-12-31' FROM seat;
        INSERT INTO officeholder(id,seat_id,person_id,term_id,entry_reason_id,exit_reason_id,start_date,end_date)
            OVERRIDING SYSTEM VALUE
            SELECT -911,seat_id,-911,id,
                (SELECT id FROM office_entry_reason WHERE reason_name='APPOINTMENT'),
                (SELECT id FROM office_exit_reason WHERE reason_name='TERM_END'),
                '2017-01-01','2021-12-31'
            FROM leadership_term WHERE id=-911;
        INSERT INTO achievement_claim(id,officeholder_id) OVERRIDING SYSTEM VALUE VALUES(-911,-911);
        INSERT INTO performance_record(claim_id,officeholder_id,title,description,period_start,period_end,source_reference)
            VALUES(-911,-911,'Water project','Archived report documents work on the water project.',
                '2018-01-01','2019-12-31','archive/water-report.pdf');
    `);
    const historical = (await db.query(`
        SELECT r.title, oh.end_date IS NOT NULL AS former_holder,
               r.created_at::date > r.period_end AS retrospective
        FROM performance_record r JOIN officeholder oh ON oh.id=r.officeholder_id
        WHERE oh.person_id=-911`)).rows;
    assert.equal(historical.length, 1);
    assert.equal(historical[0].title, 'Water project');
    assert.equal(historical[0].former_holder, true);
    assert.equal(historical[0].retrospective, true);
    assert.equal((await db.query('SELECT count(*)::int AS count FROM user_account WHERE person_id=-911')).rows[0].count, 0);
    assert.equal((await db.query('SELECT count(*)::int AS count FROM performance_target WHERE officeholder_id=-911')).rows[0].count, 0);
    await rejects("UPDATE performance_record SET title=' ' WHERE officeholder_id=-911", '23514');
    await rejects("UPDATE performance_record SET description='' WHERE officeholder_id=-911", '23514');
    await rejects("UPDATE performance_record SET source_reference=' ' WHERE officeholder_id=-911", '23514');
    await rejects("UPDATE performance_record SET period_end='2017-12-31' WHERE officeholder_id=-911", '23514');
    await rejects('UPDATE performance_record SET officeholder_id=-999 WHERE officeholder_id=-911', '23514');
    await rejects("INSERT INTO performance_record(claim_id,officeholder_id,revision_number,title,description,date_precision) VALUES(-911,-999,2,'Invalid subject','Invalid subject','UNKNOWN')", '23503');
    await rejects('DELETE FROM officeholder WHERE id=-911', '23503');
    console.log('All migrations applied; performance constraints and historical track records without accounts or targets passed.');
} catch (error) {
    console.error(error);
    process.exitCode = 1;
} finally {
    await db.close();
}
