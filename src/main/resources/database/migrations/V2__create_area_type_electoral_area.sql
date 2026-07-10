-- V2: Geographic location model (Silverston/Hay style).
-- area_type classifies each electoral_area; electoral_area is a self-referencing
-- hierarchy (parent_id) with a materialized ancestor_path for fast subtree reads.

CREATE TABLE area_type (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE electoral_area (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    -- Official code from the source data (kept as string to preserve leading zeros, e.g. '001').
    area_code     VARCHAR(255),
    area_type_id  BIGINT NOT NULL REFERENCES area_type (id),
    parent_id     BIGINT REFERENCES electoral_area (id),
    -- Materialized path of ancestor ids, e.g. '/1/4/27/'; kept in sync by the application.
    ancestor_path VARCHAR(500),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_electoral_area_parent_id ON electoral_area (parent_id);
CREATE INDEX idx_electoral_area_area_type_id ON electoral_area (area_type_id);
-- varchar_pattern_ops enables index use for prefix searches: ancestor_path LIKE '/1/4/%'
CREATE INDEX idx_electoral_area_ancestor_path ON electoral_area (ancestor_path varchar_pattern_ops);

INSERT INTO area_type (name, description) VALUES
    ('WORLD',               'Root of the geographic hierarchy'),
    ('COUNTRY',             'Sovereign state, e.g. Kenya'),
    ('COUNTY',              'Devolved county government unit'),
    ('CONSTITUENCY',        'Parliamentary constituency within a county'),
    ('WARD',                'County assembly ward within a constituency'),
    ('REGISTRATION_CENTER', 'Voter registration centre within a ward'),
    ('POLLING_STATION',     'Polling station within a registration centre');
