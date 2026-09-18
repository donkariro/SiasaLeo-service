-- Run after V2, V3, V43 and V44 in a DISPOSABLE PostgreSQL database.
-- All test changes are rolled back. The runner must stop on the first error.
BEGIN;
CREATE FUNCTION pg_temp.expect_failure(statement TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN RETURN;
    END;
    RAISE EXCEPTION 'Expected rejection: %', statement;
END $$;

DO $$
DECLARE
    existing BIGINT;
    original_count BIGINT;
    draft BIGINT;
    other_draft BIGINT;
    revision BIGINT;
    node_id BIGINT;
    parent BIGINT := NULL;
    root_id BIGINT;
    centre BIGINT;
    station BIGINT;
    path TEXT := '/';
    node_type RECORD;
    copied BIGINT;
    before_name TEXT;
    historical_id BIGINT;
    live_id BIGINT;
BEGIN
    SELECT id INTO existing FROM electoral_geography_snapshot WHERE name = 'Existing operational geography - provenance unverified';
    ASSERT existing IS NOT NULL, 'V44 must create an unverified draft';
    ASSERT (SELECT status = 'DRAFT' AND source_reference IS NULL AND reference_date IS NULL
            FROM electoral_geography_snapshot WHERE id = existing), 'No guessed provenance';
    SELECT count(*) INTO original_count FROM electoral_area;
    ASSERT (SELECT count(*) FROM electoral_area_snapshot WHERE snapshot_id = existing) = original_count,
        'Every operational node must be copied';
    ASSERT NOT EXISTS (
        SELECT 1 FROM electoral_area_snapshot a JOIN electoral_area_snapshot p ON p.id = a.parent_id
        WHERE a.snapshot_id = existing AND
              (p.snapshot_id <> a.snapshot_id OR a.ancestor_path <> p.ancestor_path || p.id || '/')
    ), 'Copied paths must use historical IDs';
    ASSERT NOT EXISTS (SELECT 1 FROM electoral_area_correspondence WHERE reviewed_at IS NOT NULL),
        'Copied correspondences must remain unreviewed';

    INSERT INTO electoral_geography_snapshot(name) VALUES ('Test draft') RETURNING id INTO draft;
    INSERT INTO electoral_geography_snapshot(name) VALUES ('Other draft') RETURNING id INTO other_draft;
    FOR node_type IN SELECT * FROM area_type ORDER BY id LOOP
        INSERT INTO electoral_area_snapshot(snapshot_id, area_type_id, name, area_code, parent_id, ancestor_path)
            VALUES (draft, node_type.id, node_type.name, '001', parent, path) RETURNING id INTO node_id;
        IF node_type.name = 'WORLD' THEN root_id := node_id; END IF;
        IF node_type.name = 'REGISTRATION_CENTER' THEN centre := node_id; END IF;
        IF node_type.name = 'POLLING_STATION' THEN station := node_id; END IF;
        parent := node_id;
        path := path || node_id || '/';
    END LOOP;

    PERFORM pg_temp.expect_failure(format('INSERT INTO electoral_area_snapshot(snapshot_id,area_type_id,name,parent_id,ancestor_path) VALUES (%s,2,''Cross snapshot'',%s,''/%s/'')', other_draft, root_id, root_id));
    PERFORM pg_temp.expect_failure(format('INSERT INTO electoral_area_snapshot(snapshot_id,area_type_id,name,parent_id,ancestor_path) VALUES (%s,1,''Second root'',NULL,''/'')', draft));
    PERFORM pg_temp.expect_failure(format('UPDATE electoral_area_snapshot SET ancestor_path = ''/wrong/'' WHERE id = %s', station));
    PERFORM pg_temp.expect_failure(format('UPDATE electoral_area_snapshot SET parent_id = %s WHERE id = %s', station, root_id));
    PERFORM pg_temp.expect_failure(format('INSERT INTO electoral_area_snapshot(snapshot_id,area_type_id,name,area_code,parent_id,ancestor_path) SELECT snapshot_id,area_type_id,''Duplicate'',area_code,parent_id,ancestor_path FROM electoral_area_snapshot WHERE id = %s', station));
    PERFORM pg_temp.expect_failure(format('UPDATE electoral_geography_snapshot SET status = ''PUBLISHED'', published_at = now() WHERE id = %s', draft));
    PERFORM pg_temp.expect_failure(format('UPDATE electoral_geography_snapshot SET status = ''PUBLISHED'', published_at = now(), source_reference = ''test'', source_sha256 = repeat(''a'',64) WHERE id = %s', other_draft));
    UPDATE electoral_geography_snapshot SET source_reference = 'archive/test.pdf', source_sha256 = repeat('a',64),
        status = 'PUBLISHED', published_at = now() WHERE id = draft;
    PERFORM pg_temp.expect_failure(format('UPDATE electoral_area_snapshot SET name = ''Changed'' WHERE id = %s', station));
    PERFORM pg_temp.expect_failure(format('DELETE FROM electoral_area_snapshot WHERE id = %s', station));
    PERFORM pg_temp.expect_failure(format('INSERT INTO electoral_area_snapshot(snapshot_id,area_type_id,name,area_code,parent_id,ancestor_path) SELECT snapshot_id,area_type_id,''New'', ''002'',parent_id,ancestor_path FROM electoral_area_snapshot WHERE id = %s', station));
    -- Moving a node to another draft must not bypass the original publication guard.
    PERFORM pg_temp.expect_failure(format('UPDATE electoral_area_snapshot SET snapshot_id = %s WHERE id = %s', other_draft, station));
    PERFORM pg_temp.expect_failure(format('UPDATE electoral_geography_snapshot SET name = ''Changed'' WHERE id = %s', draft));
    PERFORM pg_temp.expect_failure(format('UPDATE electoral_geography_snapshot SET status = ''DRAFT'', published_at = NULL WHERE id = %s', draft));
    PERFORM pg_temp.expect_failure(format('DELETE FROM electoral_geography_snapshot WHERE id = %s', draft));
    PERFORM pg_temp.expect_failure('UPDATE area_type SET name = ''OTHER_STATION'' WHERE name = ''POLLING_STATION''');

    INSERT INTO electoral_geography_snapshot(name, supersedes_snapshot_id) VALUES ('Revision', draft) RETURNING id INTO revision;
    SELECT copy_geography_snapshot(draft, revision) INTO copied;
    ASSERT copied = 7, 'Revision must copy all nodes';
    ASSERT NOT EXISTS (SELECT 1 FROM electoral_area_snapshot a JOIN electoral_area_snapshot p ON p.id = a.parent_id
                      WHERE a.snapshot_id = revision AND p.snapshot_id <> revision), 'Revision has its own parents';
    UPDATE electoral_area_snapshot SET name = 'Corrected station' WHERE snapshot_id = revision AND area_type_id = 7;
    ASSERT (SELECT name FROM electoral_area_snapshot WHERE id = station) = 'POLLING_STATION', 'Original publication unchanged';
    PERFORM pg_temp.expect_failure(format('SELECT copy_geography_snapshot(%s,%s)', draft, revision));
    PERFORM pg_temp.expect_failure(format('SELECT copy_geography_snapshot(%s,%s)', other_draft, revision));

    -- Core acceptance test: operational changes cannot rewrite the snapshot.
    SELECT c.electoral_area_id, c.area_snapshot_id, a.name INTO live_id, historical_id, before_name
        FROM electoral_area_correspondence c JOIN electoral_area_snapshot a ON a.id = c.area_snapshot_id
        WHERE a.snapshot_id = existing AND a.area_type_id = 7 LIMIT 1;
    UPDATE electoral_area SET name = 'Operational rename', parent_id = NULL, ancestor_path = '/' WHERE id = live_id;
    ASSERT (SELECT name FROM electoral_area_snapshot WHERE id = historical_id) = before_name,
        'Historical name is independent of operational name';
    ASSERT (SELECT parent_id IS NOT NULL AND ancestor_path <> '/' FROM electoral_area_snapshot WHERE id = historical_id),
        'Historical ancestry is independent of operational ancestry';
END $$;
ROLLBACK;
