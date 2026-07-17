-- V6: office — an elective office in the Kenyan leadership structure,
-- classified by the level of government it belongs to (National or County).

CREATE TABLE office (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name         VARCHAR(100) NOT NULL UNIQUE,
    abbreviation VARCHAR(10) NOT NULL UNIQUE,
    description  VARCHAR(255),
    level        VARCHAR(10) NOT NULL CHECK (level IN ('National', 'County'))
);

INSERT INTO office (name, abbreviation, description, level) VALUES
    ('President',                  'PRES', 'Head of State and Government, elected nationally on a joint ticket with the Deputy President', 'National'),
    ('Deputy President',           'DP',   'Principal assistant to the President, elected as running mate on the presidential ticket',     'National'),
    ('Senator',                    'SEN',  'Represents a county in the Senate; protects the interests of counties and their governments',  'National'),
    ('Member of National Assembly','MP',   'Represents a constituency in the National Assembly; legislates and oversees national revenue', 'National'),
    ('Woman Representative',       'WR',   'County woman member of the National Assembly, elected by voters of an entire county',          'National'),
    ('Governor',                   'GOV',  'Chief executive of a county government, elected on a joint ticket with the Deputy Governor',   'County'),
    ('Deputy Governor',            'DG',   'Principal assistant to the Governor, elected as running mate on the gubernatorial ticket',     'County'),
    ('Member of County Assembly',  'MCA',  'Represents a ward in the county assembly; legislates and oversees the county executive',       'County');
