CREATE TABLE work_effort (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    description TEXT NOT NULL CHECK (btrim(description) <> ''),
    type_code VARCHAR(40) NOT NULL REFERENCES work_effort_type(code),
    period_start DATE, period_end DATE,
    date_precision VARCHAR(15) NOT NULL DEFAULT 'UNKNOWN',
    scope_id BIGINT REFERENCES measurement_scope(id),
    recorded_by_account_id BIGINT REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (valid_governance_period(period_start,period_end,date_precision))
);
CREATE TABLE work_effort_policy_area (
    work_effort_id BIGINT NOT NULL REFERENCES work_effort(id),
    policy_area_id BIGINT NOT NULL REFERENCES policy_area(id),
    PRIMARY KEY(work_effort_id,policy_area_id)
);
CREATE TABLE work_effort_asset (
    work_effort_id BIGINT NOT NULL REFERENCES work_effort(id),
    asset_version_id BIGINT NOT NULL REFERENCES digital_asset_version(id),
    caption TEXT, locator VARCHAR(255),
    PRIMARY KEY(work_effort_id,asset_version_id)
);
CREATE TABLE work_effort_participation (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    work_effort_id BIGINT NOT NULL REFERENCES work_effort(id),
    party_id BIGINT NOT NULL REFERENCES party(id),
    role_code VARCHAR(40) NOT NULL REFERENCES participation_role_type(code),
    officeholder_id BIGINT REFERENCES officeholder(id),
    period_start DATE, period_end DATE,
    date_precision VARCHAR(15) NOT NULL DEFAULT 'UNKNOWN',
    asset_version_id BIGINT NOT NULL REFERENCES digital_asset_version(id),
    locator VARCHAR(255),
    recorded_by_account_id BIGINT NOT NULL REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (valid_governance_period(period_start,period_end,date_precision))
);
CREATE INDEX idx_participation_work ON work_effort_participation(work_effort_id);
CREATE INDEX idx_participation_party ON work_effort_participation(party_id,officeholder_id);
CREATE FUNCTION check_participation_holder() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.officeholder_id IS NOT NULL AND NOT EXISTS
 (SELECT 1 FROM officeholder WHERE id=NEW.officeholder_id AND person_id=NEW.party_id) THEN
   RAISE EXCEPTION 'Official participation must name the officeholder person' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER participation_holder BEFORE INSERT OR UPDATE ON work_effort_participation
 FOR EACH ROW EXECUTE FUNCTION check_participation_holder();

