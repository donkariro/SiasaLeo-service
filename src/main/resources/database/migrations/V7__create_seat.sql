-- V7: seat — junction of electoral_area and office: a concrete elective seat,
-- e.g. 'President of the Republic of Kenya', 'MP for Changamwe'.
-- Seats are derived from the data already seeded in V3/V6: national offices
-- attach to the COUNTRY area, county-wide offices to each COUNTY, MP to each
-- CONSTITUENCY and MCA to each WARD.

CREATE TABLE seat (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    electoral_area_id BIGINT NOT NULL REFERENCES electoral_area (id),
    office_id         BIGINT NOT NULL REFERENCES office (id),
    description       VARCHAR(255),
    UNIQUE (electoral_area_id, office_id)
);

CREATE INDEX idx_seat_office_id ON seat (office_id);

-- National seats: President and Deputy President of the Republic of Kenya.
INSERT INTO seat (electoral_area_id, office_id, description)
SELECT ea.id, o.id, o.name || ' of the Republic of Kenya'
FROM electoral_area ea
JOIN area_type t ON t.id = ea.area_type_id AND t.name = 'COUNTRY'
JOIN office o ON o.abbreviation IN ('PRES', 'DP');

-- County-wide seats: Senator, Woman Representative, Governor, Deputy Governor.
INSERT INTO seat (electoral_area_id, office_id, description)
SELECT ea.id, o.id,
       CASE o.abbreviation
           WHEN 'SEN' THEN 'Senator for ' || INITCAP(ea.name)
           WHEN 'WR'  THEN 'Woman Representative for ' || INITCAP(ea.name)
           WHEN 'GOV' THEN 'Governor of ' || INITCAP(ea.name)
           WHEN 'DG'  THEN 'Deputy Governor of ' || INITCAP(ea.name)
       END
FROM electoral_area ea
JOIN area_type t ON t.id = ea.area_type_id AND t.name = 'COUNTY'
JOIN office o ON o.abbreviation IN ('SEN', 'WR', 'GOV', 'DG');

-- Constituency seats: Member of National Assembly.
INSERT INTO seat (electoral_area_id, office_id, description)
SELECT ea.id, o.id, 'MP for ' || INITCAP(ea.name)
FROM electoral_area ea
JOIN area_type t ON t.id = ea.area_type_id AND t.name = 'CONSTITUENCY'
JOIN office o ON o.abbreviation = 'MP';

-- Ward seats: Member of County Assembly.
INSERT INTO seat (electoral_area_id, office_id, description)
SELECT ea.id, o.id, 'MCA for ' || INITCAP(ea.name)
FROM electoral_area ea
JOIN area_type t ON t.id = ea.area_type_id AND t.name = 'WARD'
JOIN office o ON o.abbreviation = 'MCA';
