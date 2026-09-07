-- V37: education_level — lookup table for the level of an award, the second
-- half of the education domain's vocabulary: educational_institution_type
-- (V35) says what kind of institution taught it, this says how far it goes.
-- The two are related but not derivable from each other — a university can
-- award a postgraduate diploma, a college a certificate or a diploma — so a
-- qualification carries both.
--
-- level_order ranks the levels so an educational threshold can be tested by
-- comparison ('a degree or above') instead of by listing the levels that
-- qualify. It is deliberately not the id: ids are assigned by insertion and
-- would silently misrank any level added later.
--
-- It is NULL for PROFESSIONAL_QUALIFICATION, the one entry that is not a rung
-- on the ladder — a professional body's award has no agreed standing against
-- an academic degree, and NULL says that rather than inventing one. This also
-- keeps threshold queries honest, since NULL fails both >= and <, so an
-- off-ladder qualification is never counted as meeting a degree requirement
-- by accident. For display, ORDER BY level_order puts it last (Postgres sorts
-- NULLs last when ascending), which is the reading order of the ladder.

CREATE TABLE education_level (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    level_name  VARCHAR(50) NOT NULL UNIQUE,
    level_order INT UNIQUE,
    description VARCHAR(255)
);

INSERT INTO education_level (level_name, level_order, description)
VALUES ('PRIMARY', 1, 'Completed the primary cycle of basic education'),
       ('SECONDARY', 2, 'Completed secondary education and holds a school-leaving certificate'),
       ('CERTIFICATE', 3, 'Post-secondary certificate, typically from a TVET institution or college'),
       ('DIPLOMA', 4, 'Post-secondary diploma, one level above a certificate'),
       ('BACHELORS_DEGREE', 5, 'First degree awarded by a university'),
       ('POSTGRADUATE_DIPLOMA', 6, 'Diploma taken after a first degree, often for professional entry'),
       ('MASTERS_DEGREE', 7, 'Second-cycle university degree taken after a first degree'),
       ('DOCTORATE', 8, 'Highest academic degree, awarded on completion of doctoral research'),
       ('PROFESSIONAL_QUALIFICATION', NULL, 'Awarded by a professional body rather than on the academic ladder');
