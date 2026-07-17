-- V11: candidacy — a person running in a contest, normally sponsored by a
-- political party. political_party_id is nullable to allow independent
-- candidates.

CREATE TABLE candidacy (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    person_id          BIGINT NOT NULL REFERENCES person (id),
    contest_id         BIGINT NOT NULL REFERENCES contest (id),
    political_party_id BIGINT REFERENCES political_party (id),
    status             VARCHAR(20) NOT NULL CHECK (status IN
                           ('ASPIRANT', 'NOMINATED', 'CLEARED', 'REJECTED',
                            'WITHDRAWN', 'DISQUALIFIED')),
    UNIQUE (person_id, contest_id)
);

CREATE INDEX idx_candidacy_contest_id ON candidacy (contest_id);
CREATE INDEX idx_candidacy_political_party_id ON candidacy (political_party_id);
