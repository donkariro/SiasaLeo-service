# Governance API

Routes are below `/api`. New governance/asset JSON uses the snake_case fields
below. Bodies are allowlisted: clients cannot set IDs, recorders, reviewers,
revision numbers or publication timestamps. Writes return HTTP 200 with the
created/updated record. Missing records return 404, wrong owners 403, invalid
data/state 400 and duplicate identities 409.

Performance/history writes require ADMINISTRATOR. Manifesto writes require
ownership; asset writes require the permissions in [digital-assets.md](digital-assets.md).
Both require USER or ADMINISTRATOR. Admin status does not permit
publishing as someone else's candidate. Account IDs come from authentication.

## Catalogues

GET `/governance/catalogs/{kind}` exposes `policy-areas`, `units`, `indicators`,
`work-effort-types`, `participation-roles`, `evidence-relationships`. Asset types
moved to GET `/digital-assets/types`; `/governance/catalogs/asset-types` remains a compatibility alias.
Retired rows remain readable; reference retirement is currently a database
administration operation. Workflow states are fixed, not user-defined.

Administrators can POST the following catalogues:

| Kind | Fields |
| --- | --- |
| `policy-areas` | `code`, `name`; optional `description`, `display_order` |
| `units` | `code`, `name` |
| `indicators` | `code`, `name`, `unit` (existing unit name), `methodology`, `direction`; optional `unit_id`, `description` |
| `scopes` | `kind`, `population`; GEOGRAPHIC also needs `snapshot_id`, `area_id` |

Indicator direction: HIGHER_IS_BETTER, LOWER_IS_BETTER, TARGET_VALUE. Scope kind:
GLOBAL or GEOGRAPHIC. Reuse scope IDs for the same geography/population.
Indicators/scopes are immutable. GET `/offices` also returns `countryCode`.
Existing offices remain KE; office names/abbreviations are unique within country.

## Digital assets

Design notes: [digital-assets.md](digital-assets.md).

1. POST `/digital-assets`: `title`, `asset_type`; optional `description`,
   `visibility` (PRIVATE by default, or PUBLIC), `owner_party_id` (requires party media authority).
2. POST `/digital-assets/{id}/versions`: optional `provenance` (not blank when
   given), HTTP/HTTPS `source_url`, `author_party_id`, `publisher_party_id`,
   `source_published_on`.
3. POST `/digital-assets/versions/{id}/files` for an existing stored file:
   `representation_type`, `storage_key`, `original_filename`, `media_type`,
   positive `byte_size`, lowercase 64-character `sha256`.
4. POST `/digital-assets/versions/{id}/seal` before linking the version anywhere.

Representations: ORIGINAL, PREVIEW, THUMBNAIL, TRANSCRIPT. Sealing requires a source
URL or file metadata. GET `/digital-assets/me/versions/{id}` reads authorized versions;
GET `/digital-assets/versions/{id}` reads accessible versions (anonymous callers see sealed PUBLIC versions),
excluding storage keys. GET `/digital-assets/{id}/current` (or
`/digital-assets/me/{id}/current` for the owner) returns the latest sealed version,
for links that follow an asset instead of pinning a version.

Evidence (claims, observations, assessments, work efforts, manifesto and
commitment assets, announcements, officeholder evidence) must cite a sealed
version of a PUBLIC asset with provenance. Person asset links only need a sealed
version.
Source authors/publishers are independent of the uploading account.

This registers metadata; it does not receive bytes, fetch URLs, verify hashes,
generate previews or issue signed downloads. Those operations need a deployment
file/object-storage adapter; no provider or bucket is configured here. External
source URLs are usable independently. Sealing freezes a version; visibility is
separate and sealing does not authenticate the source. PUBLIC evidence must be
selected explicitly. Referenced public evidence cannot subsequently be hidden.

See [Digital asset uploads, storage, and downloads](digital-asset-file-storage.md)
for the proposed file transfer implementation, access rules, failure handling,
and acceptance checks. The proposed routes in that guide are not implemented yet.

## Historical officeholders

| POST route | Fields |
| --- | --- |
| `/office-history/people` | `first_name`, `last_name`, `asset_version_id`; optional `date_of_birth` |
| `/office-history/terms` | `seat_id`, `start_date`; optional `cycle_id`, `end_date` |
| `/office-history/tenures` | `seat_id`, `person_id`, `term_id`, `entry_reason_id`, `start_date`, `asset_version_id`; optional `end_date`, `exit_reason_id`, `candidacy_id`, `locator` |

Reuse existing people/terms. Ended tenures need an exit reason. Evidence must
reference sealed versions. No user account or candidacy is created for the
subject. A supplied candidacy must match person/seat; a term must match the seat.

## Performance

POST `/governance/performance/{kind}`:

