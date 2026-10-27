-- V60: nominations — a political party's aspirant application exercise for an
-- election event (docs/nominations.md). Application answers live in forms;
-- this domain owns the exercise, the applications and their form links.

-- Explicit authority to run a party's nominations, provisioned by an
-- ADMINISTRATOR. Membership or office alone grants none.
CREATE TABLE nomination_manager (
 political_party_id BIGINT NOT NULL REFERENCES political_party(id),
 account_id BIGINT NOT NULL REFERENCES user_account(id),
 granted_by_account_id BIGINT REFERENCES user_account(id),
 granted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY (political_party_id, account_id)
);
CREATE INDEX nomination_manager_account ON nomination_manager(account_id);

CREATE TABLE nomination_exercise (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 political_party_id BIGINT NOT NULL REFERENCES political_party(id),
 election_event_id BIGINT NOT NULL REFERENCES election_event(id),
 title VARCHAR(255) NOT NULL CHECK (btrim(title)<>''),
 status VARCHAR(10) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','OPEN','CLOSED')),
 opens_at TIMESTAMPTZ NOT NULL,
 closes_at TIMESTAMPTZ NOT NULL,
 created_by_account_id BIGINT REFERENCES user_account(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 row_version BIGINT NOT NULL DEFAULT 0,
 CHECK (closes_at>opens_at)
);
CREATE INDEX nomination_exercise_party ON nomination_exercise(political_party_id);

-- The published form version an exercise uses for each purpose.
CREATE TABLE nomination_exercise_form (
 exercise_id BIGINT NOT NULL REFERENCES nomination_exercise(id),
 purpose_code VARCHAR(40) NOT NULL CHECK (purpose_code IN ('INITIAL_APPLICATION')),
 form_version_id BIGINT NOT NULL REFERENCES forms.form_version(id),
 PRIMARY KEY (exercise_id, purpose_code)
);

CREATE FUNCTION guard_nomination_exercise_form() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE target BIGINT;
BEGIN
 target:=CASE WHEN TG_OP='DELETE' THEN OLD.exercise_id ELSE NEW.exercise_id END;
 IF NOT EXISTS(SELECT 1 FROM nomination_exercise WHERE id=target AND status='DRAFT' FOR UPDATE) THEN
  RAISE EXCEPTION 'Exercise forms are fixed once the exercise opens' USING ERRCODE='23514';
 END IF;
 IF TG_OP<>'DELETE' AND NOT EXISTS(SELECT 1 FROM forms.form_version WHERE id=NEW.form_version_id AND status='PUBLISHED') THEN
  RAISE EXCEPTION 'Exercises use published form versions' USING ERRCODE='23514';
 END IF;
 RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END $$;
CREATE TRIGGER nomination_exercise_form_guard BEFORE INSERT OR UPDATE OR DELETE ON nomination_exercise_form
 FOR EACH ROW EXECUTE FUNCTION guard_nomination_exercise_form();

CREATE TABLE nomination_application (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 exercise_id BIGINT NOT NULL REFERENCES nomination_exercise(id),
 person_id BIGINT NOT NULL REFERENCES person(id),
 contest_id BIGINT NOT NULL REFERENCES contest(id),
 status VARCHAR(10) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SUBMITTED','WITHDRAWN')),
 created_by_account_id BIGINT REFERENCES user_account(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 submitted_at TIMESTAMPTZ,
 withdrawn_at TIMESTAMPTZ,
 row_version BIGINT NOT NULL DEFAULT 0,
 CHECK (status<>'SUBMITTED' OR submitted_at IS NOT NULL),
 CHECK ((status='WITHDRAWN') = (withdrawn_at IS NOT NULL))
);
-- One live application per aspirant per exercise; withdrawn ones stay as history.
CREATE UNIQUE INDEX nomination_application_live ON nomination_application(exercise_id, person_id) WHERE status<>'WITHDRAWN';
CREATE INDEX nomination_application_person ON nomination_application(person_id);

CREATE FUNCTION guard_nomination_application() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='UPDATE' AND (NEW.exercise_id<>OLD.exercise_id OR NEW.person_id<>OLD.person_id OR NEW.contest_id<>OLD.contest_id) THEN
  RAISE EXCEPTION 'An application keeps its exercise, applicant and contest' USING ERRCODE='23514';
 END IF;
 IF TG_OP='UPDATE' AND OLD.status='WITHDRAWN' AND NEW.status<>'WITHDRAWN' THEN
  RAISE EXCEPTION 'Withdrawn applications cannot be reopened' USING ERRCODE='23514';
 END IF;
 IF TG_OP='INSERT' AND NOT EXISTS(SELECT 1 FROM nomination_exercise WHERE id=NEW.exercise_id AND status='OPEN' FOR SHARE) THEN
  RAISE EXCEPTION 'The exercise is not accepting applications' USING ERRCODE='23514';
 END IF;
 IF TG_OP='INSERT' AND NOT EXISTS(SELECT 1 FROM nomination_exercise e JOIN contest c ON c.election_event_id=e.election_event_id
     WHERE e.id=NEW.exercise_id AND c.id=NEW.contest_id) THEN
  RAISE EXCEPTION 'The contest belongs to another election event' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER nomination_application_guard BEFORE INSERT OR UPDATE ON nomination_application
 FOR EACH ROW EXECUTE FUNCTION guard_nomination_application();

-- The form instance holding an application's answers for each purpose.
CREATE TABLE nomination_application_form (
 application_id BIGINT NOT NULL REFERENCES nomination_application(id),
 purpose_code VARCHAR(40) NOT NULL CHECK (purpose_code IN ('INITIAL_APPLICATION')),
 form_instance_id BIGINT NOT NULL UNIQUE REFERENCES forms.form_instance(id),
 PRIMARY KEY (application_id, purpose_code)
);
