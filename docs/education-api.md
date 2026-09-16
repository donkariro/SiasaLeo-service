# Education API

All paths are relative to the application's `/api` base. The domain uses the
existing V35–V39 tables; no additional migration is required.

## Selection lists

| Method | Path | Result |
| --- | --- | --- |
| GET | `/education/levels` | All levels in academic rank order; unranked professional qualifications appear last |
| GET | `/education/institution-types` | All institution types in name order |
| GET | `/education/fields-of-study` | All fields in name order, with nullable `parentId` for hierarchical selection |
| GET | `/education/institutions` | Institutions in name order |
| GET | `/education/institutions/{id}` | One institution, or 404 |

Institution listing accepts optional `typeId` and `search` parameters. Search
matches institution names without case sensitivity. Pagination uses `page=0`
and `size=100` by default, with a maximum size of 500. Fetch successive pages
until a page contains fewer than `size` entries. Negative pages return 400.

An administrator can register an institution with `POST /education/institutions`:

```json
{
  "name": "Example University",
  "registrationNumber": "ACC-001",
  "institutionTypeId": 4
}
```

Use IDs returned by the lookup APIs. `registrationNumber` is optional.
Creation returns 201 with the institution and its `Location` header. Institution
creation also creates its organization and official name through the existing
joined organization model. Institutions are not seeded by V36, so a new database
initially returns an empty institution list.

## Personal education records

Authenticated users with `USER` or `ADMINISTRATOR` roles manage their own records
at `/education/me/records`. The account must already have a linked person to
write education records. An unlinked account's list is empty.

Administrators can manage a specified person's records at
`/education/persons/{personId}/records` using the same operations:

| Method | Relative path | Result |
| --- | --- | --- |
| GET | (collection) | Study history, latest start date first; unknown start dates last |
| POST | (collection) | Add a study episode; returns 201 and a `Location` header |
| GET | `/{id}` | Read one study episode |
| PUT | `/{id}` | Replace the episode's education details |
| DELETE | `/{id}` | Remove the episode; returns 204 |

Example POST or PUT body:

```json
{
  "educationLevelId": 5,
  "institutionId": 123,
  "fieldOfStudyId": 45,
  "fromDate": "2018-09-01",
  "uptoDate": "2022-12-15"
}
```

Only `educationLevelId` is required. Institution, field and dates may be null.
A null `uptoDate` means ongoing study. An end date before the start date, or an
unknown lookup reference, returns 400. PUT clears optional values omitted from
the body. Record responses include lookup IDs and names for display.

Record IDs are scoped to the addressed person: a missing record or a record
belonging to another person returns 404. The self-service API derives the person
from the authenticated account and does not accept a person ID in the body.
These are recorded qualifications; they do not verify credentials or determine
candidate eligibility.
