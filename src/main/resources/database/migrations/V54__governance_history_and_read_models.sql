CREATE TABLE officeholder_evidence (
    officeholder_id BIGINT NOT NULL REFERENCES officeholder(id),
    asset_version_id BIGINT NOT NULL REFERENCES digital_asset_version(id),
    locator VARCHAR(255),
    recorded_by_account_id BIGINT NOT NULL REFERENCES user_account(id),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(officeholder_id,asset_version_id)
);
CREATE TRIGGER sealed_source BEFORE INSERT OR UPDATE ON officeholder_evidence FOR EACH ROW EXECUTE FUNCTION require_sealed_asset();
CREATE TRIGGER immutable_tenure_evidence BEFORE UPDATE OR DELETE ON officeholder_evidence FOR EACH ROW EXECUTE FUNCTION governance_immutable();
CREATE TRIGGER immutable_asset_identity BEFORE UPDATE OR DELETE ON digital_asset FOR EACH ROW EXECUTE FUNCTION governance_immutable();

-- Never allow scope definitions to reference editable draft geography.
CREATE FUNCTION governance_published_scope() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.kind='GEOGRAPHIC' AND NOT EXISTS(SELECT 1 FROM electoral_geography_snapshot WHERE id=NEW.snapshot_id AND status='PUBLISHED') THEN
   RAISE EXCEPTION 'Measurement scope requires published geography' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER published_scope BEFORE INSERT ON measurement_scope FOR EACH ROW EXECUTE FUNCTION governance_published_scope();

CREATE FUNCTION governance_published_link() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_TABLE_NAME='publication_event' THEN
   IF NOT EXISTS(SELECT 1 FROM manifesto_version WHERE id=NEW.manifesto_version_id AND publication_status='PUBLISHED') THEN
     RAISE EXCEPTION 'Announcements must identify a published manifesto version' USING ERRCODE='23514'; END IF;
 ELSE
   IF NOT EXISTS(SELECT 1 FROM commitment_revision WHERE id=NEW.commitment_revision_id AND sealed_at IS NOT NULL) THEN
     RAISE EXCEPTION 'Delivery links must identify a published commitment revision' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER published_announcement BEFORE INSERT ON publication_event FOR EACH ROW EXECUTE FUNCTION governance_published_link();
CREATE TRIGGER published_delivery BEFORE INSERT ON commitment_delivery_link FOR EACH ROW EXECUTE FUNCTION governance_published_link();

-- New explicit comparisons require known, matching semantics. Legacy links
-- installed by V51 retain their unknown scopes for review, not scoring.
CREATE FUNCTION compatible_target_observation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM performance_target t JOIN performance_observation o ON o.id=NEW.observation_id
   WHERE t.id=NEW.target_id AND t.indicator_id=o.indicator_id AND t.scope_id=o.scope_id
   AND t.officeholder_id=o.officeholder_id AND o.date_precision='DAY'
   AND o.period_start>=t.period_start AND o.period_end<=t.period_end
   AND o.publication_status='PUBLISHED') THEN
   RAISE EXCEPTION 'Comparison requires matching subject, indicator, scope and period' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER target_observation_compatibility BEFORE INSERT OR UPDATE ON target_observation FOR EACH ROW EXECUTE FUNCTION compatible_target_observation();

CREATE INDEX idx_manifesto_issuer ON manifesto(issuer_person_id);
CREATE INDEX idx_manifesto_candidacy ON manifesto(candidacy_id);
CREATE INDEX idx_commitment_root ON commitment(manifesto_id);
CREATE INDEX idx_target_revision ON commitment_target(commitment_revision_id);
CREATE INDEX idx_delivery_work ON commitment_delivery_link(work_effort_id);
CREATE INDEX idx_digital_asset_author ON digital_asset_version(author_party_id);
CREATE INDEX idx_claim_work ON performance_record(work_effort_id);

-- Office names are already country-scoped in V50; levels can now describe
-- other countries' structures without assigning them Kenyan county terminology.
ALTER TABLE office DROP CONSTRAINT office_level_check;
ALTER TABLE office ALTER COLUMN level TYPE VARCHAR(100);
ALTER TABLE office ADD CHECK(btrim(level) <> '');

-- Stable identity and original recorder cannot be reassigned between drafts.
CREATE FUNCTION governance_fixed_identity() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE field TEXT;
BEGIN
 FOREACH field IN ARRAY TG_ARGV LOOP
   IF to_jsonb(NEW)->field IS DISTINCT FROM to_jsonb(OLD)->field THEN
     RAISE EXCEPTION 'Cannot change % of an existing revision',field USING ERRCODE='23514'; END IF;
 END LOOP;
 RETURN NEW;
END $$;
CREATE TRIGGER fixed_identity BEFORE UPDATE ON manifesto_version FOR EACH ROW
 EXECUTE FUNCTION governance_fixed_identity('manifesto_id','version_number','recorded_by_account_id','created_at');
CREATE TRIGGER fixed_identity BEFORE UPDATE ON commitment_revision FOR EACH ROW
 EXECUTE FUNCTION governance_fixed_identity('commitment_id','revision_number','recorded_by_account_id','created_at');
CREATE TRIGGER fixed_identity BEFORE UPDATE ON digital_asset_version FOR EACH ROW
 EXECUTE FUNCTION governance_fixed_identity('asset_id','version_number','recorded_by_account_id','recorded_at');
CREATE TRIGGER fixed_identity BEFORE UPDATE ON performance_record FOR EACH ROW
 EXECUTE FUNCTION governance_fixed_identity('claim_id','officeholder_id','revision_number','created_at');
CREATE TRIGGER fixed_identity BEFORE UPDATE ON performance_assessment FOR EACH ROW
 EXECUTE FUNCTION governance_fixed_identity('reviewed_by_account_id','assessed_at');
