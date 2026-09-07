-- V35: educational_institution_type — lookup table for the kind of institution
-- an aspirant's qualification was earned at. It is the first table of the
-- education domain, which records the academic and professional background a
-- person brings to a candidacy (Kenyan law sets an educational threshold for
-- some offices, so the institution behind a qualification matters).
-- Seeded up the ladder of the 2-6-3-3 system, from primary through to the
-- university level, then the two kinds that sit outside that ladder: a college
-- offering post-secondary certificates and diplomas, and a professional
-- institution awarding the qualifications a regulated body recognises.

CREATE TABLE educational_institution_type (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type_name   VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO educational_institution_type (type_name, description)
VALUES ('PRIMARY_SCHOOL', 'Basic education up to the end of the primary cycle'),
       ('SECONDARY_SCHOOL', 'Secondary education leading to a school-leaving certificate'),
       ('TVET_INSTITUTION', 'Technical and vocational education and training institution'),
       ('UNIVERSITY', 'Awards degrees at bachelor level and above'),
       ('COLLEGE', 'Post-secondary certificates and diplomas outside the university system'),
       ('PROFESSIONAL_INSTITUTION', 'Awards the professional qualifications of a regulated body');
