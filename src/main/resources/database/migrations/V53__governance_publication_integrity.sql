ALTER TABLE officeholder ADD UNIQUE(id,person_id);
ALTER TABLE work_effort_participation ADD FOREIGN KEY(officeholder_id,party_id) REFERENCES officeholder(id,person_id);
ALTER TABLE candidacy ADD UNIQUE(id,person_id);
ALTER TABLE manifesto ADD FOREIGN KEY(candidacy_id,issuer_person_id) REFERENCES candidacy(id,person_id);

CREATE FUNCTION governance_immutable() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION '% records are immutable; create a new definition or revision',TG_TABLE_NAME USING ERRCODE='23514';
END $$;
CREATE TRIGGER immutable_indicator BEFORE UPDATE OR DELETE ON performance_indicator FOR EACH ROW EXECUTE FUNCTION governance_immutable();
CREATE TRIGGER immutable_scope BEFORE UPDATE OR DELETE ON measurement_scope FOR EACH ROW EXECUTE FUNCTION governance_immutable();
CREATE TRIGGER immutable_goal BEFORE UPDATE OR DELETE ON governance_goal FOR EACH ROW EXECUTE FUNCTION governance_immutable();
CREATE TRIGGER immutable_manifesto BEFORE UPDATE OR DELETE ON manifesto FOR EACH ROW EXECUTE FUNCTION governance_immutable();
CREATE TRIGGER immutable_commitment BEFORE UPDATE OR DELETE ON commitment FOR EACH ROW EXECUTE FUNCTION governance_immutable();
CREATE TRIGGER immutable_claim BEFORE UPDATE OR DELETE ON achievement_claim FOR EACH ROW EXECUTE FUNCTION governance_immutable();
CREATE TRIGGER immutable_delivery BEFORE UPDATE OR DELETE ON commitment_delivery_link FOR EACH ROW EXECUTE FUNCTION governance_immutable();
CREATE TRIGGER immutable_participation BEFORE UPDATE OR DELETE ON work_effort_participation FOR EACH ROW EXECUTE FUNCTION governance_immutable();
CREATE TRIGGER immutable_announcement BEFORE UPDATE OR DELETE ON publication_event FOR EACH ROW EXECUTE FUNCTION governance_immutable();

-- Every child writer locks its parent, serializing attachment changes with
-- publication. Check both parents on reparenting so OLD cannot be bypassed.
CREATE FUNCTION guard_governance_child() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE parent_id BIGINT; parent_data JSONB; candidate JSONB;
BEGIN
 FOR candidate IN SELECT CASE WHEN TG_OP='INSERT' THEN NULL ELSE to_jsonb(OLD) END
                  UNION ALL SELECT CASE WHEN TG_OP='DELETE' THEN NULL ELSE to_jsonb(NEW) END LOOP
   IF candidate IS NULL THEN CONTINUE; END IF;
   parent_id := (candidate->>TG_ARGV[1])::bigint;
   EXECUTE format('SELECT to_jsonb(p) FROM %I p WHERE id=$1 FOR UPDATE',TG_ARGV[0]) INTO parent_data USING parent_id;
   IF parent_data->>'publication_status' IN ('PUBLISHED','WITHDRAWN') OR parent_data->>'sealed_at' IS NOT NULL THEN
     RAISE EXCEPTION 'Published content and its associations are immutable' USING ERRCODE='23514';
   END IF;
 END LOOP;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 RETURN NEW;
END $$;
DO $$ DECLARE item RECORD; BEGIN
 FOR item IN SELECT * FROM (VALUES
 ('stored_file','digital_asset_version','asset_version_id'),
 ('claim_evidence','performance_record','record_id'),('claim_policy_area','performance_record','record_id'),
 ('observation_evidence','performance_observation','observation_id'),
 ('assessment_evidence','performance_assessment','assessment_id'),
 ('manifesto_commitment','manifesto_version','manifesto_version_id'),
 ('manifesto_asset','manifesto_version','manifesto_version_id'),
 ('commitment_target','commitment_revision','commitment_revision_id'),
 ('commitment_asset','commitment_revision','commitment_revision_id'),
 ('commitment_goal','commitment_revision','commitment_revision_id'),
 ('commitment_policy_area','commitment_revision','commitment_revision_id')
 ) AS x(child,parent,column_name) LOOP
 EXECUTE format('CREATE TRIGGER guard_parent BEFORE INSERT OR UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION guard_governance_child(%L,%L)',item.child,item.parent,item.column_name);
 END LOOP;
END $$;

CREATE FUNCTION require_sealed_asset() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.asset_version_id IS NOT NULL AND NOT EXISTS
 (SELECT 1 FROM digital_asset_version WHERE id=NEW.asset_version_id AND sealed_at IS NOT NULL) THEN
   RAISE EXCEPTION 'Evidence must reference a sealed asset version' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
