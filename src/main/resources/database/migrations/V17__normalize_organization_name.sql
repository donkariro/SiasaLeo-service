-- V17: normalize organization names into their own table with a validity
-- period. Each name lives in organization_name with from_date/upto_date;
-- organization.official_name references the row currently in force
-- (upto_date NULL). Replaces the old organization.name text column.

CREATE TABLE organization_name (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    from_date DATE,
    upto_date DATE
);

ALTER TABLE organization
    ADD COLUMN official_name BIGINT REFERENCES organization_name (id);

-- Carry the seeded names across. For political parties the current name has
-- been official since ORPP registration, so registered_on becomes from_date;
-- other organizations get a NULL from_date (unknown).
DO $$
DECLARE
    r         RECORD;
    v_name_id BIGINT;
BEGIN
    FOR r IN
        SELECT o.id, o.name, pp.registered_on
        FROM organization o
        LEFT JOIN political_party pp ON pp.id = o.id
    LOOP
        INSERT INTO organization_name (name, from_date)
        VALUES (r.name, r.registered_on)
        RETURNING id INTO v_name_id;

        UPDATE organization SET official_name = v_name_id WHERE id = r.id;
    END LOOP;
END $$;

ALTER TABLE organization
    ALTER COLUMN official_name SET NOT NULL;

ALTER TABLE organization
    DROP COLUMN name;

CREATE INDEX idx_organization_official_name ON organization (official_name);
