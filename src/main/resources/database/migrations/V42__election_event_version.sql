-- Protect election lifecycle updates against concurrent stale writes.
ALTER TABLE election_event ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
