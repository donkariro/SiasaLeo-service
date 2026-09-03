-- V29: leadership_term — the constitutional term of a seat: the period the
-- seat is mandated to be occupied, bounded by start_date and end_date.
-- The term belongs to the seat, not to any candidate or election: who actually
-- sits, and for how long, is recorded by officeholder (V14, V30), which points
-- at the term it falls within. end_date IS NULL means the term is still running.
-- cycle_id names the election_cycle the term runs under and is nullable: a term
-- can exist outside any cycle we track, e.g. one predating the cycles seeded in
-- V4, or a partial term created by a by-election, nullification or succession.

CREATE TABLE leadership_term (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    seat_id    BIGINT NOT NULL REFERENCES seat (id),
    cycle_id   BIGINT REFERENCES election_cycle (id),
    start_date DATE NOT NULL,
    end_date   DATE,
    CHECK (end_date IS NULL OR end_date >= start_date),
    -- One term per seat per cycle. Terms outside a tracked cycle carry a NULL
    -- cycle_id, which this constraint does not restrict.
    UNIQUE (seat_id, cycle_id)
);

-- seat_id needs no index of its own: the UNIQUE (seat_id, cycle_id) constraint
-- already indexes it as the leading column.
CREATE INDEX idx_leadership_term_cycle_id ON leadership_term (cycle_id);

-- A seat has at most one running term: end_date IS NULL means it is current.
CREATE UNIQUE INDEX idx_leadership_term_current ON leadership_term (seat_id)
    WHERE end_date IS NULL;
