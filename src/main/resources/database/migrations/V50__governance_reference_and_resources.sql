-- Shared taxonomy and measurement definitions. Existing performance indicators
-- remain the canonical indicator catalogue, used by both governance modules.
CREATE TABLE policy_area (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE CHECK (btrim(code) <> ''),
    name VARCHAR(200) NOT NULL CHECK (btrim(name) <> ''),
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0
);
INSERT INTO policy_area(code,name,display_order) VALUES
 ('HEALTH','Health',10),('EDUCATION','Education',20),
 ('WATER_SANITATION','Water and Sanitation',30),('AGRICULTURE','Agriculture',40),
 ('TRANSPORT','Transport',50),('HOUSING','Housing',60),('ENERGY','Energy',70),
 ('ENVIRONMENT','Environment',80),('ECONOMY','Economy and Employment',90),
 ('GOVERNANCE','Governance and Accountability',100),('SOCIAL_PROTECTION','Social Protection',110);

CREATE TABLE unit_of_measure (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
INSERT INTO unit_of_measure(code,name) VALUES
 ('COUNT','count'),('KILOMETRE','kilometre'),('PERCENT','percent'),
 ('HOUSEHOLD','household'),('DAY','day'),('HOUR','hour');
-- Preserve existing custom units without guessing conversions or equivalence.
INSERT INTO unit_of_measure(code,name)
 SELECT 'LEGACY_' || md5(unit),unit FROM performance_indicator GROUP BY unit
 ON CONFLICT (name) DO NOTHING;
ALTER TABLE performance_indicator ADD COLUMN unit_id BIGINT REFERENCES unit_of_measure(id);
UPDATE performance_indicator i SET unit_id=u.id FROM unit_of_measure u WHERE u.name=i.unit;
ALTER TABLE performance_indicator ALTER COLUMN unit_id SET NOT NULL;
CREATE FUNCTION resolve_indicator_unit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.unit_id IS NULL THEN SELECT id INTO NEW.unit_id FROM unit_of_measure WHERE name=NEW.unit; END IF;
    IF NOT EXISTS (SELECT 1 FROM unit_of_measure WHERE id=NEW.unit_id AND name=NEW.unit) THEN
        RAISE EXCEPTION 'Indicator unit label and unit reference must agree' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER indicator_unit BEFORE INSERT OR UPDATE ON performance_indicator
 FOR EACH ROW EXECUTE FUNCTION resolve_indicator_unit();

CREATE TABLE work_effort_type(code VARCHAR(40) PRIMARY KEY, name VARCHAR(100) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE);
INSERT INTO work_effort_type(code,name) VALUES ('PROJECT','Project'),('PROGRAMME','Programme'),
 ('LEGISLATIVE_INITIATIVE','Legislative Initiative'),('POLICY_REFORM','Policy Reform'),('OVERSIGHT','Oversight');
CREATE TABLE participation_role_type(code VARCHAR(40) PRIMARY KEY, name VARCHAR(100) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE);
INSERT INTO participation_role_type(code,name) VALUES ('INITIATOR','Initiator'),('SPONSOR','Sponsor'),
 ('FUNDER','Funder'),('IMPLEMENTER','Implementer'),('OVERSIGHT','Oversight Participant');
CREATE TABLE digital_asset_type(code VARCHAR(40) PRIMARY KEY, name VARCHAR(100) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE);
INSERT INTO digital_asset_type(code,name) VALUES ('REPORT','Report'),('INSPECTION_REPORT','Inspection Report'),
 ('MANIFESTO','Manifesto Document'),('PHOTOGRAPH','Photograph'),('SPEECH','Speech Recording'),
 ('DATASET','Dataset'),('OTHER','Other');
CREATE TABLE evidence_relationship_type(code VARCHAR(30) PRIMARY KEY, name VARCHAR(100) NOT NULL);
INSERT INTO evidence_relationship_type VALUES ('SUPPORTS','Supports'),('CHALLENGES','Challenges'),('CONTEXT','Provides Context');

CREATE FUNCTION valid_governance_period(start_on DATE,end_on DATE,precision_name TEXT)
 RETURNS BOOLEAN LANGUAGE sql IMMUTABLE AS $$
 SELECT CASE WHEN precision_name='UNKNOWN' THEN start_on IS NULL AND end_on IS NULL
 WHEN precision_name IN ('DAY','APPROXIMATE','MONTH','YEAR') THEN
     start_on IS NOT NULL AND end_on IS NOT NULL AND end_on >= start_on
     AND (precision_name <> 'YEAR' OR (to_char(start_on,'MM-DD')='01-01' AND to_char(end_on,'MM-DD')='12-31'))
     AND (precision_name <> 'MONTH' OR (extract(day FROM start_on)=1 AND end_on=(date_trunc('month',end_on)+interval '1 month - 1 day')::date))
 ELSE FALSE END
 $$;

CREATE TABLE measurement_scope (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('GLOBAL','GEOGRAPHIC')),
    snapshot_id BIGINT,
    area_id BIGINT,
    population TEXT NOT NULL CHECK (btrim(population) <> ''),
    FOREIGN KEY(snapshot_id,area_id) REFERENCES electoral_area_snapshot(snapshot_id,id),
    CHECK ((kind='GLOBAL' AND snapshot_id IS NULL AND area_id IS NULL) OR
           (kind='GEOGRAPHIC' AND snapshot_id IS NOT NULL AND area_id IS NOT NULL)),
    UNIQUE NULLS NOT DISTINCT(kind,snapshot_id,area_id,population)
);

