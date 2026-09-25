# Election records throughout their lifetime

An election becoming older does not move or reclassify any record. Past, current
and future elections use the same election events, contests, candidacies, voter
registers and result tables. Imports are administrative entry mechanisms into
those domains. There is no historical domain, historical-only endpoint or generic
publication table shared between registers and results.

## Ownership

| Domain | Records and behavior |
| --- | --- |
| `election` | Events, contest jurisdictions, sourced register assignments, and election results through the `election.result` feature |
| `electoralgeography` | Published geographical editions with immutable nodes |
| `candidate` | Core candidacies, administrative capture, and ballot names/affiliations |
| `voter` | Official register editions, counts, validation and publication; existing personal declarations remain separate |
| `election.result` | Result publications, candidate rosters, votes, ballots, summaries and revisions |

Results belong to the election domain. The `election.result` feature keeps its
own services, DTOs, repositories, and endpoints to manage its publication lifecycle.

Only technical utilities—bound SQL execution, fingerprints, paging and error
responses—are shared in infrastructure. Register and result services own separate
validation, persistence queries, endpoints, and publication rules.

## Capture an election

Use `POST /election-events` for any date. Existing cycle/date bounds still apply.
Optional `geographySnapshotId` identifies published geography. Optional `statusId`
records an already-known status when accompanied by `sourceReference`; omitted
status defaults to SCHEDULED. Existing status transitions continue to work.

Use `POST /contests` with either an operational `seatId`, or `officeId` plus
`jurisdictionId` from the event's geography. Supplying both requires a reviewed
correspondence. `PUT /election-events/{id}/geography` and
`PUT /contests/{id}/jurisdiction` bind previously created records. Finish geography
review before binding: the event's geography and assigned contest jurisdiction
are fixed to protect dependent records.

Cycles and offices remain migration-maintained catalogs. Earlier elections that
need different reference data or geographic hierarchies require those catalogs
and the geography model to be extended with verified source data.

## Candidacies

Self-service registration continues to create the core `Candidacy` entity. Its
ballot name and party label are captured at registration, rather than resolved
from a later profile. Administrative `POST /candidacies/imports` creates the same
entity without requiring an account or a self-declared voter registration:

```json
{
  "contestId": 30,
  "personId": 15,
  "politicalPartyId": null,
  "statusId": 9,
  "ballotName": "Candidate A",
  "ballotPartyName": null,
  "sourceReference": "archive/ballot-notice.pdf",
  "sourceRecordReference": "page 1 row 2"
}
```

IDs are illustrative. Supply either an existing `personId` or a new `profile`
with first/last name, optional date of birth and gender. No person identity is
inferred by matching names. A null party ID means independent; a party candidacy
requires its election-time `ballotPartyName`. Import source records are unique
per contest, with exact payload replays returning the existing candidacy.

`PUT /candidacies/{id}/ballot` records verified ballot labels and source references
for an existing candidacy. Its body contains `ballotName`, `ballotPartyName`,
`sourceReference` and `sourceRecordReference`. This is also how existing legacy
candidacies obtain verified labels. Previously published result rosters remain
unchanged when a profile, party or ballot label is corrected.

## Official voter registers

`voter_register` represents a sourced register edition, not an election's age.
`voter_register_count` records nonnegative counts at one declared geographical
level. This is independent of the platform's self-declared `voter_registration`.

Create a draft with `POST /voter-registers`:

```json
{
  "geographySnapshotId": 10,
  "scopeAreaId": 101,
  "areaTypeId": 7,
  "sourceReference": "archive/register-manifest.json",
  "sourceSha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
  "coverage": "PARTIAL",
  "supersedesId": null,
  "counts": [{"areaId": 107, "registeredVoters": 500, "sourceRecordReference": "row 2"}]
}
```

Publish it, then assign it through
`POST /election-events/{id}/voter-register/assignments`:

```json
{"registerId": 40, "supersedesId": null, "sourceReference": "archive/register-use-notice.pdf"}
```

Assignments are append-only. A replacement supplies the current assignment ID
as `supersedesId`. A register can serve multiple election events; publishing a
corrected edition never silently changes existing assignments.

## Results

`result_publication` identifies a contest, source, granularity, coverage and stage
(PROVISIONAL or OFFICIAL). Its geography and scope come from the core contest.
Both stages store their votes in **`contest_result`**, referencing real candidacies.
The `result_candidate` roster freezes labels from those candidacies for each
publication; it is not another candidate registry.

Create a draft with `POST /result-publications`:

