-- V40: person_claim — an account's assertion that it belongs to a person
-- already in the party model, and the decision on it.
--
-- Two very different things link a user_account to a person. A voter
-- self-declares and nobody impersonates a voter, so that link is created
-- outright (PersonProfileService). An aspirant is usually already here — a
-- sitting officeholder (V14) or a past candidate (V11) seeded from public
-- sources — so creating a second person row would split one public figure's
-- history across two ids. That link is therefore a *claim* on an existing row
-- and has to be decided by a human before it takes effect: without review,
-- any account could declare itself the Governor.
--
-- The claim is recorded here rather than as a status on user_account because
-- it outlives its own outcome: a rejected claim is evidence when the same
-- account claims the same person again, and an approved one is the audit
-- trail behind a link that grants real authority over a public figure's
-- record. user_account_id is a plain FK value on the entity side, matching
-- how user_account holds person_id — the security and party domains stay
-- decoupled in code even though the database knows both keys.
--
-- APPROVED is the only status that writes user_account.person_id; that column
-- is UNIQUE, so the database is the final guarantee that one person is never
-- linked to two accounts. The partial unique indexes below are the earlier,
-- friendlier guard: one open claim per account, and one open claim per
-- person, so two people cannot both have a pending claim on the same
-- politician and race to approval. Decided rows are left unconstrained and
-- accumulate as history, exactly as retired voter_registration rows do (V15).

CREATE TABLE person_claim (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_account_id BIGINT NOT NULL REFERENCES user_account (id),
    person_id       BIGINT NOT NULL REFERENCES person (id),
    status          VARCHAR(20) NOT NULL CHECK (status IN
                        ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    -- What the claimant offers as proof: a party nomination reference, a
    -- gazette notice, a link to an official page. Free text on purpose — the
    -- reviewer reads it, the system does not parse it.
    evidence        VARCHAR(1000),
    submitted_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at      TIMESTAMP WITH TIME ZONE,
    -- The reviewing account, not the claimant. NULL while PENDING, and stays
    -- NULL for WITHDRAWN: the claimant ended it themselves.
    decided_by      BIGINT REFERENCES user_account (id),
    decision_note   VARCHAR(500),
    CHECK ((status = 'PENDING') = (decided_at IS NULL))
);

CREATE INDEX idx_person_claim_account ON person_claim (user_account_id);
CREATE INDEX idx_person_claim_person ON person_claim (person_id);
-- The review queue: oldest first, and only ever the pending rows.
CREATE INDEX idx_person_claim_pending ON person_claim (submitted_at) WHERE status = 'PENDING';

CREATE UNIQUE INDEX idx_person_claim_open_account ON person_claim (user_account_id)
    WHERE status = 'PENDING';
CREATE UNIQUE INDEX idx_person_claim_open_person ON person_claim (person_id)
    WHERE status = 'PENDING';
