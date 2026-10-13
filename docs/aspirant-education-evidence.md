# Aspirant education evidence

Status: design proposal, recorded 2026-10-12. Nothing below is implemented.
Three decisions remain open; see [Open decisions](#open-decisions).

Political parties may require aspirants to support their education records with
documents. This guide explains how that requirement fits the existing
[education](education-api.md) and [digital asset](digital-assets.md) domains
without duplicating either.

## Problem

The education domain records qualifications that a person states. It does not
take documents as proof, and it does not verify credentials or determine
candidate eligibility. Some parties will want proof from their aspirants, such as
a degree certificate for particular seats. Other users must still be able to sign
up without uploading anything.

## Separate concerns

The feature involves three concerns. Each has its own owner:

| Concern | Example | Owner |
| --- | --- | --- |
| Claim | "BSc, University of Nairobi, 2012–2016" | `education`: `PersonEducation`, unchanged |
| Evidence | Certificate or transcript: versions, sealing, ownership and access | `digitalasset`: existing domain |
| Requirement and vetting | "Aspirants for this seat must prove a degree" and the party's verdict | `politicalparty` and `candidate`: new |

Keeping them separate avoids duplication. `PersonEducation` gets no file columns,
and parties get no separate upload store.

## Design principles

### Evidence attaches to the claim

A new association, `person_education_evidence`, is owned by the education domain
and pins sealed asset versions to one education record. It follows the existing
consumer-association pattern used for person attachments.

Do not reuse `person_digital_asset` with a purpose such as `EDUCATION_CERT`.
Purpose is a free string, so that association cannot record which qualification
a document supports.

Evidence is optional in the education domain. Ordinary sign-up is unchanged.

### The person owns the evidence

Evidence assets are PRIVATE and owned by the aspirant's account. One upload can
be presented to several parties over time, which matters because people defect.
If each party owned its own upload, the platform would hold one copy per party,
and an aspirant who left a party would lose control of their documents.

### Verification belongs to the submission

A party's acceptance of a document is that party's judgment for one nomination.
It is not a fact about the person. Verification is therefore stored on the
candidacy submission, not on `PersonEducation`. Acceptance by party A does not
make the document verified for party B or on the public profile.

A submission pins an exact sealed version and stores a snapshot of the claim. A
later edit, deletion or new asset version cannot change what the party reviewed.
Instead, the submission is reported as stale.

### Access follows the submission

Party vetters must read an aspirant's private document. Per-account grants
(`digital_asset_grant`) would have to be added for each official and revoked
manually when officials change or submissions are withdrawn. Access is derived
instead: party P's nomination officers may read an asset version while that
version is pinned to an active submission to P.

That rule depends on `candidate`, and `digitalasset` must not depend on
`candidate`. The asset domain therefore exposes an extension interface, and the
candidate domain implements it.

### Dependency direction

```
candidate ──► education ──► digitalasset
    │                            ▲
    └── implements AssetReadContributor
```

`digitalasset` stays free of dependencies on education and candidacy. This
matches moving `PersonDigitalAsset` into `party`: association tables belong to
the domain that uses the media.

## Implementation plan

### Phase 0: prerequisites

- Commit the move of `PersonDigitalAsset` into `party`.
- File transfer is still deferred (see the
  [file storage proposal](digital-asset-file-storage.md)). Until it exists,
  evidence is limited to an external `sourceUrl` or file metadata. The design
  supports real uploads without changes once they exist, but the feature is not
  ready for users before then.

### Phase 1: migration V59

`V59__education_evidence_and_nomination_vetting.sql`:

| Object | Purpose | Key constraints |
| --- | --- | --- |
| `digital_asset_type` row | Add `TRANSCRIPT`; reuse the existing `CERTIFICATE` | Seed data |
| `person_education_evidence` | Links a claim to its proof | PK `(person_education_id, asset_version_id)`; `document_kind` (`CERTIFICATE`, `TRANSCRIPT`, ...); FK to `person_education` `ON DELETE CASCADE` |
| `politicalparty_nomination_officer` | Accounts allowed to vet a party's aspirants | PK `(politicalparty_id, account_id)`; `granted_by_account_id`, `granted_at` |
| `nomination_requirement` | A party's rules | `politicalparty_id`; optional `office_id`; `min_education_level`; `evidence_required`; `active` |
| `candidacy_education_submission` | What the aspirant submitted and the party's verdict | `candidacy_id`; `person_education_id` `ON DELETE SET NULL`; snapshot of level, institution, field and dates; pinned `asset_version_id`; `status` (`SUBMITTED`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`); `reviewed_by_account_id`, `reviewed_at`, `review_note` |

### Phase 2: asset extension interface

- Add `digitalasset/control/AssetReadContributor` with
  `boolean canRead(long assetId, long versionId, long actor)`.
- `DigitalAssetService` injects `Instance<AssetReadContributor>`. It consults
  contributors only in `read(versionId, actor)` and only for sealed versions.
- Contributors are not consulted in `current()`, `readableAsset()` or
  `search()`. A vetter can open the exact pinned version, but cannot browse the
  aspirant's library or see later editions.
- Tests: a contributor can allow a sealed version; drafts stay hidden; with no
  contributors, behavior is unchanged.

### Phase 3: education evidence

- Add the `PersonEducationEvidence` entity and repository, plus
  `EducationEvidenceService`.
- Attaching requires all of the following:
  - The record belongs to the caller's person.
  - The asset's `owner_account_id` is the caller, so nobody can attach another
    person's document.
  - The asset type is an education type.
  - The version is sealed.

  Use `DigitalAssetService.link()` to get the existing archive check and lock.
- Routes under `/education/me/records/{id}/evidence`:

  | Method | Relative path | Result |
  | --- | --- | --- |
  | GET | (collection) | Evidence pinned to the record |
  | POST | (collection) | Attach a sealed version with `documentKind` |
  | DELETE | `/{versionId}` | Detach one version |

- `PersonEducationDto` stays unchanged. Evidence is fetched separately.

### Phase 4: party vetting configuration

- Grant and revoke nomination officers.
- Create, list and deactivate requirements.

### Phase 5: candidate submission and review

- **Submit**: the caller is the candidacy's person; the candidacy has a
  political party; its status is `EXPRESSED_INTEREST` or `PARTY_PRIMARY`. The
  pinned version must be evidence for the chosen education record. The service
  copies the claim snapshot.
- **Withdraw**: by the aspirant. Withdrawal removes the vetters' derived access.
- **Review**: a nomination officer accepts or rejects with a note.
- **Party queue**: lists submissions per party for its officers.
- **Requirement status**: read-only per candidacy. Each requirement is met,
  unmet, or met but stale.
- `CandidacyEvidenceReadContributor` implements `AssetReadContributor`. It allows
  reading a version pinned to a non-withdrawn submission when the actor is a
  nomination officer of the candidacy's party.
- Tests extend `CandidacyServiceTest`. They must cover two cases: an officer of
  party A cannot read party B's submissions, and withdrawal revokes access.

## Out of scope

- Moving the hardcoded access SQL in `PartyMediaService.attachments` and
  `AssetManagementRepository.search` onto the extension interface. This is worth
  doing later.
- External verification with the IEBC or the Commission for University Education.

## Open decisions

1. **Who vets, and who appoints them?** The recommended approach is a separate
   nomination-officer role.
   - Reusing `party_media_manager` would let whoever manages the party logo read
     aspirants' certificates.
   - `PoliticalPartyOfficial.positionName` is free text, so it cannot be used to
     check access.
   - Media managers are assigned by ADMINISTRATOR. Officers should be assigned
     the same way unless parties manage their own staff.
2. **Should unmet requirements block a status change** to `PARTY_PRIMARY` or
   `PARTY_NOMINEE`? Recommended: not yet. Report requirement status and let
   parties decide.
3. **Requirement scope**: party-wide with optional per-office rules
   (recommended), or party-wide only.