```json
{
  "contestId": 30,
  "areaTypeId": 7,
  "stage": "OFFICIAL",
  "sourceReference": "archive/result-manifest.json",
  "sourceSha256": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
  "coverage": "PARTIAL",
  "supersedesId": null,
  "candidacyIds": [50, 51],
  "votes": [
    {"candidacyId": 50, "areaId": 107, "voteCount": 250},
    {"candidacyId": 51, "areaId": 107, "voteCount": 140}
  ],
  "ballots": [{"areaId": 107, "ballotsCast": 400, "rejectedBallots": 10}]
}
```

Every candidacy must belong to the contest and have verified ballot labels.
Append later batches using the same metadata, with `candidacyIds` containing only
new roster members. Votes may reference candidacies already in that roster.

## Lifecycle, reads and calculations

Both register and result endpoints support:

| Method | Relative path | Purpose |
| --- | --- | --- |
| GET / POST | `/` | List visible records / create or import a draft |
| POST | `/validate` | Read-only input/reference validation |
| GET / DELETE | `/{id}` | Read / discard a draft |
| POST | `/{id}/batches` | Append up to 10,000 fact rows transactionally |
| GET | `/{id}/validation` | Validate accumulated counts and coverage |
| POST | `/{id}/publish` | Validate and freeze |
| GET | `/{id}/summary?areaId=...` | Aggregate one record over a compatible subtree |

Registers additionally expose `/{id}/counts`. Results expose `/{id}/candidates`,
`/{id}/votes`, `/{id}/ballots`, and `/{id}/batches/validate`. List endpoints use
zero-based `page` and `size` (default 100, maximum 500).

`GET /election-events/{id}/voter-register` reads the assigned edition and totals;
`/counts`, `/assignment`, and `/assignments` expose its facts and assignment trail.
`GET /contests/{id}/results?stage=OFFICIAL` selects the current published official
revision. Use `stage=PROVISIONAL` for provisional results, or `publicationId` for
an explicit published revision in the same contest/stage. `/results/revisions`
lists published revisions across both stages.

Published editions, result publications, and their facts are immutable in the
database. A correction creates a new draft with `supersedesId`, imports the full
replacement dataset, and publishes it. A result revision stays within the same
contest and stage. Publishing official results does not erase provisional results.
There is at most one published successor and one published initial result per
contest/stage. A draft never changes which published results readers see.

Counts use one area type per edition/publication, preventing double-counting of
station figures and parent totals. COMPLETE requires every expected node within
scope; PARTIAL permits gaps. Missing values remain unknown, not zero. Every
reported result area requires an explicit vote count for every roster member.
Ballots are optional, but supplied totals must reconcile: candidate votes cannot
exceed ballots cast; if rejected ballots are supplied, votes plus rejected ballots
must equal ballots cast. Use the total of non-candidate ballots for this field.

Ranks and percentages use a single selected publication. Partial coverage is
visible. Turnout requires complete compatible ballot and register coverage for
the requested territory; otherwise it is null with an explanation. Responses
identify the exact register assignment used, including when reading old result
revisions. Register counts at a larger geography cannot supply a smaller area's
denominator.

All writes and validation endpoints require ADMINISTRATOR. Public readers see
published records only. Sources are supplied archive references and lowercase
SHA-256 digests; files are not downloaded or authenticated automatically. Imports
accept structured JSON. Exact ordered payload replays are idempotent; changed
payloads for an existing source/context and overlapping batches are rejected.
Corrections to unpublished imports can discard and recreate the draft. Missing
records return 404, invalid input 400, and database/revision conflicts 409.

## Migration and verification

V45 is the replacement for the unshipped historical implementation. V1–V44 remain
unchanged. Do not apply this replacement over an installation that independently
applied the discarded V45; that would require a separately reviewed migration.

The migration extends `contest_result`, retains existing official IDs/values,
and copies provisional values and timestamps into it. Each copied provisional
row retains its old table/ID as a source-record reference. The former provisional
table becomes a compatibility read view; new writes require a result publication.
Existing records without verified geography keep their operational station IDs.
No station-code or name matching is used to assign them to snapshots.

Compatibility result summaries select one published revision per contest/stage,
falling back to legacy rows only when that stage has no published publication.
They never sum legacy, provisional, official and corrected versions together.
The officeholder mandate view is rebuilt against the selected official summary.

```text
mvn test
npm install --prefix target/snapshot-db-test --no-save --package-lock=false @electric-sql/pglite@0.5.8
node src/test/sql/run-election-records.mjs
node src/test/sql/run-election-records.mjs --full
```

The isolated runner applies all migrations and tests legacy preservation,
provisional/official separation, immutability, coverage, ballot reconciliation,
candidacy integrity and the same workflow for past/future elections. `--full`
uses the complete geography seed. PGlite does not verify concurrent PostgreSQL
connections or Payara/JPA deployment; those need the integration environment.
