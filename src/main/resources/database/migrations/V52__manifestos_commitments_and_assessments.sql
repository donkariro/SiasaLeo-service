CREATE TABLE manifesto (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    issuer_person_id BIGINT NOT NULL REFERENCES person(id),
    candidacy_id BIGINT NOT NULL REFERENCES candidacy(id),
    created_by_account_id BIGINT NOT NULL REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(issuer_person_id,candidacy_id)
);
CREATE TABLE manifesto_version (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    manifesto_id BIGINT NOT NULL REFERENCES manifesto(id),
    version_number INTEGER NOT NULL CHECK(version_number > 0),
    title VARCHAR(255) NOT NULL CHECK(btrim(title) <> ''),
    language VARCHAR(35) NOT NULL DEFAULT 'en' CHECK(btrim(language) <> ''),
    content TEXT NOT NULL CHECK(btrim(content) <> ''),
    publication_status VARCHAR(15) NOT NULL DEFAULT 'DRAFT' CHECK(publication_status IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    published_at TIMESTAMPTZ,
    recorded_by_account_id BIGINT NOT NULL REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(manifesto_id,version_number),
    CHECK((publication_status='DRAFT')=(published_at IS NULL))
);
CREATE TABLE governance_goal (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    manifesto_id BIGINT NOT NULL REFERENCES manifesto(id),
    description TEXT NOT NULL CHECK(btrim(description) <> '')
);
CREATE TABLE commitment (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    manifesto_id BIGINT NOT NULL REFERENCES manifesto(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE commitment_revision (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    commitment_id BIGINT NOT NULL REFERENCES commitment(id),
    revision_number INTEGER NOT NULL CHECK(revision_number > 0),
    wording TEXT NOT NULL CHECK(btrim(wording) <> ''),
    conditions TEXT,
    period_start DATE, period_end DATE,
    date_precision VARCHAR(15) NOT NULL DEFAULT 'UNKNOWN',
    scope_id BIGINT REFERENCES measurement_scope(id),
    recorded_by_account_id BIGINT NOT NULL REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sealed_at TIMESTAMPTZ,
    UNIQUE(commitment_id,revision_number),
    CHECK(valid_governance_period(period_start,period_end,date_precision))
);
CREATE TABLE manifesto_commitment (
    manifesto_version_id BIGINT NOT NULL REFERENCES manifesto_version(id),
    commitment_revision_id BIGINT NOT NULL REFERENCES commitment_revision(id),
    PRIMARY KEY(manifesto_version_id,commitment_revision_id)
);
CREATE TABLE commitment_goal (
    commitment_revision_id BIGINT NOT NULL REFERENCES commitment_revision(id),
    goal_id BIGINT NOT NULL REFERENCES governance_goal(id),
    PRIMARY KEY(commitment_revision_id,goal_id)
);
CREATE TABLE commitment_policy_area (
    commitment_revision_id BIGINT NOT NULL REFERENCES commitment_revision(id),
    policy_area_id BIGINT NOT NULL REFERENCES policy_area(id),
    PRIMARY KEY(commitment_revision_id,policy_area_id)
);
CREATE TABLE commitment_target (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    commitment_revision_id BIGINT NOT NULL REFERENCES commitment_revision(id),
    indicator_id BIGINT NOT NULL REFERENCES performance_indicator(id),
    scope_id BIGINT NOT NULL REFERENCES measurement_scope(id),
    baseline_value NUMERIC(20,6) CHECK(baseline_value <> 'NaN'::numeric),
    target_value NUMERIC(20,6) NOT NULL CHECK(target_value <> 'NaN'::numeric),
    period_start DATE NOT NULL, period_end DATE NOT NULL,
    CHECK(period_end >= period_start),
    UNIQUE(commitment_revision_id,indicator_id,scope_id,period_start,period_end)
);
CREATE TABLE manifesto_asset (
    manifesto_version_id BIGINT NOT NULL REFERENCES manifesto_version(id),
    asset_version_id BIGINT NOT NULL REFERENCES digital_asset_version(id),
    caption TEXT, locator VARCHAR(255),
    PRIMARY KEY(manifesto_version_id,asset_version_id)
);
CREATE TABLE commitment_asset (
    commitment_revision_id BIGINT NOT NULL REFERENCES commitment_revision(id),
    asset_version_id BIGINT NOT NULL REFERENCES digital_asset_version(id),
    caption TEXT, locator VARCHAR(255),
    PRIMARY KEY(commitment_revision_id,asset_version_id)
);
CREATE TABLE publication_event (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    manifesto_version_id BIGINT NOT NULL REFERENCES manifesto_version(id),
    announced_at TIMESTAMPTZ NOT NULL,
    venue TEXT NOT NULL CHECK(btrim(venue) <> ''),
    asset_version_id BIGINT REFERENCES digital_asset_version(id),
    recorded_by_account_id BIGINT NOT NULL REFERENCES user_account(id),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE commitment_delivery_link (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    commitment_revision_id BIGINT NOT NULL REFERENCES commitment_revision(id),
    work_effort_id BIGINT NOT NULL REFERENCES work_effort(id),
    relationship_type VARCHAR(30) NOT NULL CHECK(relationship_type IN ('CONTRIBUTES_TO','IMPLEMENTS')),
    rationale TEXT NOT NULL CHECK(btrim(rationale) <> ''),
    recorded_by_account_id BIGINT NOT NULL REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(commitment_revision_id,work_effort_id)
);
CREATE TABLE performance_assessment (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    record_id BIGINT REFERENCES performance_record(id),
    commitment_revision_id BIGINT REFERENCES commitment_revision(id),
    commitment_target_id BIGINT REFERENCES commitment_target(id),
    observation_id BIGINT REFERENCES performance_observation(id),
    delivery_status VARCHAR(25) CHECK(delivery_status IN ('NOT_STARTED','IN_PROGRESS','FULFILLED','PARTIALLY_FULFILLED','UNFULFILLED','NOT_ASSESSABLE')),
    evidence_status VARCHAR(25) NOT NULL CHECK(evidence_status IN ('UNREVIEWED','SUPPORTED','DISPUTED','INSUFFICIENT_EVIDENCE')),
    criteria TEXT NOT NULL CHECK(btrim(criteria) <> ''),
    rationale TEXT NOT NULL CHECK(btrim(rationale) <> ''),
    reviewed_by_account_id BIGINT NOT NULL REFERENCES user_account(id),
    assessed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    publication_status VARCHAR(15) NOT NULL DEFAULT 'DRAFT' CHECK(publication_status IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    published_at TIMESTAMPTZ,
    CHECK((record_id IS NULL) <> (commitment_revision_id IS NULL)),
    CHECK((commitment_target_id IS NULL)=(observation_id IS NULL)),
    CHECK(commitment_target_id IS NULL OR commitment_revision_id IS NOT NULL),
    CHECK(commitment_revision_id IS NULL OR delivery_status IS NOT NULL),
    CHECK(evidence_status NOT IN ('UNREVIEWED','INSUFFICIENT_EVIDENCE') OR delivery_status IS NULL OR delivery_status='NOT_ASSESSABLE'),
    CHECK((publication_status='DRAFT')=(published_at IS NULL))
);
CREATE TABLE assessment_evidence (
    assessment_id BIGINT NOT NULL REFERENCES performance_assessment(id),
    asset_version_id BIGINT NOT NULL REFERENCES digital_asset_version(id),
    relationship_code VARCHAR(30) NOT NULL REFERENCES evidence_relationship_type(code),
    locator VARCHAR(255), caption TEXT,
    PRIMARY KEY(assessment_id,asset_version_id,relationship_code)
);
CREATE INDEX idx_assessment_claim ON performance_assessment(record_id);
CREATE INDEX idx_assessment_commitment ON performance_assessment(commitment_revision_id);
