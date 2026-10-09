# Digital assets

The digital asset domain manages documents, images, audio, video, and other media
across SiasaLeo. It owns asset identity, versions, representations, metadata and
access. Consuming domains own associations and their meaning. Governance evidence
is one use of an asset.

Status: metadata management is implemented through V58. Actual uploads, storage,
hash verification, previews and downloads are explicitly deferred. The
[file storage proposal](digital-asset-file-storage.md) is the single reference
for that later implementation.

## Model and responsibilities

| Concept | Meaning |
| --- | --- |
| DigitalAsset | Editable library description, visibility, owner, archive state and stable ID |
| DigitalAssetVersion | One edition, with optional source provenance and immutable descriptive snapshots when sealed |
| StoredFile | Registered representation metadata; existence and hashes are not yet verified |
| Asset owner | An account or a party (person/organization), separate from the creating account |
| Asset grant | An explicit READ or MANAGE permission for another account |
| Party media selection | A consumer-owned association selecting the asset used for a purpose; follows its current sealed version |
| Person asset attachment | A consumer association pinned to an exact sealed version |

Representations remain ORIGINAL, PREVIEW, THUMBNAIL and TRANSCRIPT. Asset types
include the original codes plus IMAGE, AUDIO, VIDEO, DOCUMENT, LOGO, PORTRAIT,
IDENTITY_DOCUMENT and CERTIFICATE. Asset type describes a category, media type
describes a file format, and an association's purpose describes its use.

The asset package has no Java dependency on governance. Governance's
`EvidencePolicy` requires an active PUBLIC asset, a sealed version and nonblank
provenance. Database guards enforce that policy on evidence association tables.
General media attachments require a sealed version but do not require provenance.

## Ownership and permissions

New assets default to PRIVATE. Sealing freezes content; it never changes
visibility. PUBLIC exposes sealed versions only. Explicit grants allow private
sharing without a third visibility state.

| Actor | Library metadata / sealed version | Draft version | Edit description, version, seal, archive | Visibility, grants, ownership |
| --- | --- | --- | --- | --- |
| Anonymous / unrelated account | PUBLIC assets with a sealed version | No | No | No |
| READ grantee | Granted assets | No | No | No |
| MANAGE grantee | Granted assets | Yes | Yes | No |
| Owning account | Owned assets | Yes | Yes | Yes |
| Authorized manager of owning party | Party-owned assets | Yes | Yes | Yes |

The account linked to a person may manage that person's media. Organizational
authority comes from explicit `party_media_manager` assignments, provisioned by
an ADMINISTRATOR. Ordinary organization or political-party membership does not
grant media authority. Administrators do not automatically read private assets
or take ownership of an already owned asset.

Creation with `owner_party_id` requires authority over that party. Otherwise
ownership goes to the authenticated account. `created_by_account_id` is always
assigned by the server and remains unchanged after ownership transfer. Transfers
clear existing asset grants. Transferring to a party requires authority over that
party; transferring to an account requires an active recipient.

Legacy assets with no creator remain unassigned. An ADMINISTRATOR can explicitly
assign an owner through the ownership endpoint. No arbitrary owner is invented
during migration.

Read endpoints check visibility on every request. Authenticated reads include
the caller's grants. Media responses use `Cache-Control: private, no-store`.
Grant lists and audit history are never exposed publicly.

## Asset API

All routes below are relative to `/api/digital-assets`. JSON uses snake_case.
Writes require USER or ADMINISTRATOR plus the domain permissions above.

| Method and route | Behavior / request |
| --- | --- |
| GET /types | Asset type catalogue, including retired types for historical display |
| POST / | Create: title, asset_type; optional description, visibility, owner_party_id |
| GET / | Accessible library; q (title substring), type, include_archived, page, size |
| GET /{id} | Accessible asset metadata |
| PUT /{id} | Replace title, description, asset_type; require current row_version |
| GET /{id}/versions | Accessible version history; drafts only for managers/owners |
| POST /{id}/versions | New edition: optional source_url, provenance, author_party_id, publisher_party_id, source_published_on |
| GET /{id}/current | Latest sealed version and its file metadata |
| GET /versions/{id} | Exact accessible version and its file metadata |
| POST /versions/{id}/seal | Finalize; needs a source URL or registered file metadata |
| POST /versions/{id}/files | Existing metadata-only registration; no byte transfer or verification |
| PUT /{id}/visibility | visibility: PRIVATE or PUBLIC; owner authority required |
| GET /{id}/grants | Owner-only account grants |
| PUT /{id}/grants/{accountId} | permission: READ or MANAGE; active recipient required |
| DELETE /{id}/grants/{accountId} | Revoke explicit grant |
| PUT /{id}/owner | Exactly one of owner_account_id or owner_party_id |
| POST /{id}/archive | Remove from ordinary library selection; preserve existing references |
| POST /{id}/restore | Restore to ordinary selection |
| GET /{id}/audit | Manager/owner metadata and grant history; page, size |
| DELETE /versions/{id} | Remove an unsealed version and its registered file metadata |
| DELETE /{id} | Owner-only deletion of an empty, unreferenced asset |

