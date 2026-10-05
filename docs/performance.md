# Governance performance

Performance documents work, attributed claims, observations and assessments.
Manifesto owns promises and their exact published revisions. Shared asset
records hold evidence metadata. See [governance-api.md](governance-api.md) for
HTTP workflows and request fields.

## Migration path

V46-V49 remain unchanged. V50-V54 extend them:

| Migration | Responsibility |
| --- | --- |
| V50 | Policy areas, units, assets/files, measurement scopes, country-scoped office names |
| V51 | Work efforts, participation, stable claims, claim revisions and independent observations |
| V52 | Manifestos, goals, commitment revisions/targets, announcements, delivery links and assessments |
| V53 | Publication guards, immutable evidence and comparison rules |
| V54 | Historical tenure evidence, additional integrity guards, indexes and international office levels |

`performance_record` now holds revisions of `achievement_claim`. Existing IDs,
descriptions, dates and source strings survive. Each existing row becomes the
first draft revision of a stable claim. No author, registration, verification
or publication is inferred. Create an attributed new revision and attach sealed
evidence before publishing imported claims.

Existing measurements are copied into `performance_observation`, retaining the
old measurement ID and comparison link. `performance_measurement` remains for
legacy compatibility; new application writes use observations. This is a
one-time migration, not synchronization with legacy writers. Coordinate rollout
so old clients do not continue writing legacy measurements. Custom indicator
units are preserved without inventing conversions. No score is calculated.

## Identity, evidence and history

An officeholder and person do not need user accounts. Administrators can create
sourced historical people and tenures, then record claims without targets or
manifestos. Claimant party, participating party, recording account and assessment
reviewer are separate references. Null claimant means not recorded, not the
officeholder's endorsement. A later account claim reuses the existing person.

One work effort can span administrations. Participation records carry a role,
optional matching officeholding, dates and a specific evidence version. An
organisation can participate without an officeholding. A delivery link records
relevance to a promise; it does not establish fulfilment or award credit.

Evidence identifies a sealed asset version, SUPPORTS/CHALLENGES/CONTEXT, and
an optional locator/caption. The same report can be reused. Sealing preserves
metadata and makes the version public; it does not authenticate source content.

Publication is DRAFT/PUBLISHED/WITHDRAWN. Published content and its evidence
associations are immutable. Corrections create new revisions or observations.
Withdrawal preserves original content and timestamps; direct public reads still
show withdrawn records with their status. Published lists exclude withdrawn rows.
All published claim revisions remain in track-record results.

Delivery status and evidence review are separate assessment fields. Missing or
unreviewed evidence permits NOT_ASSESSABLE, not an automatic failure score.
Assessments retain criteria, rationale, reviewer and evidence. Later assessments
are separate records and do not overwrite earlier opinions.

## Measurement and dates

`performance_indicator` is the shared immutable indicator catalogue. Create a
new definition/code to change its unit or methodology. Scopes are immutable and
identify population and global extent or a published geographic snapshot/area.
Unknown legacy scopes remain null.

Observations can be numeric or qualitative. Sources may disagree on the same
date. Numeric comparisons require identical indicators and scopes plus an exact
observation period contained in the target period. This checks comparability,
not whether the evidence justifies the assessment.

Work, claims, participation and commitments support DAY, MONTH, YEAR,
APPROXIMATE and UNKNOWN precision. YEAR/MONTH use enclosing calendar boundaries;
UNKNOWN requires both dates null. Creation timestamps remain separate from
historical dates. Numeric targets use exact periods.

## Verification

```text
mvn test                                         # Java tests, then every PGlite suite
cd src/test/sql && npm ci                        # or run the database checks on their own
node run-performance.mjs
node run-governance.mjs
```

The SQL runners apply all migrations to an isolated PostgreSQL/WASM database
using a small real geography seed. `--full` uses the complete seed. Governance
checks cover upgrading populated V49 data, unregistered historical subjects,
conflicting observations, approximate dates, sealed evidence, publication
eligibility, preserved pledges, incompatible populations and immutable links.
Java tests cover ownership, attribution, field allowlists and endpoint protection.
Multi-connection PostgreSQL concurrency, Payara HTTP deployment and object-storage
integration require deployment testing.
