-- V12: contest_result — official results as announced by the electoral
-- commission: one row per candidacy per polling station. polling_station_id
-- references electoral_area and must point at an area of type POLLING_STATION
-- (enforced by the application, like ancestor_path).
-- Percentage and rank are derived, not stored: contest_result_summary
-- computes them per contest, so they can never disagree with the station rows.

CREATE TABLE contest_result (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidacy_id       BIGINT NOT NULL REFERENCES candidacy (id),
    polling_station_id BIGINT NOT NULL REFERENCES electoral_area (id),
    vote_count         INTEGER NOT NULL CHECK (vote_count >= 0),
    UNIQUE (candidacy_id, polling_station_id)
);

CREATE INDEX idx_contest_result_polling_station_id ON contest_result (polling_station_id);

-- Official per-contest totals per candidate, with share of the vote and rank.
CREATE VIEW contest_result_summary AS
SELECT ca.contest_id,
       cr.candidacy_id,
       SUM(cr.vote_count)                                     AS total_votes,
       ROUND(100.0 * SUM(cr.vote_count)
             / NULLIF(SUM(SUM(cr.vote_count))
                      OVER (PARTITION BY ca.contest_id), 0),
             2)                                               AS percentage,
       RANK() OVER (PARTITION BY ca.contest_id
                    ORDER BY SUM(cr.vote_count) DESC)         AS rank
FROM contest_result cr
JOIN candidacy ca ON ca.id = cr.candidacy_id
GROUP BY ca.contest_id, cr.candidacy_id;
