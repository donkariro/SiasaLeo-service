// Isolated PostgreSQL/WASM checks; never connects to the application database.
import { PGlite } from '../../../target/snapshot-db-test/node_modules/@electric-sql/pglite/dist/index.js';
import { readFile, readdir } from 'node:fs/promises';

const db = new PGlite();
try {
    const directory = new URL('../../main/resources/database/migrations/', import.meta.url);
    const migrations = (await readdir(directory)).filter(n => /^V\d+__.*\.sql$/.test(n))
        .sort((a,b) => Number(a.match(/^V(\d+)/)[1]) - Number(b.match(/^V(\d+)/)[1]));
    for (const migration of migrations) {
        if (migration.startsWith('V45__')) {
            // Simulate an installation that already holds legacy official results.
            await db.exec(`
                INSERT INTO party(id,party_type) OVERRIDING SYSTEM VALUE VALUES(-900,'PERSON');
                INSERT INTO person(id,first_name,last_name) VALUES(-900,'Legacy','Candidate');
                INSERT INTO election_event(id,election_cycle_id,election_date,type,status) OVERRIDING SYSTEM VALUE
                    VALUES(-900,1,'2013-03-04',1,3);
                INSERT INTO contest(id,election_event_id,seat_id) OVERRIDING SYSTEM VALUE
                    SELECT -900,-900,min(id) FROM seat;
                INSERT INTO candidacy(id,person_id,contest_id,status) OVERRIDING SYSTEM VALUE VALUES(-900,-900,-900,6);
                INSERT INTO contest_result(candidacy_id,polling_station_id,vote_count)
                    SELECT -900,min(a.id),123 FROM electoral_area a JOIN area_type t ON t.id=a.area_type_id WHERE t.name='POLLING_STATION';
                INSERT INTO provisional_contest_result(candidacy_id,polling_station_id,vote_count,recorded_at)
                    SELECT -900,min(a.id),120,'2013-03-04 10:00:00+00' FROM electoral_area a JOIN area_type t ON t.id=a.area_type_id WHERE t.name='POLLING_STATION';
            `);
        }
        let sql = await readFile(new URL(migration,directory),'utf8');
        if (migration.startsWith('V3__') && !process.argv.includes('--full')) {
            const rows = sql.split('\n').filter(line => /^\([1-8], /.test(line)).map(line => line.trim().replace(/,$/,''));
            sql = 'INSERT INTO electoral_area (id,name,area_code,area_type_id,parent_id,ancestor_path) OVERRIDING SYSTEM VALUE VALUES '+rows.join(',')+';';
        }
        await db.exec(sql);
        console.log(`Applied ${migration}`);
    }
    const legacy = (await db.query(`SELECT cr.vote_count,c.geography_snapshot_id,c.jurisdiction_id,c.office_id
        FROM contest_result cr JOIN candidacy ca ON ca.id=cr.candidacy_id JOIN contest c ON c.id=ca.contest_id
        WHERE c.id=-900 AND cr.stage='OFFICIAL'`)).rows[0];
    if (!legacy || Number(legacy.vote_count) !== 123 || legacy.geography_snapshot_id !== null || legacy.jurisdiction_id !== null || legacy.office_id === null) {
        throw new Error('Legacy results must survive without inferred historical geography');
    }
    const provisional=(await db.query("SELECT vote_count,recorded_at FROM contest_result WHERE candidacy_id=-900 AND stage='PROVISIONAL'")).rows[0];
    if(!provisional || Number(provisional.vote_count)!==120 || !provisional.recorded_at) throw new Error('Provisional result migration lost data');
    await db.exec(await readFile(new URL('election_records.sql',import.meta.url),'utf8'));
    console.log('Election record migration and invariant checks passed.');
} catch(error) {
    console.error(error.message);
    console.error(error.where ?? '');
    process.exitCode=1;
} finally { await db.close(); }
