-- V18: introduce organization_type as a lookup table and classify each
-- organization via organization.org_type. Seeded with POLITICAL_PARTY, the
-- only organization type in the system so far; every existing organization
-- is a political party (see V10), so all current rows are backfilled with it.

CREATE TABLE organization_type (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type_name   VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO organization_type (type_name, description)
VALUES ('POLITICAL_PARTY',
        'A political organization registered with the ORPP to nominate candidates and contest elections');

ALTER TABLE organization
    ADD COLUMN org_type BIGINT REFERENCES organization_type (id);

UPDATE organization
SET org_type = (SELECT id FROM organization_type WHERE type_name = 'POLITICAL_PARTY');

ALTER TABLE organization
    ALTER COLUMN org_type SET NOT NULL;

CREATE INDEX idx_organization_org_type ON organization (org_type);