DO $$ DECLARE name TEXT; BEGIN
 FOREACH name IN ARRAY ARRAY['claim_evidence','observation_evidence','assessment_evidence','work_effort_asset',
 'work_effort_participation','manifesto_asset','commitment_asset','person_digital_asset','publication_event'] LOOP
 EXECUTE format('CREATE TRIGGER sealed_source BEFORE INSERT OR UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION require_sealed_asset()',name);
 END LOOP;
END $$;

CREATE FUNCTION guard_sealed_governance_version() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='INSERT' AND NEW.sealed_at IS NOT NULL THEN
   RAISE EXCEPTION 'Create a draft before sealing it' USING ERRCODE='23514';
 END IF;
 IF TG_OP <> 'INSERT' AND OLD.sealed_at IS NOT NULL THEN
   RAISE EXCEPTION 'Sealed versions are immutable' USING ERRCODE='23514';
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 IF NEW.sealed_at IS NOT NULL THEN
   IF TG_TABLE_NAME='digital_asset_version' THEN
     IF NEW.source_url IS NULL AND NOT EXISTS (SELECT 1 FROM stored_file WHERE asset_version_id=NEW.id) THEN
       RAISE EXCEPTION 'Asset needs an external source or stored file' USING ERRCODE='23514';
     END IF;
   END IF;
   NEW.sealed_at := CURRENT_TIMESTAMP;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER sealed_digital_asset_version BEFORE INSERT OR UPDATE OR DELETE ON digital_asset_version FOR EACH ROW EXECUTE FUNCTION guard_sealed_governance_version();
CREATE TRIGGER sealed_commitment_revision BEFORE INSERT OR UPDATE OR DELETE ON commitment_revision FOR EACH ROW EXECUTE FUNCTION guard_sealed_governance_version();

CREATE FUNCTION check_manifesto_commitment() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE root_id BIGINT; actual_root BIGINT; stable_id BIGINT;
BEGIN
 SELECT manifesto_id INTO root_id FROM manifesto_version WHERE id=NEW.manifesto_version_id;
 SELECT c.manifesto_id,c.id INTO actual_root,stable_id FROM commitment_revision r JOIN commitment c ON c.id=r.commitment_id WHERE r.id=NEW.commitment_revision_id;
 IF actual_root IS DISTINCT FROM root_id OR EXISTS(
   SELECT 1 FROM manifesto_commitment mc JOIN commitment_revision r ON r.id=mc.commitment_revision_id
   WHERE mc.manifesto_version_id=NEW.manifesto_version_id AND r.commitment_id=stable_id
     AND mc.commitment_revision_id <> NEW.commitment_revision_id) THEN
   RAISE EXCEPTION 'Use one revision of each commitment from this manifesto' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER manifesto_commitment_identity BEFORE INSERT OR UPDATE ON manifesto_commitment FOR EACH ROW EXECUTE FUNCTION check_manifesto_commitment();
CREATE FUNCTION check_commitment_goal() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS (SELECT 1 FROM governance_goal g JOIN commitment c ON c.manifesto_id=g.manifesto_id
 JOIN commitment_revision r ON r.commitment_id=c.id WHERE g.id=NEW.goal_id AND r.id=NEW.commitment_revision_id) THEN
 RAISE EXCEPTION 'Goal and commitment must belong to the same manifesto' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER commitment_goal_identity BEFORE INSERT OR UPDATE ON commitment_goal FOR EACH ROW EXECUTE FUNCTION check_commitment_goal();

CREATE FUNCTION check_assessment_comparison() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.commitment_target_id IS NOT NULL AND NOT EXISTS (
 SELECT 1 FROM commitment_target t JOIN performance_observation o ON o.id=NEW.observation_id
 WHERE t.id=NEW.commitment_target_id AND t.commitment_revision_id=NEW.commitment_revision_id
 AND t.indicator_id=o.indicator_id AND t.scope_id=o.scope_id
 AND o.date_precision='DAY' AND o.period_start>=t.period_start AND o.period_end<=t.period_end
 AND o.publication_status='PUBLISHED') THEN
 RAISE EXCEPTION 'Assessment requires a published observation with compatible indicator, scope and period' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER assessment_comparison BEFORE INSERT OR UPDATE ON performance_assessment FOR EACH ROW EXECUTE FUNCTION check_assessment_comparison();

