-- V34: officeholder gains exit_reason_id, a FK into office_exit_reason (V33),
-- the counterpart to entry_reason_id (V32).
-- Unlike entry_reason_id it is nullable: a sitting holder has not left the
-- seat. The CHECK ties it to end_date, so an occupancy names an exit reason
-- exactly when it has ended and never before.

ALTER TABLE officeholder
    ADD COLUMN exit_reason_id BIGINT REFERENCES office_exit_reason (id);

ALTER TABLE officeholder
    ADD CONSTRAINT chk_officeholder_exit
        CHECK ((end_date IS NULL) = (exit_reason_id IS NULL));

CREATE INDEX idx_officeholder_exit_reason_id ON officeholder (exit_reason_id);

-- Appended to the V32 view so a tenure reads end to end: how the holder
-- entered, and how they left.
CREATE OR REPLACE VIEW officeholder_mandate AS
SELECT oh.id AS officeholder_id,
       oh.seat_id,
       oh.person_id,
       oh.term_id,
       oh.entry_reason_id,
       oh.start_date,
       oh.end_date,
       ca.contest_id,
       ca.political_party_id,
       s.total_votes,
       s.percentage,
       s.rank,
       oh.exit_reason_id
FROM officeholder oh
LEFT JOIN candidacy ca ON ca.id = oh.candidacy_id
LEFT JOIN contest_result_summary s ON s.candidacy_id = ca.id;
