-- Shared definitions make measurements comparable across officeholders.
-- Unit and methodology describe the raw values; no aggregate score is implied.
CREATE TABLE performance_indicator (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE CHECK (btrim(code) <> ''),
    name        VARCHAR(255) NOT NULL CHECK (btrim(name) <> ''),
    description TEXT,
    unit        VARCHAR(100) NOT NULL CHECK (btrim(unit) <> ''),
    methodology TEXT NOT NULL CHECK (btrim(methodology) <> ''),
    direction   VARCHAR(20) NOT NULL CHECK (direction IN
                    ('HIGHER_IS_BETTER', 'LOWER_IS_BETTER', 'TARGET_VALUE')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
