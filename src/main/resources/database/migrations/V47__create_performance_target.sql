-- A target is scoped to an officeholder's tenure and an inclusive period.
-- Person, seat and leadership term are reached through officeholder_id.
CREATE TABLE performance_target (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    officeholder_id  BIGINT NOT NULL REFERENCES officeholder (id),
    indicator_id     BIGINT NOT NULL REFERENCES performance_indicator (id),
    period_start     DATE NOT NULL,
    period_end       DATE NOT NULL,
    baseline_value   NUMERIC(20,6),
    target_value     NUMERIC(20,6) NOT NULL,
    source_reference VARCHAR(2048) NOT NULL CHECK (btrim(source_reference) <> ''),
    notes            TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (period_end >= period_start),
    CHECK (baseline_value IS NULL OR baseline_value <> 'NaN'::numeric),
    CHECK (target_value <> 'NaN'::numeric),
    UNIQUE (officeholder_id, indicator_id, period_start, period_end)
);

CREATE INDEX idx_performance_target_indicator ON performance_target (indicator_id);