| Kind | Fields |
| --- | --- |
| `work-efforts` | `title`, `description`, `type_code`; optional period fields, `scope_id` |
| `participations` | `work_effort_id`, `party_id`, `role_code`, `asset_version_id`; optional `officeholder_id`, period fields, `locator` |
| `claims` | `officeholder_id`, `title`, `description`, period fields; optional `claim_id`, `claimant_party_id`, `work_effort_id`, `scope_id`, `source_reference` |
| `observations` | `officeholder_id` and/or `work_effort_id`; either `indicator_id` + `actual_value` or `qualitative_result`; optional `scope_id`, period fields, `source_reference` |
| `targets` | `officeholder_id`, `indicator_id`, `period_start`, `period_end`, `target_value`, `source_reference`; optional `baseline_value`, `scope_id`, `notes` |
| `target-observations` | `target_id`, `observation_id` |
| `delivery-links` | `commitment_revision_id`, `work_effort_id`, `relationship_type` (CONTRIBUTES_TO/IMPLEMENTS), `rationale` |
| `assessments` | either `record_id` or `commitment_revision_id`; `evidence_status`, `criteria`, `rationale`; `delivery_status` for commitments; optional paired `commitment_target_id`, `observation_id` |

Period fields: `period_start`, `period_end`, `date_precision`. Claims default to
DAY; specify UNKNOWN and omit dates if unknown. Work, participation, observations
and commitment revisions default to UNKNOWN. MONTH/YEAR use calendar boundaries.

Example historical claim:

```json
{
  "officeholder_id": 17,
  "title": "Water programme during the former administration",
  "description": "An archived inspection report documents twelve operational facilities.",
  "period_start": "2001-01-01",
  "period_end": "2002-12-31",
  "date_precision": "YEAR"
}
```

The returned `id` is a revision; `claim_id` is stable. POST another claim with the
same `claim_id`/officeholder to correct a published claim without overwriting it.

Below `/governance/performance`:

- POST `/{kind}/{id}/evidence` for claims, observations, assessments, work-efforts:
  `asset_version_id`, optional `locator`, `caption`. Except for work efforts,
  include `relationship_code`: SUPPORTS, CHALLENGES or CONTEXT.
- POST `/{kind}/{id}/policy-areas/{policyId}` classifies claims or work efforts.
- PATCH `/{kind}/{id}` edits draft claims, observations or assessments using
  their content fields. Identity and account-attribution fields are protected.
- POST `/{kind}/{id}/publish` or `/withdraw` operates on those three collections.

Publication requires versioned evidence. UNREVIEWED/INSUFFICIENT_EVIDENCE reviews
can record an evidence gap without attachments, with delivery NOT_ASSESSABLE (or
no delivery status for a claim review). Other evidence states are SUPPORTED and
DISPUTED. Other delivery states: NOT_STARTED, IN_PROGRESS, FULFILLED,
PARTIALLY_FULFILLED, UNFULFILLED. Links alone do not establish delivery.

## Manifestos

The active account must be linked to the issuer. Existing candidacy supports
EXPRESSED_INTEREST, so no artificial nomination is needed. This implementation
supports individual registered aspirants. Organisational issuers, delegated
campaign accounts and pre-contest aspirancy are not exposed.

POST `/governance/manifestos` with `candidacy_id`. Subsequent routes below that
prefix are:

| POST route | Fields |
| --- | --- |
| `/{manifestoId}/versions` | `title`, `content`; optional `language` |
| `/{manifestoId}/goals` | `description` |
| `/{manifestoId}/commitments` | `wording`; optional `conditions`, period fields, `scope_id` |
| `/versions/{id}/commitments` | `commitment_revision_id` |
| `/revisions/{id}/targets` | `indicator_id`, `scope_id`, `target_value`, `period_start`, `period_end`; optional `baseline_value` |
| `/revisions/{id}/goals` | `goal_id` |
| `/revisions/{id}/policy-areas` | `policy_area_id` |
| `/versions/{id}/assets` or `/revisions/{id}/assets` | `asset_version_id`; optional `caption`, `locator` |
| `/versions/{id}/announcements` | `announced_at`, `venue`; optional `asset_version_id` |

Create commitments, attach their exact revisions to a version, then POST
`/versions/{id}/publish`. At least one commitment is required. Publication checks
eligible candidacy and a future election date, then seals referenced promises
and targets. Announcements identify a published edition and preserve the event
date separately from the recording timestamp.

PATCH `/versions/{id}` edits draft title/content/language; PATCH `/revisions/{id}`
edits unsealed wording/conditions/dates/scope. POST `/commitments/{id}/revisions`
creates an amended promise to attach to a new manifesto version. POST
`/versions/{id}/withdraw` preserves the original edition with withdrawn status.

## Read models

- GET `/governance/performance/people/{id}/track-record?page=0&size=25`: published
  claim revisions by tenure with evidence links and assessments.
- GET `/governance/performance/people/{id}/contributions?page=0&size=25`: sourced
  participation in work linked to published claims/manifestos.
- GET `/governance/performance/{kind}/{id}`: published/withdrawn claims,
  observations, assessments. ADMINISTRATOR `/editor/{kind}/{id}` also reads drafts.
- GET `/governance/manifestos/people/{id}?page=0&size=25`: published editions.
- GET `/governance/manifestos/versions/{id}`: published/withdrawn edition with
  exact promise revisions, targets and asset links. `/me/versions/{id}` also
  reads the owner's draft.
- GET `/governance/manifestos/revisions/{id}/delivery`: work links, participants
  and published assessments for that exact promise revision.

Paged lists accept page >= 0 and size 1..100. Published lists exclude withdrawn
entries; direct public reads retain them with their status. Conflicting
observations and competing assessments remain separate.
