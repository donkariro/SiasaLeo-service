-- Document an officeholder's track record independently of platform membership,
-- manifesto promises or numeric targets. The subject is an officeholder/person,
-- not a user_account. This also supports retrospective entries for closed tenures.
CREATE TABLE performance_record (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    officeholder_id  BIGINT NOT NULL REFERENCES officeholder (id),
    title            VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    description      TEXT NOT NULL CHECK (btrim(description) <> ''),
    period_start     DATE NOT NULL,
    period_end       DATE NOT NULL,
    source_reference VARCHAR(2048) NOT NULL CHECK (btrim(source_reference) <> ''),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (period_end >= period_start)
);

CREATE INDEX idx_performance_record_officeholder
    ON performance_record (officeholder_id, period_start);
