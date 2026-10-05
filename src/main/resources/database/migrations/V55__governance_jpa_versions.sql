-- Optimistic concurrency for JPA-managed draft aggregates.
ALTER TABLE work_effort ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE performance_record ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE performance_observation ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE performance_assessment ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE manifesto_version ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE commitment_revision ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE digital_asset_version ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE performance_target ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;

-- Withdrawal preserves content while JPA advances its optimistic version.
CREATE OR REPLACE FUNCTION guard_governance_publication() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE has_evidence BOOLEAN; actor_id BIGINT; manifest_id BIGINT; revision_id BIGINT;
BEGIN
 IF TG_OP='INSERT' AND NEW.publication_status <> 'DRAFT' THEN
   RAISE EXCEPTION 'Create a draft before publishing' USING ERRCODE='23514';
 END IF;
 IF TG_OP <> 'INSERT' AND OLD.publication_status <> 'DRAFT' THEN
   IF TG_OP='UPDATE' AND OLD.publication_status='PUBLISHED' AND NEW.publication_status='WITHDRAWN'
      AND (to_jsonb(NEW)-'publication_status'-'row_version')=(to_jsonb(OLD)-'publication_status'-'row_version') THEN RETURN NEW; END IF;
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
