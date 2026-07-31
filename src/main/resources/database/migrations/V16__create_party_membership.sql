-- V16: party_membership — junction of person and political_party: a person's
-- membership in a party over time. end_date IS NULL means the membership is
-- current; earlier rows are kept as history when a member resigns or defects.
-- Under the Political Parties Act a person may belong to at most one party at
-- a time, hence the partial unique index on person_id.

CREATE TABLE party_membership (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    person_id          BIGINT NOT NULL REFERENCES person (id),
    political_party_id BIGINT NOT NULL REFERENCES political_party (id),
    start_date         DATE NOT NULL,
    end_date           DATE,
    CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_party_membership_person_id ON party_membership (person_id);
CREATE INDEX idx_party_membership_political_party_id ON party_membership (political_party_id);

-- At most one current membership per person.
CREATE UNIQUE INDEX idx_party_membership_current ON party_membership (person_id)
    WHERE end_date IS NULL;
