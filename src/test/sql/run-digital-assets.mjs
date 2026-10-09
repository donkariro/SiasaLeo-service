import { PGlite } from '@electric-sql/pglite';
import { readFile, readdir } from 'node:fs/promises';
import assert from 'node:assert/strict';

const db = new PGlite();
let checks = 0;
const scalar = async (sql, params = []) => Object.values((await db.query(sql, params)).rows[0])[0];
const rejects = async (sql, code = '23514') => {
    await assert.rejects(db.exec(sql), error => error.code === code, sql);
    checks++;
};
try {
    const directory = new URL('../../main/resources/database/migrations/', import.meta.url);
    const migrations = (await readdir(directory)).filter(n => /^V\d+__.*\.sql$/.test(n))
        .sort((a,b) => Number(a.match(/^V(\d+)/)[1]) - Number(b.match(/^V(\d+)/)[1]));
    for (const name of migrations) {
        if (name.startsWith('V57__')) {
            await db.exec(`
                INSERT INTO party(id,party_type) OVERRIDING SYSTEM VALUE VALUES(880001,'PERSON'),(880002,'PERSON');
                INSERT INTO person(id,first_name,last_name) VALUES(880001,'Asset','Owner'),(880002,'Asset','Reader');
                INSERT INTO user_account(id,person_id,email,password_hash,status) OVERRIDING SYSTEM VALUE
                VALUES(880001,880001,'asset-owner@example.test','unused','ACTIVE'),
                      (880002,880002,'asset-reader@example.test','unused','ACTIVE');
                INSERT INTO digital_asset(id,title,description,asset_type,created_by_account_id) OVERRIDING SYSTEM VALUE
                VALUES(880001,'Historical report','Historical description','REPORT',880001),
                      (880002,'Unassigned legacy asset',NULL,'OTHER',NULL);
                INSERT INTO digital_asset_version(id,asset_id,version_number,source_url,provenance) OVERRIDING SYSTEM VALUE
                VALUES(880001,880001,1,'https://example.test/report','Historical archive');
                UPDATE digital_asset_version SET sealed_at=CURRENT_TIMESTAMP WHERE id=880001;
                INSERT INTO politicalparty_official(official_id,politicalparty_id,position_name,photo)
                SELECT 880001,min(id),'Legacy filename','missing-file.jpeg' FROM political_party;
                INSERT INTO politicalparty_official(official_id,politicalparty_id,position_name,photo)
                SELECT 880001,min(id),'Legacy URL','https://example.test/photo.jpg' FROM political_party;
            `);
        }
        let sql = await readFile(new URL(name,directory),'utf8');
        if(name.startsWith('V3__') && !process.argv.includes('--full')) {
            const rows=sql.split('\n').filter(l=>/^\([1-8], /.test(l)).map(l=>l.trim().replace(/,$/,''));
            sql='INSERT INTO electoral_area(id,name,area_code,area_type_id,parent_id,ancestor_path) OVERRIDING SYSTEM VALUE VALUES '+rows.join(',')+';';
        }
        await db.exec(sql);
    }
    assert.equal(await scalar('SELECT owner_account_id FROM digital_asset WHERE id=880001'),880001);
    assert.equal(await scalar('SELECT owner_account_id FROM digital_asset WHERE id=880002'),null);
    assert.equal(await scalar('SELECT visibility FROM digital_asset WHERE id=880001'),'PUBLIC');
    assert.equal(await scalar('SELECT snapshot_title FROM digital_asset_version WHERE id=880001'),'Historical report');
    assert.equal(await scalar("SELECT count(*)::int FROM legacy_media_reconciliation WHERE legacy_reference='missing-file.jpeg' AND status='NEEDS_FILE_IMPORT'"),1);
    assert.equal(await scalar("SELECT count(*)::int FROM legacy_media_reconciliation r JOIN digital_asset_version v ON v.asset_id=r.asset_id WHERE r.legacy_reference='missing-file.jpeg' AND v.sealed_at IS NULL"),1);
    assert.equal(await scalar("SELECT count(*)::int FROM legacy_media_reconciliation r JOIN digital_asset_version v ON v.asset_id=r.asset_id WHERE r.legacy_reference='https://example.test/photo.jpg' AND v.sealed_at IS NOT NULL"),1);
    await db.exec(`
        INSERT INTO digital_asset(id,title,asset_type,created_by_account_id,owner_account_id) OVERRIDING SYSTEM VALUE
        VALUES(880003,'Private portrait','PORTRAIT',880001,880001),
              (880004,'Other private portrait','PORTRAIT',880002,880002),
              (880005,'Private draft','DOCUMENT',880001,880001);
        INSERT INTO digital_asset_version(id,asset_id,version_number,source_url) OVERRIDING SYSTEM VALUE
        VALUES(880003,880003,1,'https://example.test/portrait.jpg'),
              (880004,880004,1,'https://example.test/other.jpg'),
              (880005,880005,1,'https://example.test/draft');
        UPDATE digital_asset_version SET sealed_at=CURRENT_TIMESTAMP WHERE id IN (880003,880004);
    `);
    assert.equal(await scalar('SELECT visibility FROM digital_asset WHERE id=880003'),'PRIVATE');
    assert.equal(await scalar('SELECT current_asset_version_id(880003)'),880003);
    assert.equal(await scalar('SELECT current_asset_version_id(880005)'),null);

    // Execute the actual native library query, including permissions and filters.
    const repo=await readFile(new URL('../java/../../main/java/com/arriyiaconsulting/siasaleo/service/domain/digitalasset/repository/AssetManagementRepository.java',import.meta.url),'utf8');
    const query=repo.match(/return sql.rows\("""([\s\S]*?)""",archived/)[1].replace(/\?(\d+)/g,(_,n)=>'$'+n);
    const list=async(actor,term='',type='',archived=false)=> (await db.query(query,[archived,term,type,actor,100,0])).rows.map(r=>r.id);
    assert(!(await list(null)).includes(880003));
    assert((await list(880001)).includes(880003));
    assert(!(await list(880002)).includes(880003));
    await db.exec("INSERT INTO digital_asset_grant VALUES(880003,880002,'READ')");
    assert((await list(880002)).includes(880003));
    assert.deepEqual(await list(880002,'Private portrait','PORTRAIT'),[880004,880003]);
    await db.exec("DELETE FROM digital_asset_grant WHERE asset_id=880003 AND account_id=880002");
    assert(!(await list(880002)).includes(880003));

    await db.exec(`
        BEGIN;
        SELECT set_config('siasaleo.actor','880001',true);
        UPDATE digital_asset SET title='Corrected library label',description='Corrected description' WHERE id=880001;
        COMMIT;
    `);
    assert.equal(await scalar('SELECT snapshot_title FROM digital_asset_version WHERE id=880001'),'Historical report');
    assert.equal(await scalar("SELECT actor_account_id FROM digital_asset_audit WHERE asset_id=880001 ORDER BY id DESC LIMIT 1"),880001);
    await rejects("UPDATE digital_asset_version SET snapshot_title='Rewrite history' WHERE id=880001");
    await rejects("UPDATE digital_asset SET created_by_account_id=880002 WHERE id=880001");
    await rejects("DELETE FROM digital_asset WHERE id=880001");
    await rejects("UPDATE digital_asset_audit SET action='Erased'");

    await db.exec(`
        INSERT INTO work_effort(id,title,description,type_code) OVERRIDING SYSTEM VALUE VALUES(880001,'Example','Example','PROJECT');
        INSERT INTO work_effort_asset(work_effort_id,asset_version_id) VALUES(880001,880001);
    `);
    await rejects("UPDATE digital_asset SET visibility='PRIVATE' WHERE id=880001");
    await rejects("INSERT INTO work_effort_asset(work_effort_id,asset_version_id) VALUES(880001,880003)");
    await db.exec("UPDATE digital_asset SET visibility='PUBLIC' WHERE id=880003");
    await rejects("INSERT INTO work_effort_asset(work_effort_id,asset_version_id) VALUES(880001,880003)");
    // General attachments permit a sealed version without evidence provenance.
    await db.exec("INSERT INTO person_digital_asset(person_id,asset_version_id,purpose) VALUES(880001,880003,'PORTRAIT')");
    await db.exec(`
        INSERT INTO party_media_selection(party_id,purpose,asset_id,selected_by_account_id) VALUES(880001,'PORTRAIT',880003,880001);
        UPDATE party_media_selection SET asset_id=880004 WHERE party_id=880001 AND purpose='PORTRAIT';
    `);
    assert.equal(await scalar("SELECT asset_id FROM party_media_selection_history WHERE party_id=880001"),880003);
    await rejects("UPDATE party_media_selection SET asset_id=880005 WHERE party_id=880001");
    await db.exec("UPDATE digital_asset SET archived_at=CURRENT_TIMESTAMP WHERE id=880004");
    assert(!(await list(880002)).includes(880004));
    assert((await list(880002,'','',true)).includes(880004));
    await rejects("INSERT INTO digital_asset_version(asset_id,version_number,source_url) VALUES(880004,2,'https://example.test/next')");
    assert.equal(await scalar('SELECT current_asset_version_id(880004)'),880004);
    await rejects("INSERT INTO party_media_selection(party_id,purpose,asset_id) VALUES(880002,'PORTRAIT',880004)");

    await db.exec("DELETE FROM digital_asset_version WHERE id=880005; DELETE FROM digital_asset WHERE id=880005");
    assert.equal(await scalar('SELECT count(*)::int FROM digital_asset WHERE id=880005'),0);
    await rejects("UPDATE politicalparty_official SET photo='new-legacy-file.jpg' WHERE position_name='Legacy filename'");
    await rejects("UPDATE politicalparty_official SET photo_asset_id=880004 WHERE position_name='Legacy filename'");
    await db.exec("UPDATE politicalparty_official SET photo_asset_id=880003 WHERE position_name='Legacy filename'");
    console.log(`Digital asset migrations, access queries, snapshots, legacy references and ${checks} rejection checks passed.`);
} catch(error) { console.error(error); process.exitCode=1; }
finally { await db.close(); }
