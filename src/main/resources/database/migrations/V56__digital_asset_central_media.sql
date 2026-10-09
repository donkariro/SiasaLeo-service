-- Digital assets become the central media store for every domain, not only
-- governance evidence. Visibility is separate from sealing, provenance is
-- required only where an asset is cited as evidence, and consumers may follow
-- an asset's latest sealed version instead of pinning one.

-- Fixed at creation: digital_asset rows are immutable (V54).
ALTER TABLE digital_asset ADD COLUMN visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC'
    CHECK (visibility IN ('PUBLIC','PRIVATE'));

-- The existing CHECK (btrim(provenance) <> '') still rejects blank values.
ALTER TABLE digital_asset_version ALTER COLUMN provenance DROP NOT NULL;

-- Evidence and public records must cite a sealed, public version with
-- provenance. person_digital_asset is a general media link (e.g. a portrait or
-- a private identity document), so it only requires a sealed version.
CREATE OR REPLACE FUNCTION require_sealed_asset() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE sealed TIMESTAMPTZ; provenance_text TEXT; asset_visibility TEXT;
BEGIN
 IF NEW.asset_version_id IS NULL THEN RETURN NEW; END IF;
 SELECT v.sealed_at,v.provenance,a.visibility INTO sealed,provenance_text,asset_visibility
   FROM digital_asset_version v JOIN digital_asset a ON a.id=v.asset_id WHERE v.id=NEW.asset_version_id;
 IF sealed IS NULL THEN
   RAISE EXCEPTION 'Evidence must reference a sealed asset version' USING ERRCODE='23514';
 END IF;
 IF TG_TABLE_NAME <> 'person_digital_asset' AND (asset_visibility <> 'PUBLIC' OR provenance_text IS NULL) THEN
   RAISE EXCEPTION 'Evidence must reference a public asset version with provenance' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;

-- Latest sealed version of an asset, for consumers that follow the asset
-- rather than pin a version (logos, portraits). NULL until one is sealed.
CREATE FUNCTION current_asset_version_id(asset BIGINT) RETURNS BIGINT LANGUAGE sql STABLE AS $$
 SELECT id FROM digital_asset_version WHERE asset_id=asset AND sealed_at IS NOT NULL
 ORDER BY version_number DESC LIMIT 1
$$;
