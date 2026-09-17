# Election domain API

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
accepted to support historical records. Every new event starts at `SCHEDULED`.
The response is `201 Created`, with a Location header and an event DTO containing
`id`, `electionCycleId`, `electionDate`, and nested `type` and `status` catalog DTOs.

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

The event and seat must exist. Description is optional, limited to 255 characters,
trimmed, and normalized to null when blank. Only one contest is allowed for an
event/seat pair; the database constraint also protects concurrent inserts.
Historical contests can be entered regardless of event status.
The response is `201 Created`, with a Location header and
`{id, electionEventId, seatId, description}`. The returned contest ID can be used
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
