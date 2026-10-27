# Nominations

A political party's nomination exercise: aspirants apply for a contest in an
election event by completing the party's application form. Nominations owns the
exercise, the applications and who may act when; the [forms](forms.md) domain owns
the questions and answers.

Status: v1 implemented in V60. Review, decisions, appeals and certificates are
not implemented; SUBMITTED is the last application state.

## Model

| Concept | Meaning |
| --- | --- |
| Nomination manager | An account explicitly authorized to run a party's nominations, assigned by an ADMINISTRATOR |
| Exercise | One party's application round for an election event: DRAFT → OPEN → CLOSED, with an `opens_at`/`closes_at` window |
| Exercise form | The published form version an exercise uses for a purpose (`INITIAL_APPLICATION`) |
| Application | One aspirant's application for one contest: DRAFT → SUBMITTED, or WITHDRAWN |
| Application form | The forms instance holding the application's answers |

An exercise may use the platform nomination core directly or the party's own
extension of it; another tenant's form is rejected. Forms are fixed once the
exercise opens, so applicants all answer the same version.

## Rules

- Party membership, office or the ADMINISTRATOR role grant no nomination
  authority. Administrators assign managers and author forms, but cannot read
  applications unless they are also managers.
- Only a current member of the party can apply, and only for a contest in the
  exercise's election event. The applicant is always the caller's linked person.
- An aspirant has at most one live application per exercise. A withdrawn
  application stays as history and cannot be reopened; the aspirant may apply again.
- Applicants save, submit and correct only while the exercise is OPEN and inside
  its window. Each submit creates a new immutable form revision.
- A submission snapshots exercise, party, event, contest, person and membership
  start date as its context, so later profile changes do not rewrite it.

## API

Routes are relative to `/api/nominations`. JSON uses snake_case.

| Method and route | Who | Behavior |
| --- | --- | --- |
| PUT /parties/{party}/managers/{account} | ADMINISTRATOR | Assign a nomination manager |
| DELETE /parties/{party}/managers/{account} | ADMINISTRATOR | Revoke |
| GET /parties/{party}/exercises | Anyone | Managers see drafts; others see opened exercises |
| POST /exercises | Manager | political_party_id, election_event_id, title, opens_at, closes_at |
| GET /exercises/{id} | Anyone | Drafts only for managers |
| PUT /exercises/{id}/forms/INITIAL_APPLICATION | Manager | form_version_id; draft exercises only |
| POST /exercises/{id}/open | Manager | row_version; needs an application form |
| POST /exercises/{id}/close | Manager | row_version |
| GET /exercises/{id}/applications | Manager | Application summaries |
| POST /exercises/{id}/applications | Member | contest_id; returns the application with its form |
| GET /applications/mine | Applicant | The caller's applications |
| GET /applications/{id} | Applicant or manager | Application, definition, draft answers, revisions |
| PUT /applications/{id}/answers | Applicant | answers + the form's row_version |
| POST /applications/{id}/submit | Applicant | row_version + accepted_declarations |
| GET /applications/{id}/submissions/{revision} | Applicant or manager | One submitted revision |
| POST /applications/{id}/withdraw | Applicant | row_version of the application |

## Example flow

1. An administrator publishes the `nomination.application` core, and optionally
   a party extension (`tenant_type` `POLITICAL_PARTY`, `tenant_ref` the party id).
2. An administrator assigns the party's nomination manager.
3. The manager creates an exercise, binds the form and opens it.
4. A member applies for a contest, saves answers, uploads and seals documents in
   the digital asset library, references their version IDs, and submits.

## Deferred

- Review workspace, findings, decisions and appeals.
- Reviewer access to submitted private documents (application-scoped grants).
- Nomination certificates and fees.
- Further form purposes, such as supplementary information or appeals.
