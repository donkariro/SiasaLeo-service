# Office selection API

`GET /api/offices` returns all offices in alphabetical name order, without
pagination or required filters. Each option contains `id`, `name`,
`abbreviation`, nullable `description`, and `level` (`National` or `County`).
Use `id` as the selected value and `name` as the display label.

The list includes deputy offices and uses the existing register seeded by V6.
An empty register returns `[]`. No additional migration is required.

`GET /api/offices/{id}` returns an individual office, or 404 if it does not exist.
Both endpoints are public reference-data reads and are included in the generated
OpenAPI specification.
