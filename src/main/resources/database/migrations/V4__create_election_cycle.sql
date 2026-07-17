-- V4: Election concepts.
-- election_cycle represents a five-year electoral period bounded by two
-- general election years (from_year inclusive, upto_year the next election year).

CREATE TABLE election_cycle (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name      VARCHAR(100) NOT NULL UNIQUE,
    from_year INT NOT NULL,
    upto_year INT NOT NULL,
    status    VARCHAR(10) NOT NULL CHECK (status IN ('past', 'current', 'future'))
);

INSERT INTO election_cycle (name, from_year, upto_year, status) VALUES
    ('Kenya 2013 General Elections', 2013, 2017, 'past'),
    ('Kenya 2017 General Elections', 2017, 2022, 'past'),
    ('Kenya 2022 General Elections', 2022, 2027, 'current'),
    ('Kenya 2027 General Elections', 2027, 2032, 'future'),
    ('Kenya 2032 General Elections', 2032, 2037, 'future'),
    ('Kenya 2037 General Elections', 2037, 2042, 'future'),
    ('Kenya 2042 General Elections', 2042, 2047, 'future'),
    ('Kenya 2047 General Elections', 2047, 2052, 'future'),
    ('Kenya 2052 General Elections', 2052, 2057, 'future'),
    ('Kenya 2057 General Elections', 2057, 2062, 'future'),
    ('Kenya 2062 General Elections', 2062, 2067, 'future'),
    ('Kenya 2067 General Elections', 2067, 2072, 'future'),
    ('Kenya 2072 General Elections', 2072, 2077, 'future');
