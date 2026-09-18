-- Preserve the current tree as an UNVERIFIED draft, without asserting an election
-- year or changing any existing voter, seat or result foreign keys.
-- Insert top-down: parents and paths use new snapshot IDs, never live-area IDs.
DO $$
DECLARE
    draft_id BIGINT;
    tree_depth INTEGER;
BEGIN
    INSERT INTO electoral_geography_snapshot(name)
        VALUES ('Existing operational geography - provenance unverified') RETURNING id INTO draft_id;
    CREATE TEMP TABLE geography_copy_ids(old_id BIGINT PRIMARY KEY, new_id BIGINT NOT NULL, depth INTEGER NOT NULL) ON COMMIT DROP;
    INSERT INTO geography_copy_ids
        WITH RECURSIVE tree AS (
            SELECT a.id, 0 AS depth FROM electoral_area a WHERE parent_id IS NULL
            UNION ALL
            SELECT a.id, t.depth + 1 FROM electoral_area a JOIN tree t ON a.parent_id = t.id
        ) SELECT id, nextval(pg_get_serial_sequence('electoral_area_snapshot', 'id')), depth FROM tree ORDER BY depth, id;
    IF (SELECT count(*) FROM geography_copy_ids) <> (SELECT count(*) FROM electoral_area) THEN
        RAISE EXCEPTION 'Operational geography has unreachable nodes; cannot create a complete draft';
    END IF;
    ANALYZE geography_copy_ids;
    -- One insert per depth instead of one round trip per node. Parents are fully
    -- inserted before the next level, so both triggers and composite FKs apply.
    FOR tree_depth IN SELECT DISTINCT depth FROM geography_copy_ids ORDER BY depth LOOP
        INSERT INTO electoral_area_snapshot(id, snapshot_id, area_type_id, name, area_code, parent_id,
                                            ancestor_path, source_record_reference)
        OVERRIDING SYSTEM VALUE
        SELECT m.new_id, draft_id, a.area_type_id, a.name, a.area_code, pm.new_id,
               CASE WHEN a.parent_id IS NULL THEN '/' ELSE p.ancestor_path || p.id || '/' END,
               'electoral_area:' || a.id
        FROM geography_copy_ids m JOIN electoral_area a ON a.id = m.old_id
        LEFT JOIN geography_copy_ids pm ON pm.old_id = a.parent_id
        LEFT JOIN electoral_area_snapshot p ON p.id = pm.new_id
        WHERE m.depth = tree_depth ORDER BY m.old_id;
        -- This table started empty in this transaction. Refresh estimates before
        -- the next level so parent lookups do not retain an empty-table scan plan.
        ANALYZE electoral_area_snapshot;
    END LOOP;
    INSERT INTO electoral_area_correspondence(electoral_area_id, area_snapshot_id, evidence_reference)
        SELECT old_id, new_id, 'Copied from operational geography by V44; provenance unverified' FROM geography_copy_ids;
END $$;
