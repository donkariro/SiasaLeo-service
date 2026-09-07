-- V36: educational_institution — subtype of organization (joined inheritance,
-- same pattern as political_party in V9): reuses the organization id as PK +
-- FK. The institution's name lives in organization_name via
-- organization.official_name (V17), and its accreditation number maps to
-- organization.registration_number, so this table adds only what is specific
-- to the subtype: which kind of institution it is.
--
-- Three "type" columns meet on a row here, and they answer different
-- questions (see V26):
--   party.party_type                          — structural: which tables hold
--                                               the row, for JPA inheritance
--   organization.org_type (V18)               — business: what kind of
--                                               organization this is
--   educational_institution.institution_type  — which kind of educational
--                                               institution (V35)
-- The first two are widened here so a row of this subtype can exist at all;
-- the third is the reference this migration is really about.

-- Structural discriminator: 'EDUCATIONAL_INSTITUTION' joins the chain
-- party -> organization -> educational_institution.
ALTER TABLE party
    DROP CONSTRAINT IF EXISTS party_party_type_check;

ALTER TABLE party
    ADD CONSTRAINT party_party_type_check
        CHECK (party_type IN ('PERSON', 'ORGANIZATION', 'POLITICAL_PARTY',
                              'EDUCATIONAL_INSTITUTION'));

-- Business classification: organization.org_type is NOT NULL (V18), so the
-- lookup needs this value before any educational institution can be inserted.
INSERT INTO organization_type (type_name, description)
VALUES ('EDUCATIONAL_INSTITUTION',
        'An institution that provides education or training and awards the resulting qualifications');

CREATE TABLE educational_institution (
    id               BIGINT PRIMARY KEY REFERENCES organization (id) ON DELETE CASCADE,
    institution_type BIGINT NOT NULL REFERENCES educational_institution_type (id)
);

CREATE INDEX idx_educational_institution_institution_type ON educational_institution (institution_type);
