-- V21: candidacy_status — lookup table for the candidacy lifecycle, replacing
-- the inline VARCHAR + CHECK on candidacy.status (V11). Seeded in lifecycle
-- order, from first expression of interest through to the election outcome.
-- candidacy.status becomes a FK into this table.

CREATE TABLE candidacy_status (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    status_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO candidacy_status (status_name, description)
VALUES ('EXPRESSED_INTEREST', 'Person has publicly declared interest in contesting the seat'),
       ('PARTY_PRIMARY', 'Contesting the sponsoring party''s primary elections'),
       ('PARTY_NOMINEE', 'Selected as the party''s nominee for the contest'),
       ('INDEPENDENT_PENDING', 'Running as an independent, pending fulfilment of independent-candidate requirements'),
       ('SUBMITTED_TO_IEBC', 'Nomination papers submitted to the IEBC'),
       ('CLEARED', 'Cleared by the IEBC to contest'),
       ('REJECTED', 'Nomination rejected by the IEBC'),
       ('WITHDRAWN', 'Candidacy withdrawn before the election'),
       ('ON_BALLOT', 'Appears on the ballot for the election'),
       ('ELECTED', 'Won the contest'),
       ('NOT_ELECTED', 'Contested but was not elected');

ALTER TABLE candidacy
    ADD COLUMN status_id BIGINT REFERENCES candidacy_status (id);

-- Map any existing rows from the old V11 status values onto the new
-- lifecycle (no candidacy data is seeded, so this is normally a no-op).
-- DISQUALIFIED has no direct equivalent and maps to REJECTED.
UPDATE candidacy c
SET status_id = cs.id
FROM candidacy_status cs
WHERE cs.status_name = CASE c.status
                           WHEN 'ASPIRANT' THEN 'EXPRESSED_INTEREST'
                           WHEN 'NOMINATED' THEN 'PARTY_NOMINEE'
                           WHEN 'DISQUALIFIED' THEN 'REJECTED'
                           ELSE c.status
                       END;

ALTER TABLE candidacy
    ALTER COLUMN status_id SET NOT NULL;

ALTER TABLE candidacy
    DROP COLUMN status;

ALTER TABLE candidacy
    RENAME COLUMN status_id TO status;

CREATE INDEX idx_candidacy_status ON candidacy (status);
