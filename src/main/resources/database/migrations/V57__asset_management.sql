-- Preserve V56 visibility for existing records; new assets are private.
DROP TRIGGER immutable_asset_identity ON digital_asset;
ALTER TABLE digital_asset ALTER COLUMN visibility SET DEFAULT 'PRIVATE';
ALTER TABLE digital_asset ADD COLUMN owner_account_id BIGINT REFERENCES user_account(id);
ALTER TABLE digital_asset ADD COLUMN owner_party_id BIGINT REFERENCES party(id);
ALTER TABLE digital_asset ADD COLUMN archived_at TIMESTAMPTZ;
ALTER TABLE digital_asset ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE digital_asset ADD CHECK (owner_account_id IS NULL OR owner_party_id IS NULL);
UPDATE digital_asset SET owner_account_id=created_by_account_id;

-- Null legacy owners remain unassigned. An administrator can explicitly claim
-- them for an account or party; no arbitrary account is invented by migration.
CREATE TABLE party_media_manager (
 party_id BIGINT NOT NULL REFERENCES party(id),
 account_id BIGINT NOT NULL REFERENCES user_account(id),
 granted_by_account_id BIGINT NOT NULL REFERENCES user_account(id),
 PRIMARY KEY(party_id,account_id)
);
CREATE TABLE digital_asset_grant (
 asset_id BIGINT NOT NULL REFERENCES digital_asset(id) ON DELETE CASCADE,
 account_id BIGINT NOT NULL REFERENCES user_account(id),
 permission VARCHAR(10) NOT NULL CHECK(permission IN ('READ','MANAGE')),
 PRIMARY KEY(asset_id,account_id)
);
CREATE TABLE digital_asset_audit (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 asset_id BIGINT,
 actor_account_id BIGINT REFERENCES user_account(id),
 action TEXT NOT NULL,
 before_data JSONB,
 after_data JSONB,
 recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_asset_owner_account ON digital_asset(owner_account_id);
CREATE INDEX idx_asset_owner_party ON digital_asset(owner_party_id);
CREATE INDEX idx_asset_grant_account ON digital_asset_grant(account_id,asset_id);
CREATE INDEX idx_asset_audit_asset ON digital_asset_audit(asset_id,id);

CREATE FUNCTION audit_digital_asset() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE old_data JSONB; new_data JSONB; target BIGINT;
BEGIN
 IF TG_OP <> 'INSERT' THEN old_data:=to_jsonb(OLD); END IF;
 IF TG_OP <> 'DELETE' THEN new_data:=to_jsonb(NEW); END IF;
 IF TG_TABLE_NAME='digital_asset' THEN target:=coalesce((new_data->>'id')::bigint,(old_data->>'id')::bigint);
 ELSE target:=coalesce((new_data->>'asset_id')::bigint,(old_data->>'asset_id')::bigint); END IF;
 INSERT INTO digital_asset_audit(asset_id,actor_account_id,action,before_data,after_data)
 VALUES(target,nullif(current_setting('siasaleo.actor',true),'')::bigint,
        TG_TABLE_NAME || ':' || TG_OP,old_data,new_data);
 RETURN NULL;
END $$;
CREATE TRIGGER asset_audit AFTER INSERT OR UPDATE OR DELETE ON digital_asset FOR EACH ROW EXECUTE FUNCTION audit_digital_asset();
CREATE TRIGGER asset_grant_audit AFTER INSERT OR UPDATE OR DELETE ON digital_asset_grant FOR EACH ROW EXECUTE FUNCTION audit_digital_asset();
CREATE TRIGGER party_media_manager_audit AFTER INSERT OR UPDATE OR DELETE ON party_media_manager FOR EACH ROW EXECUTE FUNCTION audit_digital_asset();
CREATE TRIGGER immutable_asset_audit BEFORE UPDATE OR DELETE ON digital_asset_audit FOR EACH ROW EXECUTE FUNCTION governance_immutable();

-- Freeze descriptive labels with each sealed version, independently of the
-- editable library title. Backfill only these new columns on historical rows.
ALTER TABLE digital_asset_version ADD COLUMN snapshot_title VARCHAR(255);
ALTER TABLE digital_asset_version ADD COLUMN snapshot_description TEXT;
ALTER TABLE digital_asset_version ADD COLUMN snapshot_asset_type VARCHAR(40);
ALTER TABLE digital_asset_version DISABLE TRIGGER sealed_digital_asset_version;
UPDATE digital_asset_version v SET snapshot_title=a.title,
 snapshot_description=a.description,snapshot_asset_type=a.asset_type
 FROM digital_asset a WHERE a.id=v.asset_id AND v.sealed_at IS NOT NULL;
ALTER TABLE digital_asset_version ENABLE TRIGGER sealed_digital_asset_version;

CREATE FUNCTION asset_has_evidence(asset BIGINT) RETURNS BOOLEAN LANGUAGE plpgsql AS $$
DECLARE relation TEXT; found BOOLEAN;
BEGIN
 FOREACH relation IN ARRAY ARRAY['claim_evidence','observation_evidence','assessment_evidence',
 'work_effort_asset','work_effort_participation','manifesto_asset','commitment_asset',
 'publication_event','officeholder_evidence'] LOOP
  EXECUTE format('SELECT EXISTS(SELECT 1 FROM %I e JOIN digital_asset_version v ON v.id=e.asset_version_id WHERE v.asset_id=$1)',relation)
   INTO found USING asset;
  IF found THEN RETURN TRUE; END IF;
 END LOOP;
 RETURN FALSE;
END $$;

CREATE FUNCTION guard_asset_management() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN
  IF EXISTS(SELECT 1 FROM digital_asset_version WHERE asset_id=OLD.id) THEN
   RAISE EXCEPTION 'Remove eligible draft versions before deleting an asset' USING ERRCODE='23514';
  END IF;
  RETURN OLD;
 END IF;
 IF (NEW.id,NEW.created_by_account_id,NEW.created_at) IS DISTINCT FROM (OLD.id,OLD.created_by_account_id,OLD.created_at) THEN
  RAISE EXCEPTION 'Asset identity and creator are immutable' USING ERRCODE='23514';
 END IF;
 IF OLD.visibility='PUBLIC' AND NEW.visibility<>'PUBLIC' AND asset_has_evidence(OLD.id) THEN
  RAISE EXCEPTION 'Public evidence cannot be made private while referenced' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER asset_management BEFORE UPDATE OR DELETE ON digital_asset FOR EACH ROW EXECUTE FUNCTION guard_asset_management();

CREATE FUNCTION guard_asset_version_management() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE asset digital_asset;
BEGIN
 SELECT * INTO asset FROM digital_asset WHERE id=NEW.asset_id FOR UPDATE;
 IF asset.archived_at IS NOT NULL THEN
  RAISE EXCEPTION 'Archived assets cannot receive new content' USING ERRCODE='23514';
 END IF;
 IF NEW.sealed_at IS NOT NULL AND OLD.sealed_at IS NULL THEN
  NEW.snapshot_title:=asset.title;
  NEW.snapshot_description:=asset.description;
  NEW.snapshot_asset_type:=asset.asset_type;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER asset_version_management BEFORE INSERT OR UPDATE ON digital_asset_version FOR EACH ROW EXECUTE FUNCTION guard_asset_version_management();

-- Evidence rules remain on consuming association tables. Lock the asset so a
-- concurrent visibility change cannot hide newly attached public evidence.
CREATE OR REPLACE FUNCTION require_sealed_asset() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE version digital_asset_version; asset digital_asset;
BEGIN
 IF NEW.asset_version_id IS NULL THEN RETURN NEW; END IF;
 SELECT * INTO version FROM digital_asset_version WHERE id=NEW.asset_version_id;
 IF version.sealed_at IS NULL THEN RAISE EXCEPTION 'Link a sealed asset version' USING ERRCODE='23514'; END IF;
 SELECT * INTO asset FROM digital_asset WHERE id=version.asset_id FOR UPDATE;
 IF asset.archived_at IS NOT NULL THEN RAISE EXCEPTION 'Cannot attach an archived asset' USING ERRCODE='23514'; END IF;
 IF TG_TABLE_NAME<>'person_digital_asset' AND (asset.visibility<>'PUBLIC' OR version.provenance IS NULL) THEN
  RAISE EXCEPTION 'Evidence requires public source material with provenance' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;

INSERT INTO digital_asset_type(code,name) VALUES ('IMAGE','Image'),('AUDIO','Audio'),('VIDEO','Video'),
 ('DOCUMENT','Document'),('LOGO','Logo'),('PORTRAIT','Portrait'),
 ('IDENTITY_DOCUMENT','Identity Document'),('CERTIFICATE','Certificate');

-- Consumer-owned selection: a party can be a person or organization. A selected
-- asset follows its current sealed version; pinned person attachments stay intact.
CREATE TABLE party_media_selection (
 party_id BIGINT NOT NULL REFERENCES party(id),
 purpose VARCHAR(100) NOT NULL CHECK(btrim(purpose)<>''),
 asset_id BIGINT NOT NULL REFERENCES digital_asset(id),
 selected_by_account_id BIGINT REFERENCES user_account(id),
 selected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(party_id,purpose)
);
CREATE TABLE party_media_selection_history (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 party_id BIGINT NOT NULL REFERENCES party(id),
 purpose VARCHAR(100) NOT NULL,
 asset_id BIGINT NOT NULL REFERENCES digital_asset(id),
 selected_by_account_id BIGINT REFERENCES user_account(id),
 selected_at TIMESTAMPTZ NOT NULL,
 replaced_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE FUNCTION guard_party_media() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE asset digital_asset;
BEGIN
 IF TG_OP<>'DELETE' THEN
  SELECT * INTO asset FROM digital_asset WHERE id=NEW.asset_id FOR UPDATE;
  IF asset.archived_at IS NOT NULL OR current_asset_version_id(NEW.asset_id) IS NULL THEN
   RAISE EXCEPTION 'Select an active asset with a sealed version' USING ERRCODE='23514';
  END IF;
 END IF;
 IF TG_OP<>'INSERT' THEN
  INSERT INTO party_media_selection_history(party_id,purpose,asset_id,selected_by_account_id,selected_at)
  VALUES(OLD.party_id,OLD.purpose,OLD.asset_id,OLD.selected_by_account_id,OLD.selected_at);
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER party_media_guard BEFORE INSERT OR UPDATE OR DELETE ON party_media_selection FOR EACH ROW EXECUTE FUNCTION guard_party_media();
CREATE TRIGGER immutable_party_media_history BEFORE UPDATE OR DELETE ON party_media_selection_history FOR EACH ROW EXECUTE FUNCTION governance_immutable();
