-- V5: election_event — a dated electoral event (e.g. a general election or
-- by-election) occurring within an election_cycle.

CREATE TABLE election_event (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    election_cycle_id BIGINT NOT NULL REFERENCES election_cycle (id),
    election_date     DATE NOT NULL,
    type              VARCHAR(50) NOT NULL,
    status            VARCHAR(50) NOT NULL
);

CREATE INDEX idx_election_event_election_cycle_id ON election_event (election_cycle_id);
