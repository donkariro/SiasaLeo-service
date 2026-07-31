-- V15: voter_registration — junction of person and electoral_area: where a
-- voter is registered to vote. registration_center_id references
-- electoral_area and must point at an area of type REGISTRATION_CENTER
-- (enforced by the application, like polling_station_id on contest_result).
-- Registration is per centre, not per polling station: stations are the
-- election-day streams a centre is split into, one level further down the
-- hierarchy.
-- A person holds one ACTIVE registration at a time (partial unique index);
-- earlier rows are kept as history when a voter transfers to another center
-- or is deregistered.

CREATE TABLE voter_registration (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    person_id              BIGINT NOT NULL REFERENCES person (id),
    registration_center_id BIGINT NOT NULL REFERENCES electoral_area (id),
    registration_date      DATE NOT NULL,
    status                 VARCHAR(20) NOT NULL CHECK (status IN
                               ('ACTIVE', 'TRANSFERRED', 'DEREGISTERED'))
);

CREATE INDEX idx_voter_registration_person_id ON voter_registration (person_id);
CREATE INDEX idx_voter_registration_center_id ON voter_registration (registration_center_id);

-- One active registration per voter.
CREATE UNIQUE INDEX idx_voter_registration_active ON voter_registration (person_id)
    WHERE status = 'ACTIVE';
