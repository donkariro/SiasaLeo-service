-- V28: election_status — lookup table for the lifecycle of an electoral event,
-- replacing the inline VARCHAR on election_event.status (V5). Seeded in
-- lifecycle order: an event is gazetted, held, and concluded, with the two
-- terminal states that stop it short — nullification by a court (which is
-- rerun as a REPEAT_ELECTION, see V27) and cancellation before polling.
-- election_event.status becomes a FK into this table.

CREATE TABLE election_status (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    status_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO election_status (status_name, description)
VALUES ('SCHEDULED', 'Gazetted for a future date; polling has not yet opened'),
       ('ONGOING', 'Polling is underway'),
       ('COMPLETED', 'Polling closed and the results were declared'),
       ('NULLIFIED', 'Annulled after the fact, to be rerun as a repeat election'),
       ('CANCELLED', 'Called off before polling took place');

ALTER TABLE election_event
    ADD COLUMN status_id BIGINT REFERENCES election_status (id);

-- Backfill any existing rows from the old V5 free-text values (no election
-- events are seeded, so this is normally a no-op).
UPDATE election_event ee
SET status_id = es.id
FROM election_status es
WHERE es.status_name = ee.status;

ALTER TABLE election_event
    ALTER COLUMN status_id SET NOT NULL;

ALTER TABLE election_event
    DROP COLUMN status;

ALTER TABLE election_event
    RENAME COLUMN status_id TO status;

CREATE INDEX idx_election_event_status ON election_event (status);
