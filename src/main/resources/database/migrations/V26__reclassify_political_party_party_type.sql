-- V26: make party_type carry the concrete subtype so the joined-inheritance
-- chain party -> organization -> political_party can be mapped in JPA.
--
-- V1 wrote the CHECK before political_party existed (V9), so every party
-- seeded by V10 was classified as the intermediate 'ORGANIZATION'. With only
-- two values a persistence provider cannot tell a plain organization from a
-- political party by discriminator alone, so the third level widens the
-- constraint and the seeded parties are reclassified.
--
-- party_type is the structural discriminator (which tables hold the row);
-- organization.org_type (V18) stays the business classification.

ALTER TABLE party
    DROP CONSTRAINT IF EXISTS party_party_type_check;

ALTER TABLE party
    ADD CONSTRAINT party_party_type_check
        CHECK (party_type IN ('PERSON', 'ORGANIZATION', 'POLITICAL_PARTY'));

UPDATE party
SET party_type = 'POLITICAL_PARTY'
WHERE id IN (SELECT id FROM political_party);
