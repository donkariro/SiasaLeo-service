-- V27: election_type — lookup table for the kind of electoral event, replacing
-- the inline VARCHAR on election_event.type (V5). Seeded with the event types
-- run under the Kenyan electoral calendar, from the scheduled general election
-- through to the off-cycle events that fill or vacate a seat.
-- election_event.type becomes a FK into this table.

CREATE TABLE election_type (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type_name   VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO election_type (type_name, description)
VALUES ('GENERAL', 'Regular election'),
       ('BY_ELECTION', 'Vacancy replacement'),
       ('RUNOFF', 'Second round'),
       ('REPEAT_ELECTION', 'Nullification rerun'),
       ('PARTY_PRIMARY', 'Party nominations'),
       ('RECALL', 'Voters remove office holder');

ALTER TABLE election_event
    ADD COLUMN type_id BIGINT REFERENCES election_type (id);

-- Backfill any existing rows from the old V5 free-text values (no election
-- events are seeded, so this is normally a no-op).
UPDATE election_event ee
SET type_id = et.id
FROM election_type et
WHERE et.type_name = ee.type;

ALTER TABLE election_event
    ALTER COLUMN type_id SET NOT NULL;

ALTER TABLE election_event
    DROP COLUMN type;

ALTER TABLE election_event
    RENAME COLUMN type_id TO type;

CREATE INDEX idx_election_event_type ON election_event (type);
