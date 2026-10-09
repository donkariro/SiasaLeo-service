-- Keep legacy strings for traceability; new writes select asset references.
ALTER TABLE politicalparty_symbol ADD COLUMN image_asset_id BIGINT REFERENCES digital_asset(id);
ALTER TABLE politicalparty_official ADD COLUMN photo_asset_id BIGINT REFERENCES digital_asset(id);
CREATE TABLE legacy_media_reconciliation (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 asset_id BIGINT NOT NULL UNIQUE REFERENCES digital_asset(id),
 legacy_reference TEXT NOT NULL,
 source_table TEXT NOT NULL,
 source_record_id BIGINT NOT NULL,
 status VARCHAR(30) NOT NULL CHECK(status IN ('EXTERNAL_REFERENCE','NEEDS_FILE_IMPORT')),
 UNIQUE(source_table,source_record_id)
);
DO $$
DECLARE item RECORD; asset BIGINT; version BIGINT; source TEXT;
BEGIN
 FOR item IN
  SELECT 'politicalparty_symbol' AS origin,id,politicalparty_id AS party,image_file AS reference,'LOGO' AS kind
   FROM politicalparty_symbol WHERE image_file IS NOT NULL AND btrim(image_file)<>''
  UNION ALL
  SELECT 'politicalparty_official',id,politicalparty_id,photo,'PORTRAIT'
   FROM politicalparty_official WHERE photo IS NOT NULL AND btrim(photo)<>''
 LOOP
  source:=CASE WHEN btrim(item.reference) ~ '^https?://[^[:space:]]+$' THEN btrim(item.reference) ELSE NULL END;
  INSERT INTO digital_asset(title,asset_type,visibility,owner_party_id)
   VALUES(CASE WHEN item.kind='LOGO' THEN 'Party symbol' ELSE 'Party official portrait' END,
    item.kind,'PUBLIC',item.party) RETURNING id INTO asset;
  INSERT INTO digital_asset_version(asset_id,version_number,source_url,provenance)
   VALUES(asset,1,source,'Legacy reference from ' || item.origin || ': ' || item.reference) RETURNING id INTO version;
  IF source IS NOT NULL THEN UPDATE digital_asset_version SET sealed_at=CURRENT_TIMESTAMP WHERE id=version; END IF;
  INSERT INTO legacy_media_reconciliation(asset_id,legacy_reference,source_table,source_record_id,status)
   VALUES(asset,item.reference,item.origin,item.id,CASE WHEN source IS NULL THEN 'NEEDS_FILE_IMPORT' ELSE 'EXTERNAL_REFERENCE' END);
  IF item.origin='politicalparty_symbol' THEN UPDATE politicalparty_symbol SET image_asset_id=asset WHERE id=item.id;
  ELSE UPDATE politicalparty_official SET photo_asset_id=asset WHERE id=item.id; END IF;
 END LOOP;
END $$;

CREATE FUNCTION guard_legacy_media_reference() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE old_data JSONB; new_data JSONB; asset_id BIGINT; asset digital_asset;
BEGIN
 new_data:=to_jsonb(NEW);
 IF TG_OP='UPDATE' THEN old_data:=to_jsonb(OLD); END IF;
 IF (new_data->>TG_ARGV[0]) IS DISTINCT FROM (old_data->>TG_ARGV[0]) THEN
  RAISE EXCEPTION 'Legacy filenames are read only; select a digital asset' USING ERRCODE='23514';
 END IF;
 IF (new_data->>TG_ARGV[1]) IS DISTINCT FROM (old_data->>TG_ARGV[1]) AND new_data->>TG_ARGV[1] IS NOT NULL THEN
  asset_id:=(new_data->>TG_ARGV[1])::bigint;
  SELECT * INTO asset FROM digital_asset WHERE id=asset_id FOR UPDATE;
  IF asset.visibility<>'PUBLIC' OR asset.archived_at IS NOT NULL OR current_asset_version_id(asset_id) IS NULL THEN
   RAISE EXCEPTION 'Public media needs an active public asset with a sealed version' USING ERRCODE='23514';
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER symbol_media BEFORE INSERT OR UPDATE ON politicalparty_symbol FOR EACH ROW
 EXECUTE FUNCTION guard_legacy_media_reference('image_file','image_asset_id');
CREATE TRIGGER official_media BEFORE INSERT OR UPDATE ON politicalparty_official FOR EACH ROW
 EXECUTE FUNCTION guard_legacy_media_reference('photo','photo_asset_id');
