-- Historical geography is independent of the operational electoral_area tree.
CREATE TABLE electoral_geography_snapshot (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL CHECK (btrim(name) <> ''),
    reference_date DATE,
    source_reference VARCHAR(2048),
    source_sha256 VARCHAR(64) CHECK (source_sha256 ~ '^[0-9a-f]{64}$'),
    supersedes_snapshot_id BIGINT REFERENCES electoral_geography_snapshot(id),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK ((status = 'DRAFT' AND published_at IS NULL) OR
           (status = 'PUBLISHED' AND published_at IS NOT NULL)),
    CHECK (supersedes_snapshot_id IS DISTINCT FROM id)
);

CREATE TABLE electoral_area_snapshot (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    snapshot_id BIGINT NOT NULL REFERENCES electoral_geography_snapshot(id),
    area_type_id BIGINT NOT NULL REFERENCES area_type(id),
    name VARCHAR(255) NOT NULL CHECK (btrim(name) <> ''),
    area_code VARCHAR(255) CHECK (area_code IS NULL OR btrim(area_code) <> ''),
    parent_id BIGINT,
    ancestor_path VARCHAR(500) NOT NULL,
    source_record_reference VARCHAR(1024),
    UNIQUE (snapshot_id, id),
    FOREIGN KEY (snapshot_id, parent_id) REFERENCES electoral_area_snapshot(snapshot_id, id),
    CHECK (parent_id IS DISTINCT FROM id)
);
CREATE UNIQUE INDEX uq_snapshot_root ON electoral_area_snapshot(snapshot_id) WHERE parent_id IS NULL;
-- Codes are local to an area type and parent, not globally unique.
CREATE UNIQUE INDEX uq_snapshot_sibling_code
    ON electoral_area_snapshot(snapshot_id, parent_id, area_type_id, area_code)
    WHERE area_code IS NOT NULL;
CREATE INDEX idx_area_snapshot_children ON electoral_area_snapshot(snapshot_id, parent_id);
CREATE INDEX idx_area_snapshot_type ON electoral_area_snapshot(snapshot_id, area_type_id, id);
CREATE INDEX idx_area_snapshot_path
    ON electoral_area_snapshot(snapshot_id, ancestor_path varchar_pattern_ops);

-- Matching can be reviewed independently of an immutable historical publication.
CREATE TABLE electoral_area_correspondence (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    electoral_area_id BIGINT NOT NULL REFERENCES electoral_area(id),
    area_snapshot_id BIGINT NOT NULL UNIQUE REFERENCES electoral_area_snapshot(id),
    evidence_reference VARCHAR(2048) NOT NULL CHECK (btrim(evidence_reference) <> ''),
    reviewed_at TIMESTAMPTZ
);
CREATE INDEX idx_area_correspondence_operational ON electoral_area_correspondence(electoral_area_id);

CREATE FUNCTION guard_snapshot_area() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    snapshot_status TEXT;
    node_type TEXT;
    parent_type TEXT;
    required_parent TEXT;
    parent_path TEXT;
    target_snapshot BIGINT;
BEGIN
    target_snapshot := CASE WHEN TG_OP = 'DELETE' THEN OLD.snapshot_id ELSE NEW.snapshot_id END;
    -- Every writer uses the same lock as publication, including direct SQL imports.
    SELECT status INTO snapshot_status FROM electoral_geography_snapshot
        WHERE id = target_snapshot FOR UPDATE;
    IF snapshot_status IS DISTINCT FROM 'DRAFT' THEN
        RAISE EXCEPTION 'Only draft snapshot areas can be changed';
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    IF TG_OP = 'UPDATE' AND
       (NEW.id IS DISTINCT FROM OLD.id OR NEW.snapshot_id IS DISTINCT FROM OLD.snapshot_id OR
        NEW.parent_id IS DISTINCT FROM OLD.parent_id OR NEW.area_type_id IS DISTINCT FROM OLD.area_type_id) THEN
        RAISE EXCEPTION 'Snapshot node identity, type and parent are immutable; rebuild the draft branch';
    END IF;
    SELECT name INTO node_type FROM area_type WHERE id = NEW.area_type_id;
    IF node_type = 'WORLD' THEN
        IF NEW.parent_id IS NOT NULL OR NEW.ancestor_path <> '/' THEN
            RAISE EXCEPTION 'WORLD must be a root with path /';
        END IF;
    ELSE
        required_parent := CASE node_type
            WHEN 'COUNTRY' THEN 'WORLD' WHEN 'COUNTY' THEN 'COUNTRY'
            WHEN 'CONSTITUENCY' THEN 'COUNTY' WHEN 'WARD' THEN 'CONSTITUENCY'
            WHEN 'REGISTRATION_CENTER' THEN 'WARD' WHEN 'POLLING_STATION' THEN 'REGISTRATION_CENTER' END;
        SELECT t.name, a.ancestor_path INTO parent_type, parent_path
            FROM electoral_area_snapshot a JOIN area_type t ON t.id = a.area_type_id
            WHERE a.snapshot_id = NEW.snapshot_id AND a.id = NEW.parent_id;
        IF required_parent IS NULL OR parent_type IS DISTINCT FROM required_parent THEN
            RAISE EXCEPTION 'Invalid parent type or parent from another snapshot';
        END IF;
        IF NEW.ancestor_path <> parent_path || NEW.parent_id || '/' THEN
            RAISE EXCEPTION 'Snapshot ancestor path does not match parent';
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_snapshot_area BEFORE INSERT OR UPDATE OR DELETE ON electoral_area_snapshot
    FOR EACH ROW EXECUTE FUNCTION guard_snapshot_area();