`GET /me/versions/{id}` and `GET /me/{id}/current` remain authenticated aliases.
`GET /api/governance/catalogs/asset-types` remains a compatibility alias for
the asset catalogue.

Pagination is zero-based, defaults to page=0 and size=25, and permits size 1..100.
Public listings exclude archived assets. Authenticated callers may request
include_archived=true for assets they can read. Exact existing references remain
readable after archive; new versions and attachments require restoration first.

An unknown or retired asset type is rejected for creation and metadata edits.
Stale metadata row_version returns a conflict. Ownership/management failures
return 403; hidden reads return 404. Database rules reject hiding an asset while
governance evidence references it. Detaching a media selection does not delete
its asset or files.

## Historical integrity and auditing

Sealed versions store snapshot_title, snapshot_description and snapshot_asset_type.
Historical citations should display these fields, not the current library label.
V57 backfills snapshots on existing sealed versions before allowing label edits.

Updates cannot change the asset ID, original creator or creation timestamp.
Database audit entries capture metadata, ownership and grant changes, including
the acting account for service writes. Database/import writes without an actor
retain a null actor rather than inventing one. Audit entries are immutable.

Version allocation locks the asset while reading/incrementing the version
number. Attachment and evidence guards lock the asset while checking archive
and visibility state. Sealed versions and their file metadata remain immutable.

## Person and organization media

Routes are relative to `/api/parties`; a party is a person or organization.

| Method and route | Behavior |
| --- | --- |
| GET /{partyId}/media/{purpose} | Resolve selected asset's latest sealed version, respecting current asset access |
| PUT /{partyId}/media/{purpose} | Select using asset_id; require party-management authority and access to the active asset |
| DELETE /{partyId}/media/{purpose} | Remove selection; preserve prior selection in history |
| PUT /{partyId}/media-managers/{accountId} | ADMINISTRATOR assigns explicit media authority |
| DELETE /{partyId}/media-managers/{accountId} | ADMINISTRATOR revokes media authority |
| GET /{personId}/attachments | List readable pinned person attachments; page, size |
| PUT /{personId}/attachments/{purpose} | Attach asset_version_id and optional caption; person only |
| DELETE /{personId}/attachments/{purpose}/{versionId} | Remove one person attachment |

Use PORTRAIT for a person's current photo and LOGO for an organization's current
logo. Purpose is a nonblank string up to 100 characters, owned by the consumer.
Selecting private media never publishes it: public profile reads receive 404
until its owner explicitly makes it public. Replacement preserves previous
asset selections in party_media_selection_history. Each old asset retains its
own version history.

Example metadata-only portrait workflow:

1. Create a PRIVATE PORTRAIT asset owned by the caller or their person.
2. Create a version with an external source URL and seal it.
3. PUT `/api/parties/{personId}/media/PORTRAIT` with `{"asset_id": 42}`.
4. An authenticated owner can resolve it. Public resolution is hidden.
5. Explicitly make asset 42 PUBLIC to show it publicly.
6. Seal a new version to update the selected asset's current edition, or select
   another asset to replace it.

## Political-party legacy media

V58 adds image_asset_id on politicalparty_symbol and photo_asset_id on
politicalparty_official. Their DTOs expose these references. Existing string
columns remain for provenance; new string writes are rejected.

ADMINISTRATOR may select an accessible active PUBLIC asset using:

- PUT `/api/party-media/symbols/{symbolRecordId}` with asset_id.
- PUT `/api/party-media/officials/{officialRecordId}` with asset_id.

Matching GET routes resolve the current sealed version and recheck public access.
A later visibility change can hide this media; it never bypasses access policy.
These routes use the symbol/official record ID, not the political-party ID.

Migration preserves legacy source values in legacy_media_reconciliation:

- Valid HTTP/HTTPS references become sealed external-reference versions.
- Opaque filenames become draft versions marked NEEDS_FILE_IMPORT.
- No bytes, sizes or hashes are invented.
- Imported assets are owned by the relevant political party.
- IDs on pre-existing assets, versions and associations are unchanged.

Actual file import and object reconciliation remain part of the deferred storage
work. A legacy filename alone cannot be served as a verified file.

## Verification and deferred work

Java tests cover ownership, sharing, private defaults, draft access, finalization,
version allocation, transfer, consumer selection, and evidence eligibility.
The SQL suite applies every migration and checks legacy backfill, the actual
library access query, snapshots, audit history, public-evidence protections,
archiving, draft deletion and selection history.

Run `mvn test` for Java and SQL suites, or `node src/test/sql/run-digital-assets.mjs`
for the asset database checks. Tests use in-process PostgreSQL (PGlite); they do
not establish deployed Payara behavior or multi-connection concurrency.

Deferred at the user's request: byte uploads, storage provider configuration,
hash verification, downloads, storage cleanup, automatic representations and
external-URL capture. The existing file-registration API remains metadata-only;
it must be restricted/replaced when the verified upload workflow is introduced.
