-- V39: person_education — the education domain's fact table, tying a person to
-- one episode of study: where it happened (educational_institution, V36), how
-- far it went (education_level, V37) and in what (field_of_study, V38). A
-- person has one row per qualification, so the aspirant who holds a diploma
-- and a later degree has two.
--
-- from_date and upto_date live here rather than in a table of their own. The
-- separate-table pattern used by organization_name (V17) and
-- politicalparty_symbol (V20) normalizes an attribute that changes over the
-- life of an entity that outlives it — an organization outlives its names, so
-- each name carries its own validity window. This row does not outlive its own
-- period: it *is* the study episode, so a child table would be joined 1:1 and
-- buy nothing. If a single qualification ever has to record disjoint stretches
-- of study — a deferral, or a transfer between institutions — that becomes a
-- person_education_period child table and these two columns stay as the
-- overall span.
--
-- Only the person and the level are required. The level is what a
-- constitutional or statutory threshold is stated in, so a record without it
-- says nothing; the institution is often unrecorded in the public sources this
-- data is drawn from, and a field of study does not exist at all for PRIMARY
-- and SECONDARY. A NULL upto_date means the study is still under way, matching
-- the reading of upto_date elsewhere in the schema.

CREATE TABLE person_education (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    person_id               BIGINT NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    education_level         BIGINT NOT NULL REFERENCES education_level (id),
    educational_institution BIGINT REFERENCES educational_institution (id),
    field_of_study          BIGINT REFERENCES field_of_study (id),
    from_date               DATE,
    upto_date               DATE,
    CHECK (upto_date IS NULL OR from_date IS NULL OR upto_date >= from_date)
);

-- Reading a person's education is the common query; listing an institution's
-- alumni is the other. education_level and field_of_study are not indexed:
-- both are low-cardinality lookups that the planner is better off reaching
-- through one of these.
CREATE INDEX idx_person_education_person ON person_education (person_id);
CREATE INDEX idx_person_education_institution ON person_education (educational_institution);
