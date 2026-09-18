// Optional isolated PostgreSQL/WASM check; see docs/geography-snapshots.md.
import { PGlite } from '../../../target/snapshot-db-test/node_modules/@electric-sql/pglite/dist/index.js';
import { readFile } from 'node:fs/promises';

const db = new PGlite();
try {
    for (const migration of [
        'V2__create_area_type_electoral_area.sql',
        'V3__seed_electoral_areas.sql',
        'V43__electoral_geography_snapshots.sql',
        'V44__draft_existing_geography.sql'
    ]) {
        let sql = await readFile(new URL(`../../main/resources/database/migrations/${migration}`, import.meta.url), 'utf8');
        if (migration.startsWith('V3__') && process.argv.includes('--small')) {
            // Keep the real seed's first complete branch for a fast smoke check.
            const rows = sql.split('\n').filter(line => /^\([1-8], /.test(line)).map(line => line.trim().replace(/,$/, ''));
            sql = 'INSERT INTO electoral_area (id,name,area_code,area_type_id,parent_id,ancestor_path) OVERRIDING SYSTEM VALUE VALUES '
                + rows.join(',') + ';';
        }
        await db.exec(sql);
        console.log(`Applied ${migration}`);
    }
    await db.exec(await readFile(new URL('geography_snapshots.sql', import.meta.url), 'utf8'));
    console.log('Snapshot migration and invariant checks passed.');
} catch (error) {
    console.error(error.message);
    console.error(error.where ?? '');
    process.exitCode = 1;
} finally {
    await db.close();
}
