-- V19: politicalparty_official — a person's tenure as an official of a
-- political party (chairperson, secretary-general, treasurer, ...).
-- upto_date IS NULL means the person currently holds the position; earlier
-- rows are kept as history when officials change.

CREATE TABLE politicalparty_official (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    official_id       BIGINT NOT NULL REFERENCES person (id),
    politicalparty_id BIGINT NOT NULL REFERENCES political_party (id),
    from_date         DATE,
    upto_date         DATE,
    position_name     VARCHAR(100) NOT NULL,
    photo             VARCHAR(255),
    about             TEXT,
    CHECK (upto_date IS NULL OR from_date IS NULL OR upto_date >= from_date)
);

CREATE INDEX idx_politicalparty_official_official_id ON politicalparty_official (official_id);
CREATE INDEX idx_politicalparty_official_politicalparty_id ON politicalparty_official (politicalparty_id);