CREATE TABLE digital_asset (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL CHECK (btrim(title) <> ''),
    description TEXT,
    asset_type VARCHAR(40) NOT NULL REFERENCES digital_asset_type(code),
    created_by_account_id BIGINT REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE digital_asset_version (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asset_id BIGINT NOT NULL REFERENCES digital_asset(id),
    version_number INTEGER NOT NULL CHECK (version_number > 0),
    source_url VARCHAR(2048) CHECK (source_url IS NULL OR source_url ~ '^https?://[^[:space:]]+$'),
    author_party_id BIGINT REFERENCES party(id),
    publisher_party_id BIGINT REFERENCES party(id),
    source_published_on DATE,
    provenance TEXT NOT NULL CHECK (btrim(provenance) <> ''),
    recorded_by_account_id BIGINT REFERENCES user_account(id),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sealed_at TIMESTAMPTZ,
    UNIQUE(asset_id,version_number)
);
CREATE INDEX idx_digital_asset_version_asset ON digital_asset_version(asset_id);
CREATE TABLE stored_file (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asset_version_id BIGINT NOT NULL REFERENCES digital_asset_version(id),
    representation_type VARCHAR(30) NOT NULL CHECK (representation_type IN ('ORIGINAL','PREVIEW','THUMBNAIL','TRANSCRIPT')),
    storage_key VARCHAR(1024) NOT NULL UNIQUE CHECK (btrim(storage_key) <> ''),
    original_filename VARCHAR(255) NOT NULL CHECK (btrim(original_filename) <> ''),
    media_type VARCHAR(150) NOT NULL CHECK (btrim(media_type) <> ''),
    byte_size BIGINT NOT NULL CHECK (byte_size > 0),
    sha256 VARCHAR(64) NOT NULL CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_stored_file_version ON stored_file(asset_version_id);
CREATE TABLE person_digital_asset (
    person_id BIGINT NOT NULL REFERENCES person(id),
    asset_version_id BIGINT NOT NULL REFERENCES digital_asset_version(id),
    purpose VARCHAR(100) NOT NULL,
    caption TEXT,
    PRIMARY KEY(person_id,asset_version_id,purpose)
);

-- Offices already represent categories. Scope their uniqueness to a country
-- without manufacturing a new OfficeType. Kenya is the existing seed context.
ALTER TABLE office ADD COLUMN country_code VARCHAR(2) NOT NULL DEFAULT 'KE'
 CHECK (country_code ~ '^[A-Z]{2}$');
ALTER TABLE office DROP CONSTRAINT office_name_key;
ALTER TABLE office DROP CONSTRAINT office_abbreviation_key;
ALTER TABLE office ADD UNIQUE(country_code,name);
ALTER TABLE office ADD UNIQUE(country_code,abbreviation);