CREATE FUNCTION guard_governance_publication() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE has_evidence BOOLEAN; actor_id BIGINT; manifest_id BIGINT; revision_id BIGINT;
BEGIN
 IF TG_OP='INSERT' AND NEW.publication_status <> 'DRAFT' THEN
   RAISE EXCEPTION 'Create a draft before publishing' USING ERRCODE='23514';
 END IF;
 IF TG_OP <> 'INSERT' AND OLD.publication_status <> 'DRAFT' THEN
   IF TG_OP='UPDATE' AND OLD.publication_status='PUBLISHED' AND NEW.publication_status='WITHDRAWN'
      AND (to_jsonb(NEW)-'publication_status')=(to_jsonb(OLD)-'publication_status') THEN RETURN NEW; END IF;
   RAISE EXCEPTION 'Published records are immutable; create a new revision' USING ERRCODE='23514';
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 IF NEW.publication_status='WITHDRAWN' THEN RAISE EXCEPTION 'Only published records may be withdrawn' USING ERRCODE='23514'; END IF;
 IF NEW.publication_status='PUBLISHED' THEN
   IF TG_TABLE_NAME='manifesto_version' THEN
     manifest_id:=NEW.manifesto_id;
     IF NOT EXISTS (SELECT 1 FROM manifesto m JOIN user_account a ON a.person_id=m.issuer_person_id
       JOIN candidacy c ON c.id=m.candidacy_id JOIN candidacy_status s ON s.id=c.status
       JOIN contest ct ON ct.id=c.contest_id JOIN election_event e ON e.id=ct.election_event_id
       WHERE m.id=manifest_id AND a.id=NEW.recorded_by_account_id AND a.status='ACTIVE'
       AND e.election_date>CURRENT_DATE AND s.status_name IN
       ('EXPRESSED_INTEREST','PARTY_PRIMARY','PARTY_NOMINEE','INDEPENDENT_PENDING','SUBMITTED_TO_IEBC','CLEARED','ON_BALLOT')) THEN
       RAISE EXCEPTION 'Only the registered issuer with an eligible future candidacy may publish' USING ERRCODE='23514';
     END IF;
     IF NOT EXISTS(SELECT 1 FROM manifesto_commitment WHERE manifesto_version_id=NEW.id) THEN
       RAISE EXCEPTION 'A manifesto needs at least one commitment' USING ERRCODE='23514'; END IF;
     FOR revision_id IN SELECT commitment_revision_id FROM manifesto_commitment WHERE manifesto_version_id=NEW.id ORDER BY commitment_revision_id LOOP
       UPDATE commitment_revision SET sealed_at=CURRENT_TIMESTAMP WHERE id=revision_id AND sealed_at IS NULL;
     END LOOP;
   ELSE
     CASE TG_TABLE_NAME
       WHEN 'performance_record' THEN
         actor_id:=NEW.recorded_by_account_id;
         SELECT EXISTS(SELECT 1 FROM claim_evidence WHERE record_id=NEW.id) INTO has_evidence;
       WHEN 'performance_observation' THEN
         actor_id:=NEW.recorded_by_account_id;
         SELECT EXISTS(SELECT 1 FROM observation_evidence WHERE observation_id=NEW.id) INTO has_evidence;
       WHEN 'performance_assessment' THEN
         actor_id:=NEW.reviewed_by_account_id;
         SELECT EXISTS(SELECT 1 FROM assessment_evidence WHERE assessment_id=NEW.id) INTO has_evidence;
         IF NEW.evidence_status='INSUFFICIENT_EVIDENCE' OR NEW.evidence_status='UNREVIEWED' THEN has_evidence:=TRUE; END IF;
         IF NEW.record_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM performance_record WHERE id=NEW.record_id AND publication_status='PUBLISHED') THEN
           RAISE EXCEPTION 'Assessment subject must be published' USING ERRCODE='23514'; END IF;
         IF NEW.commitment_revision_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM commitment_revision WHERE id=NEW.commitment_revision_id AND sealed_at IS NOT NULL) THEN
           RAISE EXCEPTION 'Assessment promise must be published' USING ERRCODE='23514'; END IF;
     END CASE;
     IF actor_id IS NULL OR NOT has_evidence THEN
       RAISE EXCEPTION 'Publication requires an attributed recorder and versioned evidence' USING ERRCODE='23514'; END IF;
   END IF;
   NEW.published_at:=CURRENT_TIMESTAMP;
 END IF;
 RETURN NEW;
END $$;
DO $$ DECLARE name TEXT; BEGIN
 FOREACH name IN ARRAY ARRAY['performance_record','performance_observation','performance_assessment','manifesto_version'] LOOP
 EXECUTE format('CREATE TRIGGER publication_guard BEFORE INSERT OR UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION guard_governance_publication()',name);
 END LOOP;
END $$;

CREATE VIEW governance_track_record AS
 SELECT r.*,oh.person_id,oh.seat_id,oh.term_id FROM performance_record r
 JOIN officeholder oh ON oh.id=r.officeholder_id WHERE r.publication_status='PUBLISHED';
