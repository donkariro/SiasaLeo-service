-- V14: officeholder — who holds a seat during a term. term_cycle_id names the
-- election_cycle in its role as the term of office: in Kenya every elective
-- mandate runs on the shared five-year cycle, so the cycle IS the term period,
-- while the officeholder row itself is the tenure (start/end dates capture
-- late swearing-in, death in office, impeachment, succession).
-- candidacy_id links to the winning candidacy when the seat was won at the
-- ballot; NULL for nominated/appointed/succession holders. Consistency of the
-- candidacy's person and seat with person_id/seat_id is enforced by the
-- application, like ancestor_path and polling_station_id. Vote totals, share
-- and rank are never stored here — they come from contest_result_summary via
-- candidacy_id (see officeholder_mandate below).

CREATE TABLE officeholder (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    seat_id       BIGINT NOT NULL REFERENCES seat (id),
    person_id     BIGINT NOT NULL REFERENCES person (id),
    term_cycle_id BIGINT NOT NULL REFERENCES election_cycle (id),
    candidacy_id  BIGINT UNIQUE REFERENCES candidacy (id),
    acquisition   VARCHAR(20) NOT NULL CHECK (acquisition IN
                      ('ELECTED', 'NOMINATED', 'APPOINTED', 'SUCCESSION')),
    start_date    DATE NOT NULL,
    end_date      DATE,
    CHECK (acquisition <> 'ELECTED' OR candidacy_id IS NOT NULL)
);

CREATE INDEX idx_officeholder_seat_id ON officeholder (seat_id);
CREATE INDEX idx_officeholder_person_id ON officeholder (person_id);
CREATE INDEX idx_officeholder_term_cycle_id ON officeholder (term_cycle_id);

-- A seat has at most one sitting holder: end_date IS NULL means in office.
CREATE UNIQUE INDEX idx_officeholder_sitting ON officeholder (seat_id)
    WHERE end_date IS NULL;

-- The electoral mandate behind each tenure: result columns are NULL for
-- holders who did not win a contest.
CREATE VIEW officeholder_mandate AS
SELECT oh.id AS officeholder_id,
       oh.seat_id,
       oh.person_id,
       oh.term_cycle_id,
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