CREATE TABLE achievement_claim (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    officeholder_id BIGINT NOT NULL REFERENCES officeholder(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(id,officeholder_id)
);
-- Existing performance_record rows become first revisions of stable claims.
-- Preserve IDs, text, source strings and dates; never infer an author or review.
INSERT INTO achievement_claim(id,officeholder_id,created_at) OVERRIDING SYSTEM VALUE
 SELECT id,officeholder_id,created_at FROM performance_record;
SELECT setval(pg_get_serial_sequence('achievement_claim','id'),
 GREATEST(COALESCE((SELECT max(id) FROM achievement_claim),1),1),EXISTS(SELECT 1 FROM achievement_claim WHERE id>0));
ALTER TABLE performance_record ADD COLUMN claim_id BIGINT;
UPDATE performance_record SET claim_id=id;
ALTER TABLE performance_record ALTER COLUMN claim_id SET NOT NULL;
ALTER TABLE performance_record ADD FOREIGN KEY(claim_id,officeholder_id) REFERENCES achievement_claim(id,officeholder_id);
ALTER TABLE performance_record ADD COLUMN revision_number INTEGER NOT NULL DEFAULT 1 CHECK (revision_number > 0);
ALTER TABLE performance_record ADD UNIQUE(claim_id,revision_number);
ALTER TABLE performance_record ADD COLUMN claimant_party_id BIGINT REFERENCES party(id);
ALTER TABLE performance_record ADD COLUMN recorded_by_account_id BIGINT REFERENCES user_account(id);
ALTER TABLE performance_record ADD COLUMN work_effort_id BIGINT REFERENCES work_effort(id);
ALTER TABLE performance_record ADD COLUMN date_precision VARCHAR(15) NOT NULL DEFAULT 'DAY';
ALTER TABLE performance_record ALTER COLUMN period_start DROP NOT NULL;
ALTER TABLE performance_record ALTER COLUMN period_end DROP NOT NULL;
ALTER TABLE performance_record ALTER COLUMN source_reference DROP NOT NULL;
ALTER TABLE performance_record ADD CHECK(valid_governance_period(period_start,period_end,date_precision));
ALTER TABLE performance_record ADD COLUMN publication_status VARCHAR(15) NOT NULL DEFAULT 'DRAFT'
 CHECK(publication_status IN ('DRAFT','PUBLISHED','WITHDRAWN'));
ALTER TABLE performance_record ADD COLUMN published_at TIMESTAMPTZ;
ALTER TABLE performance_record ADD CHECK ((publication_status='DRAFT')=(published_at IS NULL));
ALTER TABLE performance_record ADD COLUMN scope_id BIGINT REFERENCES measurement_scope(id);
CREATE INDEX idx_claim_holder ON achievement_claim(officeholder_id);
CREATE TABLE claim_evidence (
    record_id BIGINT NOT NULL REFERENCES performance_record(id),
    asset_version_id BIGINT NOT NULL REFERENCES digital_asset_version(id),
    relationship_code VARCHAR(30) NOT NULL REFERENCES evidence_relationship_type(code),
    locator VARCHAR(255), caption TEXT,
    PRIMARY KEY(record_id,asset_version_id,relationship_code)
);
CREATE TABLE claim_policy_area (
    record_id BIGINT NOT NULL REFERENCES performance_record(id),
    policy_area_id BIGINT NOT NULL REFERENCES policy_area(id),
    PRIMARY KEY(record_id,policy_area_id)
);

ALTER TABLE performance_target ADD COLUMN scope_id BIGINT REFERENCES measurement_scope(id);
CREATE TABLE performance_observation (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    indicator_id BIGINT REFERENCES performance_indicator(id),
    actual_value NUMERIC(20,6) CHECK(actual_value <> 'NaN'::numeric),
    qualitative_result TEXT CHECK(qualitative_result IS NULL OR btrim(qualitative_result) <> ''),
    officeholder_id BIGINT REFERENCES officeholder(id),
    work_effort_id BIGINT REFERENCES work_effort(id),
    scope_id BIGINT REFERENCES measurement_scope(id),
    period_start DATE, period_end DATE,
    date_precision VARCHAR(15) NOT NULL DEFAULT 'UNKNOWN',
    source_reference VARCHAR(2048) CHECK(source_reference IS NULL OR btrim(source_reference) <> ''),
    recorded_by_account_id BIGINT REFERENCES user_account(id),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    publication_status VARCHAR(15) NOT NULL DEFAULT 'DRAFT' CHECK(publication_status IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    published_at TIMESTAMPTZ,
    legacy_measurement_id BIGINT UNIQUE REFERENCES performance_measurement(id),
    CHECK(officeholder_id IS NOT NULL OR work_effort_id IS NOT NULL),
    CHECK((actual_value IS NOT NULL AND indicator_id IS NOT NULL AND qualitative_result IS NULL)
       OR (actual_value IS NULL AND indicator_id IS NULL AND qualitative_result IS NOT NULL)),
    CHECK(valid_governance_period(period_start,period_end,date_precision)),
    CHECK((publication_status='DRAFT')=(published_at IS NULL))
);
CREATE INDEX idx_observation_holder ON performance_observation(officeholder_id);
CREATE INDEX idx_observation_work ON performance_observation(work_effort_id);
INSERT INTO performance_observation(indicator_id,actual_value,officeholder_id,scope_id,
 period_start,period_end,date_precision,source_reference,recorded_at,legacy_measurement_id)
 SELECT t.indicator_id,m.actual_value,t.officeholder_id,t.scope_id,m.measured_on,m.measured_on,
 'DAY',m.source_reference,m.created_at,m.id FROM performance_measurement m JOIN performance_target t ON t.id=m.target_id;
CREATE TABLE observation_evidence (
    observation_id BIGINT NOT NULL REFERENCES performance_observation(id),
    asset_version_id BIGINT NOT NULL REFERENCES digital_asset_version(id),
    relationship_code VARCHAR(30) NOT NULL REFERENCES evidence_relationship_type(code),
    locator VARCHAR(255), caption TEXT,
    PRIMARY KEY(observation_id,asset_version_id,relationship_code)
);
-- Links preserve the old comparison context. Unknown historical scopes stay
-- unknown and are not silently considered comparable by new assessments.
CREATE TABLE target_observation (
    target_id BIGINT NOT NULL REFERENCES performance_target(id),
    observation_id BIGINT NOT NULL REFERENCES performance_observation(id),
    PRIMARY KEY(target_id,observation_id)
);
INSERT INTO target_observation SELECT m.target_id,o.id FROM performance_observation o
 JOIN performance_measurement m ON m.id=o.legacy_measurement_id;
