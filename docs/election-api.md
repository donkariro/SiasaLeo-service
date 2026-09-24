# Election domain API

Election capture, official register editions and result publications are described
in [election records](election-records.md). The same APIs and storage serve every
election. Contests can reference an office and snapshot jurisdiction without an
operational `seatId`.

All paths are relative to the application's REST base path. Reads are public;
event creation, status changes, and contest creation require an authenticated
account with the `ADMINISTRATOR` role.

## Reference data and selection

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/elections/cycles` | All seeded cycles, ordered by starting year and ID |
| GET | `/elections/cycles/{id}` | One cycle |
| GET | `/elections/types` | Seeded event types in ID order |
| GET | `/elections/statuses` | Seeded lifecycle statuses in ID order |
| GET | `/seats` | Seats, optionally filtered by `officeId` and `electoralAreaId` |
| GET | `/seats/{id}` | One seat, including its office and electoral area IDs |
| GET | `/election-events` | Events, optionally filtered by `electionCycleId`, `typeId`, and `statusId` |
| GET | `/election-events/{id}` | One event |
| GET | `/contests` | Contests for required `electionEventId`, optionally filtered by `seatId` |
| GET | `/contests/{id}` | One contest |

Seat filters match the exact electoral area; they do not include descendants.
Seat and contest lists are ordered by ID. Events are ordered by election date
descending, then ID descending. These three lists accept zero-based `page`
(default 0) and `size` (default 100, clamped to 1–500), and return JSON arrays.
Negative pages are rejected. Catalog lists are unpaginated.

Cycles, types, statuses, and seats are reference data maintained by migrations.
Cycle status is the stored `past`, `current`, or `future` value, not calculated
from the server clock.

## Create an event

`POST /election-events`

```json
{
  "electionCycleId": 4,
  "electionDate": "2027-08-10",
  "typeId": 1
}
```

Use IDs returned by the catalog endpoints. The date must fall within the cycle's
years: `fromYear` is inclusive and `uptoYear` is exclusive. Dates in the past are
accepted. A new event defaults to `SCHEDULED`. Administrators may supply `statusId`
and a `sourceReference` to capture an event's documented status directly. Optional
`geographySnapshotId` binds published geography at creation; it can also be assigned
with `PUT /election-events/{id}/geography` before recording results or a register
assignment. These options do not depend on whether the date is past or future.
The response is `201 Created`, with a Location header and an event DTO containing
`id`, `electionCycleId`, `electionDate`, nested `type` and `status` catalog DTOs,
`geographySnapshotId`, and `sourceReference`.

## Change event status

`PUT /election-events/{id}/status`

```json
{ "statusId": 2 }
```

Allowed transitions:

| Current status | Next status |
| --- | --- |
| SCHEDULED | ONGOING or CANCELLED |
| ONGOING | COMPLETED or NULLIFIED |
| COMPLETED | NULLIFIED |
| CANCELLED or NULLIFIED | No further transition |

Submitting the existing status succeeds without updating the event. Successful
changes return the event DTO. V42 adds an optimistic version column so concurrent
stale updates cannot overwrite a newer status. Status changes do not create
results, officeholders, or a replacement election event.

## Create a contest

`POST /contests`

```json
{
  "electionEventId": 1,
  "seatId": 123,
  "description": "MP contest"
}
```

The event and supplied seat must exist. Alternatively, supply `officeId` and
`jurisdictionId` from the event's published geography and omit `seatId`. A supplied
seat alongside a jurisdiction requires a reviewed correspondence and the same office.
Existing seat-based contests can be bound with `PUT /contests/{id}/jurisdiction`.
Description is optional, limited to 255 characters,
trimmed, and normalized to null when blank. Only one contest is allowed for an
event/seat pair; the database constraint also protects concurrent inserts.
Historical contests can be entered regardless of event status.
The response is `201 Created`, with a Location header and
`{id, electionEventId, seatId, description, officeId, geographySnapshotId, jurisdictionId}`.
The returned contest ID can be used
with the existing candidacy registration endpoint.

## Errors and deployment

Missing single resources return 404. Invalid request bodies, missing references
or filters, invalid date ranges, illegal transitions, and duplicates detected
before insertion return 400. Writes without authentication return 401; a caller
without the administrator role receives 403. A duplicate detected only by the
database or a concurrent stale status write fails the transaction; no custom
conflict response is currently provided.

Deploy with migration `V42__election_event_version.sql` applied. Existing migration
files are unchanged. The build generates the OpenAPI contract in
`target/generated/openapi.json` and `target/generated/openapi.yaml`.
