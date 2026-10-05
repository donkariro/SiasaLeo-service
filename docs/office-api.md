# Office selection API

`GET /api/offices` returns all offices in alphabetical name order, without
pagination or required filters. Each option contains `id`, `name`,
`abbreviation`, nullable `description`, `level`, and `countryCode` (currently `KE`).
Use `id` as the selected value and `name` as the display label. V50 scopes name
and abbreviation uniqueness to country; V54 permits country-specific level labels.

The list includes deputy offices and uses the existing register seeded by V6.
An empty register returns `[]`. Apply V50-V54 before deploying the updated API.

`GET /api/offices/{id}` returns an individual office, or 404 if it does not exist.
Both endpoints are public reference-data reads and are included in the generated
OpenAPI specification.
