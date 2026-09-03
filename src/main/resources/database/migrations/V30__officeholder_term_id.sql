-- V30: officeholder points at leadership_term instead of election_cycle.
-- V14 used term_cycle_id because, on the Kenyan five-year calendar, the cycle
-- stood in for the term of office. leadership_term (V29) now models that term
-- in its own right — the seat's constitutional mandate period, independent of
-- any candidate or election — so the cycle reference here is redundant:
-- officeholder keeps only the actual occupancy dates and names the term it
-- falls within. The cycle is still reachable as leadership_term.cycle_id.

-- The view selects term_cycle_id, so it is rebuilt after the column swap.
DROP VIEW officeholder_mandate;

ALTER TABLE officeholder
    ADD COLUMN term_id BIGINT REFERENCES leadership_term (id);

-- Backfill: give every (seat, cycle) pair already recorded on officeholder a
-- leadership_term, spanning the occupancies it covers, then repoint the rows
-- at it. No officeholders are seeded, so this is normally a no-op.
INSERT INTO leadership_term (seat_id, cycle_id, start_date, end_date)
SELECT oh.seat_id,
       oh.term_cycle_id,
       MIN(oh.start_date),
       CASE WHEN COUNT(*) FILTER (WHERE oh.end_date IS NULL) > 0
            THEN NULL
            ELSE MAX(oh.end_date)
       END
FROM officeholder oh
GROUP BY oh.seat_id, oh.term_cycle_id;

UPDATE officeholder oh
SET term_id = lt.id
FROM leadership_term lt
WHERE lt.seat_id = oh.seat_id
  AND lt.cycle_id = oh.term_cycle_id;

ALTER TABLE officeholder
    ALTER COLUMN term_id SET NOT NULL;

ALTER TABLE officeholder
    DROP COLUMN term_cycle_id;

CREATE INDEX idx_officeholder_term_id ON officeholder (term_id);

-- Rebuilt as in V14, with term_id in place of term_cycle_id.
CREATE VIEW officeholder_mandate AS
SELECT oh.id AS officeholder_id,
       oh.seat_id,
       oh.person_id,
       oh.term_id,
       oh.acquisition,
       oh.start_date,
       oh.end_date,
       ca.contest_id,
       ca.political_party_id,
       s.total_votes,
       s.percentage,
       s.rank
FROM officeholder oh
LEFT JOIN candidacy ca ON ca.id = oh.candidacy_id
LEFT JOIN contest_result_summary s ON s.candidacy_id = ca.id;
