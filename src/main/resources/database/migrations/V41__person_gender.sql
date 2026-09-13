-- V41: person.gender — MALE or FEMALE, the two values the electoral domain
-- actually reasons about.
--
-- This is not a general statement about people; it is the classification
-- Kenyan electoral law runs on. The Woman Representative seat (V6) is defined
-- by it, the two-thirds gender rule constrains party lists and county
-- assemblies by it, and IEBC reports registration and turnout broken down by
-- it. A record without it cannot answer any of those questions, which is why
-- the column exists at all rather than being left to a generic attribute.
--
-- Nullable, because person outlives any one source of it: rows drawn from
-- public records about officeholders and past candidates often do not carry
-- it, and a row created by a user declaring a role records only what that user
-- chose to give. A NULL means "not recorded", never a third value, and the
-- CHECK keeps the column to the two the domain defines.

ALTER TABLE person
    ADD COLUMN gender VARCHAR(6);

ALTER TABLE person
    ADD CONSTRAINT ck_person_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE'));