CREATE FUNCTION guard_geography_snapshot() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP <> 'INSERT' AND OLD.status = 'PUBLISHED' THEN
        RAISE EXCEPTION 'Published geography snapshots are immutable';
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    IF TG_OP = 'INSERT' AND NEW.status <> 'DRAFT' THEN
        RAISE EXCEPTION 'Snapshots must be created as drafts';
    END IF;
    IF NEW.supersedes_snapshot_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM electoral_geography_snapshot WHERE id = NEW.supersedes_snapshot_id AND status = 'PUBLISHED'
    ) THEN RAISE EXCEPTION 'A revision must reference a published snapshot'; END IF;
    IF TG_OP = 'UPDATE' AND (NEW.id IS DISTINCT FROM OLD.id OR
        NEW.supersedes_snapshot_id IS DISTINCT FROM OLD.supersedes_snapshot_id) THEN
        RAISE EXCEPTION 'Snapshot identity and predecessor are immutable';
    END IF;
    IF NEW.status = 'PUBLISHED' THEN
        IF NEW.source_reference IS NULL OR btrim(NEW.source_reference) = '' OR NEW.source_sha256 IS NULL THEN
            RAISE EXCEPTION 'Publication requires a source reference and SHA-256 checksum';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM electoral_area_snapshot WHERE snapshot_id = NEW.id AND parent_id IS NULL)
           OR NOT EXISTS (SELECT 1 FROM electoral_area_snapshot a JOIN area_type t ON t.id = a.area_type_id
                          WHERE a.snapshot_id = NEW.id AND t.name = 'POLLING_STATION') THEN
            RAISE EXCEPTION 'Publication requires a rooted polling station hierarchy';
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_geography_snapshot BEFORE INSERT OR UPDATE OR DELETE ON electoral_geography_snapshot
    FOR EACH ROW EXECUTE FUNCTION guard_geography_snapshot();

-- Shared reference data must not change the meaning of historical nodes.
CREATE FUNCTION guard_snapshot_area_type() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.name IS DISTINCT FROM OLD.name AND EXISTS (
        SELECT 1 FROM electoral_area_snapshot WHERE area_type_id = OLD.id
    ) THEN RAISE EXCEPTION 'Cannot rename an area type used by a geography snapshot'; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER guard_snapshot_area_type BEFORE UPDATE ON area_type
    FOR EACH ROW EXECUTE FUNCTION guard_snapshot_area_type();

-- Used by the revision endpoint; each new node gets its own ID and parent path.
CREATE FUNCTION copy_geography_snapshot(source_id BIGINT, target_id BIGINT) RETURNS BIGINT LANGUAGE plpgsql AS $$
DECLARE
    tree_depth INTEGER;
    copied BIGINT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM electoral_geography_snapshot WHERE id = source_id AND status = 'PUBLISHED') THEN
        RAISE EXCEPTION 'Source must be published';
    END IF;
    PERFORM 1 FROM electoral_geography_snapshot
        WHERE id = target_id AND status = 'DRAFT' AND supersedes_snapshot_id = source_id FOR UPDATE;
    IF NOT FOUND OR EXISTS (SELECT 1 FROM electoral_area_snapshot WHERE snapshot_id = target_id) THEN
        RAISE EXCEPTION 'Target must be an empty draft';
    END IF;
    CREATE TEMP TABLE IF NOT EXISTS snapshot_revision_ids(old_id BIGINT PRIMARY KEY, new_id BIGINT NOT NULL, depth INTEGER NOT NULL) ON COMMIT DROP;
    TRUNCATE snapshot_revision_ids;
    INSERT INTO snapshot_revision_ids
        SELECT id, nextval(pg_get_serial_sequence('electoral_area_snapshot', 'id')),
               length(ancestor_path) - length(replace(ancestor_path, '/', '')) - 1
        FROM electoral_area_snapshot WHERE snapshot_id = source_id ORDER BY id;
    ANALYZE snapshot_revision_ids;
    FOR tree_depth IN SELECT DISTINCT depth FROM snapshot_revision_ids ORDER BY depth LOOP
        INSERT INTO electoral_area_snapshot(id, snapshot_id, area_type_id, name, area_code, parent_id, ancestor_path, source_record_reference)
        OVERRIDING SYSTEM VALUE
        SELECT m.new_id, target_id, a.area_type_id, a.name, a.area_code, pm.new_id,
               CASE WHEN a.parent_id IS NULL THEN '/' ELSE p.ancestor_path || p.id || '/' END,
               a.source_record_reference
        FROM snapshot_revision_ids m JOIN electoral_area_snapshot a ON a.id = m.old_id
        LEFT JOIN snapshot_revision_ids pm ON pm.old_id = a.parent_id
        LEFT JOIN electoral_area_snapshot p ON p.id = pm.new_id
        WHERE m.depth = tree_depth ORDER BY m.old_id;
        ANALYZE electoral_area_snapshot;
    END LOOP;
    -- Copy matches as suggestions; a changed revision requires fresh review.
    INSERT INTO electoral_area_correspondence(electoral_area_id, area_snapshot_id, evidence_reference)
        SELECT c.electoral_area_id, m.new_id, 'Copied from snapshot ' || source_id || '; review required'
        FROM snapshot_revision_ids m JOIN electoral_area_correspondence c ON c.area_snapshot_id = m.old_id;
    SELECT count(*) INTO copied FROM snapshot_revision_ids;
    RETURN copied;
END $$;
