-- Each measurement is the observed value as of a date within the target period.
-- Its unit and methodology come from the target's indicator. Negative values
-- are valid for indicators such as net change; absence is not represented by 0.
CREATE TABLE performance_measurement (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    target_id        BIGINT NOT NULL REFERENCES performance_target (id),
    measured_on      DATE NOT NULL,
    actual_value     NUMERIC(20,6) NOT NULL CHECK (actual_value <> 'NaN'::numeric),
    source_reference VARCHAR(2048) NOT NULL CHECK (btrim(source_reference) <> ''),
    notes            TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (target_id, measured_on)
);

-- Lock the parent when checking dates so a concurrent period edit cannot
-- invalidate a measurement after it has been checked.
CREATE FUNCTION check_performance_measurement_period() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    target_start DATE;
    target_end DATE;
BEGIN
    SELECT period_start, period_end INTO target_start, target_end
    FROM performance_target WHERE id = NEW.target_id FOR SHARE;
    IF FOUND AND (NEW.measured_on < target_start OR NEW.measured_on > target_end) THEN
        RAISE EXCEPTION 'Measurement date must fall within the target period'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER performance_measurement_period
BEFORE INSERT OR UPDATE ON performance_measurement
FOR EACH ROW EXECUTE FUNCTION check_performance_measurement_period();

-- Period edits must also preserve the dates of existing measurements.
CREATE FUNCTION check_performance_target_period() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM performance_measurement
               WHERE target_id = NEW.id
                 AND (measured_on < NEW.period_start OR measured_on > NEW.period_end)) THEN
        RAISE EXCEPTION 'Target period must contain its measurements'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER performance_target_period
BEFORE UPDATE OF period_start, period_end ON performance_target
FOR EACH ROW EXECUTE FUNCTION check_performance_target_period();
