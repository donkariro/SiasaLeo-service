-- V8: contest — junction of election_event and seat: the race for a specific
-- seat in a specific election event, e.g. 'MP for Changamwe in the 2027
-- General Election'.

CREATE TABLE contest (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    election_event_id BIGINT NOT NULL REFERENCES election_event (id),
    seat_id           BIGINT NOT NULL REFERENCES seat (id),
    description       VARCHAR(255),
    UNIQUE (election_event_id, seat_id)
);

CREATE INDEX idx_contest_seat_id ON contest (seat_id);
