-- V32: officeholder.acquisition becomes a FK into office_entry_reason (V31),
-- replacing the inline VARCHAR CHECK from V14.
-- Backfill maps the old values onto the seeded reasons: ELECTED becomes
-- GENERAL_ELECTION_WINNER (the old value did not distinguish a general
-- election from a by-election, so the general case is assumed — rows won at a
-- by-election must be corrected by hand), APPOINTED and NOMINATED both become
-- APPOINTMENT (a party-list nominee is placed in the seat without a ballot),
-- and SUCCESSION carries over unchanged.
-- V14's CHECK (acquisition <> 'ELECTED' OR candidacy_id IS NOT NULL) is
-- dropped with the column: a CHECK cannot read reason_name from the lookup
-- table, so the application now enforces that a holder entering on an
-- election-winner reason carries a candidacy, as it does for ancestor_path.

-- The view selects acquisition, so it is rebuilt after the column swap.
DROP VIEW officeholder_mandate;

ALTER TABLE officeholder
    ADD COLUMN entry_reason_id BIGINT REFERENCES office_entry_reason (id);

-- No officeholders are seeded, so this is normally a no-op.
UPDATE officeholder oh
SET entry_reason_id = oer.id
FROM office_entry_reason oer
WHERE oer.reason_name = CASE oh.acquisition
                            WHEN 'ELECTED'    THEN 'GENERAL_ELECTION_WINNER'
                            WHEN 'NOMINATED'  THEN 'APPOINTMENT'
                            WHEN 'APPOINTED'  THEN 'APPOINTMENT'
                            WHEN 'SUCCESSION' THEN 'SUCCESSION'
                        END;

ALTER TABLE officeholder
    ALTER COLUMN entry_reason_id SET NOT NULL;

ALTER TABLE officeholder
    DROP COLUMN acquisition;

CREATE INDEX idx_officeholder_entry_reason_id ON officeholder (entry_reason_id);

-- Rebuilt as in V30, with entry_reason_id in place of acquisition.
CREATE VIEW officeholder_mandate AS
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
       s.rank
FROM officeholder oh
LEFT JOIN candidacy ca ON ca.id = oh.candidacy_id
LEFT JOIN contest_result_summary s ON s.candidacy_id = ca.id;
