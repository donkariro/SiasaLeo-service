-- V13: provisional_contest_result — results streamed in near-realtime before
-- the electoral commission's official announcement. Same shape as
-- contest_result (V12); recorded_at tracks when each station's figure landed.
-- Rows here are working data: the official record lives in contest_result.

CREATE TABLE provisional_contest_result (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidacy_id       BIGINT NOT NULL REFERENCES candidacy (id),
    polling_station_id BIGINT NOT NULL REFERENCES electoral_area (id),
    vote_count         INTEGER NOT NULL CHECK (vote_count >= 0),
    recorded_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (candidacy_id, polling_station_id)
);

CREATE INDEX idx_provisional_contest_result_polling_station_id
    ON provisional_contest_result (polling_station_id);

-- Provisional per-contest totals per candidate, with share of the vote and rank.
CREATE VIEW provisional_contest_result_summary AS
SELECT ca.contest_id,
       pr.candidacy_id,
       SUM(pr.vote_count)                                     AS total_votes,
       ROUND(100.0 * SUM(pr.vote_count)
             / NULLIF(SUM(SUM(pr.vote_count))
                      OVER (PARTITION BY ca.contest_id), 0),
             2)                                               AS percentage,
       RANK() OVER (PARTITION BY ca.contest_id
                    ORDER BY SUM(pr.vote_count) DESC)         AS rank
FROM provisional_contest_result pr
JOIN candidacy ca ON ca.id = pr.candidacy_id
GROUP BY ca.contest_id, pr.candidacy_id;
