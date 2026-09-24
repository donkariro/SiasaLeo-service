# Historical electoral geography

The existing `electoral_area` hierarchy remains operational. Historical browsing
uses `electoral_geography_snapshot` and `electoral_area_snapshot`; each node's
parent and materialized path refer exclusively to nodes in the same snapshot.
The snapshot stores the entire hierarchy, including its WORLD root.

## Lifecycle and provenance

Create a DRAFT, add nodes from root to leaves, then publish after reviewing its
source and coverage. Each snapshot has an optional reference date, source
reference (archive path, document identifier or URL), and source SHA-256 digest.
Publication requires the source reference and digest. These fields describe the
source; they do not mean the platform has independently authenticated it. For a
multi-file source, reference and hash an archived manifest listing the files.

Publication checks the hierarchy, paths, sibling codes, one root and at least
one polling station. This structural validation does **not** certify that every
station in Kenya has been imported: administrators must check source coverage
before publication. Dates and codes are never inferred from names.

Published metadata and nodes cannot be updated or deleted. Database triggers
guard writes, including direct SQL imports. Node writes and publication lock the
same snapshot row. Nodes in a draft may have their names, codes and source-record
references edited. Identity, type and parent are fixed; delete leaves upward and
recreate a branch to correct ancestry. All lists are paginated (maximum 500).

A correction starts with `POST /geography-snapshots/{id}/revisions`. This copies
the published tree into a new draft, remapping IDs and paths, and records the
predecessor. Revision metadata is supplied explicitly; source provenance is not
silently inherited. Original publications and references to them remain intact.

`electoral_area_correspondence` optionally links a historical node to an
operational node. It does not determine historical grouping. Its evidence can
be reviewed separately, including after publication. A same-type check is
required, but matching codes alone is not evidence of identity. Split/merge
crosswalks are not represented as same-place correspondences.

## API

Paths are relative to the application's existing REST base path. Public callers
see published snapshots only; administrators can also inspect drafts. Hidden
drafts and area IDs from another snapshot return 404. All writes and
correspondence reads require the existing ADMINISTRATOR role.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/geography-snapshots?page=0&size=100` | Visible snapshots |
| POST | `/geography-snapshots` | Create draft |
| GET / PUT | `/geography-snapshots/{id}` | Read / replace draft metadata |
| POST | `/geography-snapshots/{id}/publish` | Validate and freeze |
| POST | `/geography-snapshots/{id}/revisions` | Copy into a correction draft |
| GET | `/geography-snapshots/{id}/areas?type=COUNTY&page=0&size=100` | Browse by type |
| POST | `/geography-snapshots/{id}/areas` | Add draft node |
| GET / PUT / DELETE | `/geography-snapshots/{id}/areas/{areaId}` | Read / edit / delete leaf |
| GET | `/geography-snapshots/{id}/areas/{areaId}/children` | Paginated direct children |
| GET | `/geography-snapshots/{id}/areas/{areaId}/subtree` | Paginated descendants, excluding node |
| GET / PUT | `/geography-snapshots/{id}/areas/{areaId}/correspondence` | Inspect / review identity match |

Create/update metadata and create-revision body:

```json
{
  "name": "Historical geography awaiting verification",
  "referenceDate": null,
  "sourceReference": null,
  "sourceSha256": null
}
```

For publication, replace the null source fields with the actual archived source
reference and lowercase 64-character SHA-256 digest. `referenceDate` may remain
unknown. Area creation takes `name`, `areaCode`, `areaType`, `parentId` and
`sourceRecordReference`; root creation uses `WORLD` and a null parent. Area PUT
takes only `name`, `areaCode` and `sourceRecordReference`. Correspondence PUT
takes `electoralAreaId` and `evidenceReference` and records the review time.

## Migration and integration

V43 introduces the schema and database guards. V44 copies **all current
operational geography**, not an assumed election register, into an unverified
draft. It copies correspondence suggestions without marking them reviewed.
Neither migration changes existing seat, voter-registration or result foreign
keys. An unexpected malformed operational hierarchy aborts migration rather
than silently dropping nodes. Test the migrations against a database copy before
deployment; V44 copies tens of thousands of rows in a single migration.

The schema migrations are applied by the existing Flyway startup mechanism.
After deployment, an administrator can inspect the draft, establish the source,
correct its metadata/nodes and publish it. There is no automatic publication.

V45 connects this foundation to core voter-register editions, election-register
assignments, contest jurisdictions and result publications, for elections of any date.
See [election records](election-records.md) for their capture and import workflow.
Their references target historical snapshot nodes and enforce matching snapshots.
Existing result records are not reassigned by station-code matching.

The source reference/checksum is stored directly on the snapshot because there
is currently no shared source-document entity. It can later be replaced with a
document FK while preserving that metadata.

## Validation

`mvn test` covers visibility, draft edits, revision orchestration, publication
validation, hierarchy isolation and MapStruct mappings through the services.

`src/test/sql/geography_snapshots.sql` checks the database guards and isolation
from live-area edits. Run it with stop-on-error enabled in a disposable PostgreSQL
database after applying V2, V3, V43 and V44. It rolls its own changes back.

An optional isolated runner uses PGlite (PostgreSQL compiled to WASM):

```text
npm install --prefix target/snapshot-db-test --no-save --package-lock=false @electric-sql/pglite@0.5.8
node src/test/sql/run-geography-snapshots.mjs
```

The runner initializes an in-memory database, applies the actual 67,385-node seed
and both new migrations, and executes the SQL checks. It does not access your
application database. It validates SQL behavior but not concurrent connections
or Payara/JPA deployment; those require a PostgreSQL/Payara integration environment.
