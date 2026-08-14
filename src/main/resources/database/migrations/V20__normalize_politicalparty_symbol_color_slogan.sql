-- V20: normalize the symbol, colors and slogan attributes of political_party
-- into their own tables. Symbols and slogans are temporal (upto_date IS NULL
-- means currently in use); colors are a plain multi-valued attribute kept in
-- register order. The seeded ORPP data is carried across (from_date taken as
-- registered_on) and the flat columns are then dropped from political_party.

CREATE TABLE politicalparty_symbol (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    politicalparty_id  BIGINT NOT NULL REFERENCES political_party (id) ON DELETE CASCADE,
    symbol_description VARCHAR(255) NOT NULL,
    image_file         VARCHAR(255),
    from_date          DATE,
    upto_date          DATE,
    CHECK (upto_date IS NULL OR from_date IS NULL OR upto_date >= from_date)
);

CREATE TABLE politicalparty_color (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    politicalparty_id BIGINT NOT NULL REFERENCES political_party (id) ON DELETE CASCADE,
    color_name        VARCHAR(100) NOT NULL,
    display_order     INT NOT NULL
);

CREATE TABLE politicalparty_slogan (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    politicalparty_id BIGINT NOT NULL REFERENCES political_party (id) ON DELETE CASCADE,
    slogan            VARCHAR(255) NOT NULL,
    from_date         DATE,
    upto_date         DATE,
    CHECK (upto_date IS NULL OR from_date IS NULL OR upto_date >= from_date)
);

CREATE INDEX idx_politicalparty_symbol_party ON politicalparty_symbol (politicalparty_id);
CREATE INDEX idx_politicalparty_color_party ON politicalparty_color (politicalparty_id);
CREATE INDEX idx_politicalparty_slogan_party ON politicalparty_slogan (politicalparty_id);

-- A party presents exactly one symbol at a time on the ballot.
CREATE UNIQUE INDEX idx_politicalparty_symbol_current ON politicalparty_symbol (politicalparty_id)
    WHERE upto_date IS NULL;

-- Carry the seeded symbol across (every seeded party has one).
INSERT INTO politicalparty_symbol (politicalparty_id, symbol_description, image_file, from_date)
SELECT id, symbol, symbol_image_file, registered_on
FROM political_party
WHERE symbol IS NOT NULL;

-- Split the free-text colors column ('Red, Black and Green') into one row per
-- color, preserving the order in which the ORPP register lists them. Tokens
-- like 'Colourless' come through as a single "color" row; clean up by hand
-- later if needed.
INSERT INTO politicalparty_color (politicalparty_id, color_name, display_order)
SELECT pp.id, btrim(t.color), t.ord
FROM political_party pp
         CROSS JOIN LATERAL regexp_split_to_table(pp.colors, '\s*,\s*|\s+and\s+')
    WITH ORDINALITY AS t(color, ord)
WHERE pp.colors IS NOT NULL
  AND btrim(t.color) <> '';

INSERT INTO politicalparty_slogan (politicalparty_id, slogan, from_date)
SELECT id, slogan, registered_on
FROM political_party
WHERE slogan IS NOT NULL;

ALTER TABLE political_party
    DROP COLUMN symbol,
    DROP COLUMN symbol_image_file,
    DROP COLUMN colors,
    DROP COLUMN slogan;
