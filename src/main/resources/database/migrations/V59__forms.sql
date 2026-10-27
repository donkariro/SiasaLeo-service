-- V59: forms — reusable, versioned dynamic forms (docs/forms.md).
-- Relational columns hold ownership, versions, actors and workflow; JSONB holds
-- the form tree (source/resolved) and the answers, whose shape varies per form.
-- The schema keeps the module boundary visible: other domains may reference
-- forms.*, but nothing here references a consuming domain's tables.

CREATE SCHEMA forms;

-- An owner of forms and answers. external_ref is opaque to forms: the consuming
-- domain maps its own records (e.g. a political party) onto a tenant.
CREATE TABLE forms.tenant (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 tenant_type VARCHAR(40) NOT NULL CHECK (tenant_type ~ '^[A-Z][A-Z0-9_]*$'),
 external_ref VARCHAR(100) NOT NULL CHECK (btrim(external_ref)<>''),
 UNIQUE (tenant_type, external_ref)
);

-- A platform core (tenant_id NULL) or a tenant's extension of a core.
CREATE TABLE forms.form_definition (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 code VARCHAR(100) NOT NULL CHECK (code ~ '^[a-z][a-z0-9-]*$'),
 purpose_key VARCHAR(100) NOT NULL CHECK (purpose_key ~ '^[a-z][a-z0-9_.-]*$'),
 tenant_id BIGINT REFERENCES forms.tenant(id),
 title VARCHAR(255) NOT NULL CHECK (btrim(title)<>''),
 created_by_account_id BIGINT REFERENCES user_account(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX form_definition_platform_code ON forms.form_definition(code) WHERE tenant_id IS NULL;
CREATE UNIQUE INDEX form_definition_tenant_code ON forms.form_definition(tenant_id,code) WHERE tenant_id IS NOT NULL;

-- source is what an administrator authored; resolved is the compiled, complete
-- definition that renderers display and validators enforce.
CREATE TABLE forms.form_version (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 definition_id BIGINT NOT NULL REFERENCES forms.form_definition(id),
 version_number INTEGER NOT NULL CHECK (version_number>0),
 base_version_id BIGINT REFERENCES forms.form_version(id),
 status VARCHAR(10) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED','RETIRED')),
 source JSONB NOT NULL CHECK (jsonb_typeof(source)='object'),
 resolved JSONB CHECK (resolved IS NULL OR jsonb_typeof(resolved)='object'),
 checksum CHAR(64),
 created_by_account_id BIGINT REFERENCES user_account(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 published_by_account_id BIGINT REFERENCES user_account(id),
 published_at TIMESTAMPTZ,
 row_version BIGINT NOT NULL DEFAULT 0,
 UNIQUE (definition_id, version_number),
 CHECK (status='DRAFT' OR (resolved IS NOT NULL AND checksum IS NOT NULL AND published_at IS NOT NULL))
);
CREATE INDEX form_version_base ON forms.form_version(base_version_id);

CREATE FUNCTION forms.guard_form_version() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE definition forms.form_definition; base forms.form_version; base_definition forms.form_definition;
BEGIN
 IF TG_OP='DELETE' THEN
  IF OLD.status<>'DRAFT' THEN RAISE EXCEPTION 'Published form versions cannot be deleted' USING ERRCODE='23514'; END IF;
  RETURN OLD;
 END IF;
 IF TG_OP='UPDATE' AND OLD.status<>'DRAFT' THEN
  IF NOT (OLD.status='PUBLISHED' AND NEW.status='RETIRED')
     OR (to_jsonb(NEW)-'status'-'row_version') IS DISTINCT FROM (to_jsonb(OLD)-'status'-'row_version') THEN
   RAISE EXCEPTION 'Published form versions are immutable; create another version' USING ERRCODE='23514';
  END IF;
  RETURN NEW;
 END IF;
 IF TG_OP='UPDATE' AND (NEW.definition_id<>OLD.definition_id OR NEW.version_number<>OLD.version_number
     OR NEW.base_version_id IS DISTINCT FROM OLD.base_version_id) THEN
  RAISE EXCEPTION 'A form version keeps its definition, number and base' USING ERRCODE='23514';
 END IF;
 IF NEW.status='RETIRED' THEN RAISE EXCEPTION 'Only published versions can be retired' USING ERRCODE='23514'; END IF;
 SELECT * INTO definition FROM forms.form_definition WHERE id=NEW.definition_id;
 -- One level of extension: platform cores stand alone, tenant forms extend a core.
 IF (definition.tenant_id IS NULL) <> (NEW.base_version_id IS NULL) THEN
  RAISE EXCEPTION 'Tenant forms extend a platform core; platform cores have no base' USING ERRCODE='23514';
 END IF;
 IF NEW.base_version_id IS NOT NULL THEN
  SELECT * INTO base FROM forms.form_version WHERE id=NEW.base_version_id FOR SHARE;
  SELECT * INTO base_definition FROM forms.form_definition WHERE id=base.definition_id;
  IF base_definition.tenant_id IS NOT NULL OR base_definition.purpose_key<>definition.purpose_key THEN
   RAISE EXCEPTION 'An extension needs a platform core with the same purpose' USING ERRCODE='23514';
  END IF;
  IF NEW.status='PUBLISHED' AND base.status<>'PUBLISHED' THEN
   RAISE EXCEPTION 'Publish against a published core version' USING ERRCODE='23514';
  END IF;
 END IF;
 IF NEW.status='PUBLISHED' THEN
  -- jsonb text is canonical, so the checksum can be recomputed from the row.
  NEW.checksum:=encode(sha256(convert_to(NEW.resolved::text,'UTF8')),'hex');
 ELSE
  NEW.checksum:=NULL;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER form_version_guard BEFORE INSERT OR UPDATE OR DELETE ON forms.form_version
 FOR EACH ROW EXECUTE FUNCTION forms.guard_form_version();

-- One use of a form: the working draft, pinned to a published version.
CREATE TABLE forms.form_instance (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 form_version_id BIGINT NOT NULL REFERENCES forms.form_version(id),
 tenant_id BIGINT REFERENCES forms.tenant(id),
 answers JSONB NOT NULL DEFAULT '{}' CHECK (jsonb_typeof(answers)='object'),
 created_by_account_id BIGINT REFERENCES user_account(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 row_version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX form_instance_version ON forms.form_instance(form_version_id);

CREATE FUNCTION forms.guard_form_instance() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='UPDATE' AND (NEW.form_version_id<>OLD.form_version_id OR NEW.tenant_id IS DISTINCT FROM OLD.tenant_id) THEN
  RAISE EXCEPTION 'A form instance stays on its pinned version and tenant' USING ERRCODE='23514';
 END IF;
 IF TG_OP='INSERT' AND NOT EXISTS(SELECT 1 FROM forms.form_version WHERE id=NEW.form_version_id AND status='PUBLISHED') THEN
  RAISE EXCEPTION 'Form instances use a published version' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER form_instance_guard BEFORE INSERT OR UPDATE ON forms.form_instance
 FOR EACH ROW EXECUTE FUNCTION forms.guard_form_instance();

-- Immutable snapshots of what was submitted. Corrections add revisions.
-- instance_row_version makes a repeated submit request idempotent.
CREATE TABLE forms.form_submission (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 instance_id BIGINT NOT NULL REFERENCES forms.form_instance(id),
 revision INTEGER NOT NULL CHECK (revision>0),
 form_version_id BIGINT NOT NULL REFERENCES forms.form_version(id),
 definition_checksum CHAR(64) NOT NULL,
 answers JSONB NOT NULL CHECK (jsonb_typeof(answers)='object'),
 context JSONB NOT NULL DEFAULT '{}' CHECK (jsonb_typeof(context)='object'),
 instance_row_version BIGINT NOT NULL,
 submitted_by_account_id BIGINT NOT NULL REFERENCES user_account(id),
 submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE (instance_id, revision),
 UNIQUE (instance_id, instance_row_version)
);
CREATE INDEX form_submission_answers ON forms.form_submission USING GIN (answers jsonb_path_ops);

CREATE FUNCTION forms.guard_form_submission() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE version forms.form_version;
BEGIN
 IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'Form submissions are immutable' USING ERRCODE='23514'; END IF;
 SELECT v.* INTO version FROM forms.form_version v JOIN forms.form_instance i ON i.form_version_id=v.id WHERE i.id=NEW.instance_id;
 IF version.id<>NEW.form_version_id OR version.checksum IS DISTINCT FROM NEW.definition_checksum THEN
  RAISE EXCEPTION 'A submission records its instance''s pinned version and checksum' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER form_submission_guard BEFORE INSERT OR UPDATE OR DELETE ON forms.form_submission
 FOR EACH ROW EXECUTE FUNCTION forms.guard_form_submission();

-- The exact sealed document version submitted for a document field.
CREATE TABLE forms.submission_evidence (
 submission_id BIGINT NOT NULL REFERENCES forms.form_submission(id),
 field_key VARCHAR(201) NOT NULL,
 asset_version_id BIGINT NOT NULL REFERENCES digital_asset_version(id),
 PRIMARY KEY (submission_id, field_key, asset_version_id)
);
CREATE INDEX submission_evidence_asset_version ON forms.submission_evidence(asset_version_id);

-- The exact declaration wording an applicant accepted.
CREATE TABLE forms.declaration_acceptance (
 submission_id BIGINT NOT NULL REFERENCES forms.form_submission(id),
 declaration_key VARCHAR(201) NOT NULL,
 declaration_text TEXT NOT NULL,
 PRIMARY KEY (submission_id, declaration_key)
);

CREATE FUNCTION forms.guard_submission_detail() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'Submitted evidence and declarations are immutable' USING ERRCODE='23514'; END IF;
 -- Nested, not AND-ed: PL/pgSQL does not short-circuit, and NEW has no
 -- asset_version_id on declaration_acceptance.
 IF TG_TABLE_NAME='submission_evidence' THEN
  IF NOT EXISTS(SELECT 1 FROM digital_asset_version WHERE id=NEW.asset_version_id AND sealed_at IS NOT NULL) THEN
   RAISE EXCEPTION 'Submitted documents need a sealed asset version' USING ERRCODE='23514';
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER submission_evidence_guard BEFORE INSERT OR UPDATE OR DELETE ON forms.submission_evidence
 FOR EACH ROW EXECUTE FUNCTION forms.guard_submission_detail();
CREATE TRIGGER declaration_acceptance_guard BEFORE INSERT OR UPDATE OR DELETE ON forms.declaration_acceptance
 FOR EACH ROW EXECUTE FUNCTION forms.guard_submission_detail();
