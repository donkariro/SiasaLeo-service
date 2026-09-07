-- V38: field_of_study — what a qualification was earned in, the third part of
-- the education vocabulary alongside educational_institution_type (V35) and
-- education_level (V37). A qualification names one field; the level and the
-- institution say how far it went and where.
--
-- Fields are hierarchical, so parent_field_of_study is a nullable self
-- reference: a NULL parent marks a broad field (the roots of the tree), and a
-- child names a specialization within it. The column takes no ON DELETE
-- action, so a broad field cannot be deleted while specializations still hang
-- off it — the tree has to be pruned deliberately rather than by cascade.
--
-- The seed is two levels deep, but nothing in the schema fixes the depth: a
-- narrower specialization can be added later by pointing it at a child row.
--
-- The broad fields follow ISCED-F 2013, the classification the Commission for
-- University Education and KNBS report against, so counts drawn from this
-- table line up with published education statistics. The specializations under
-- them are the ones that actually appear on Kenyan transcripts rather than the
-- full ISCED detail. Names are unique across the whole tree, not just within a
-- parent, so a field can be referenced by name without knowing where it sits;
-- where a subject spans two broad fields it is placed once, under the one it
-- is usually taught in.
--
-- Broad fields carry a description because their scope is not obvious from the
-- name; specializations are left NULL, since restating a name as prose adds
-- nothing.

CREATE TABLE field_of_study (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    field_name            VARCHAR(100) NOT NULL UNIQUE,
    parent_field_of_study BIGINT REFERENCES field_of_study (id),
    description           VARCHAR(255),
    -- Blocks the one-row cycle. Longer cycles are not reachable through the
    -- seed and would need a trigger to prevent outright.
    CONSTRAINT chk_field_of_study_not_own_parent
        CHECK (parent_field_of_study IS NULL OR parent_field_of_study <> id)
);

CREATE INDEX idx_field_of_study_parent ON field_of_study (parent_field_of_study);

-- The broad fields (ISCED-F 2013), inserted first so the specializations below
-- can resolve their parent by name.
INSERT INTO field_of_study (field_name, description)
VALUES ('EDUCATION', 'Teaching, pedagogy and the administration of schooling'),
       ('ARTS_AND_HUMANITIES', 'Languages, literature, history, philosophy and the creative arts'),
       ('SOCIAL_SCIENCES', 'The study of society, government, economies and human behaviour'),
       ('BUSINESS_AND_ADMINISTRATION', 'Commerce, accounting, management and the administration of public affairs'),
       ('LAW', 'The study and practice of law'),
       ('NATURAL_SCIENCES', 'The physical and life sciences, mathematics and statistics'),
       ('INFORMATION_AND_COMMUNICATION_TECHNOLOGY', 'Computing, software, networks and information systems'),
       ('ENGINEERING_AND_CONSTRUCTION', 'Engineering, manufacturing, architecture and the built environment'),
       ('AGRICULTURE_AND_VETERINARY', 'Crop and animal production, forestry, fisheries and veterinary medicine'),
       ('HEALTH_AND_WELFARE', 'Medicine, the allied health professions and social welfare'),
       ('SERVICES', 'Hospitality, tourism, transport, security and sport');

-- Specializations, each resolving its parent from the rows above. A parent
-- named here that did not exist would drop its children silently, so the
-- resulting counts are asserted at the end of this migration.
INSERT INTO field_of_study (field_name, parent_field_of_study)
SELECT c.field_name, p.id
FROM (VALUES
    ('TEACHER_TRAINING', 'EDUCATION'),
    ('EARLY_CHILDHOOD_EDUCATION', 'EDUCATION'),
    ('SPECIAL_NEEDS_EDUCATION', 'EDUCATION'),
    ('CURRICULUM_STUDIES', 'EDUCATION'),
    ('EDUCATIONAL_ADMINISTRATION_AND_PLANNING', 'EDUCATION'),

    ('LITERATURE', 'ARTS_AND_HUMANITIES'),
    ('LINGUISTICS_AND_LANGUAGES', 'ARTS_AND_HUMANITIES'),
    ('HISTORY', 'ARTS_AND_HUMANITIES'),
    ('PHILOSOPHY', 'ARTS_AND_HUMANITIES'),
    ('THEOLOGY_AND_RELIGIOUS_STUDIES', 'ARTS_AND_HUMANITIES'),
    ('FINE_ARTS_AND_DESIGN', 'ARTS_AND_HUMANITIES'),
    ('MUSIC_AND_PERFORMING_ARTS', 'ARTS_AND_HUMANITIES'),

    ('POLITICAL_SCIENCE', 'SOCIAL_SCIENCES'),
    ('INTERNATIONAL_RELATIONS', 'SOCIAL_SCIENCES'),
    ('PUBLIC_POLICY', 'SOCIAL_SCIENCES'),
    ('SOCIOLOGY', 'SOCIAL_SCIENCES'),
    ('ANTHROPOLOGY', 'SOCIAL_SCIENCES'),
    ('PSYCHOLOGY', 'SOCIAL_SCIENCES'),
    ('ECONOMICS', 'SOCIAL_SCIENCES'),
    ('GEOGRAPHY', 'SOCIAL_SCIENCES'),
    ('DEVELOPMENT_STUDIES', 'SOCIAL_SCIENCES'),
    ('PEACE_AND_CONFLICT_STUDIES', 'SOCIAL_SCIENCES'),
    ('GENDER_STUDIES', 'SOCIAL_SCIENCES'),
    ('JOURNALISM_AND_MEDIA_STUDIES', 'SOCIAL_SCIENCES'),
    ('COMMUNICATION_STUDIES', 'SOCIAL_SCIENCES'),

    ('ACCOUNTING', 'BUSINESS_AND_ADMINISTRATION'),
    ('FINANCE', 'BUSINESS_AND_ADMINISTRATION'),
    ('BANKING_AND_INSURANCE', 'BUSINESS_AND_ADMINISTRATION'),
    ('BUSINESS_MANAGEMENT', 'BUSINESS_AND_ADMINISTRATION'),
    ('MARKETING', 'BUSINESS_AND_ADMINISTRATION'),
    ('HUMAN_RESOURCE_MANAGEMENT', 'BUSINESS_AND_ADMINISTRATION'),
    ('PROJECT_MANAGEMENT', 'BUSINESS_AND_ADMINISTRATION'),
    ('PROCUREMENT_AND_SUPPLY_CHAIN_MANAGEMENT', 'BUSINESS_AND_ADMINISTRATION'),
    ('ENTREPRENEURSHIP', 'BUSINESS_AND_ADMINISTRATION'),
    ('COOPERATIVE_MANAGEMENT', 'BUSINESS_AND_ADMINISTRATION'),
    ('PUBLIC_ADMINISTRATION', 'BUSINESS_AND_ADMINISTRATION'),

    ('CONSTITUTIONAL_LAW', 'LAW'),
    ('CRIMINAL_LAW', 'LAW'),
    ('COMMERCIAL_LAW', 'LAW'),
    ('INTERNATIONAL_LAW', 'LAW'),
    ('HUMAN_RIGHTS_LAW', 'LAW'),

    ('BIOLOGY', 'NATURAL_SCIENCES'),
    ('BIOCHEMISTRY', 'NATURAL_SCIENCES'),
    ('CHEMISTRY', 'NATURAL_SCIENCES'),
    ('PHYSICS', 'NATURAL_SCIENCES'),
    ('MATHEMATICS', 'NATURAL_SCIENCES'),
    ('STATISTICS', 'NATURAL_SCIENCES'),
    ('GEOLOGY', 'NATURAL_SCIENCES'),
    ('ENVIRONMENTAL_SCIENCE', 'NATURAL_SCIENCES'),

    ('COMPUTER_SCIENCE', 'INFORMATION_AND_COMMUNICATION_TECHNOLOGY'),
    ('INFORMATION_TECHNOLOGY', 'INFORMATION_AND_COMMUNICATION_TECHNOLOGY'),
    ('SOFTWARE_ENGINEERING', 'INFORMATION_AND_COMMUNICATION_TECHNOLOGY'),
    ('INFORMATION_SYSTEMS', 'INFORMATION_AND_COMMUNICATION_TECHNOLOGY'),
    ('DATA_SCIENCE', 'INFORMATION_AND_COMMUNICATION_TECHNOLOGY'),
    ('CYBERSECURITY', 'INFORMATION_AND_COMMUNICATION_TECHNOLOGY'),
    ('TELECOMMUNICATIONS', 'INFORMATION_AND_COMMUNICATION_TECHNOLOGY'),

    ('CIVIL_ENGINEERING', 'ENGINEERING_AND_CONSTRUCTION'),
    ('ELECTRICAL_AND_ELECTRONIC_ENGINEERING', 'ENGINEERING_AND_CONSTRUCTION'),
    ('MECHANICAL_ENGINEERING', 'ENGINEERING_AND_CONSTRUCTION'),
    ('CHEMICAL_ENGINEERING', 'ENGINEERING_AND_CONSTRUCTION'),
    ('AGRICULTURAL_ENGINEERING', 'ENGINEERING_AND_CONSTRUCTION'),
    ('ARCHITECTURE', 'ENGINEERING_AND_CONSTRUCTION'),
    ('QUANTITY_SURVEYING', 'ENGINEERING_AND_CONSTRUCTION'),
    ('LAND_SURVEYING', 'ENGINEERING_AND_CONSTRUCTION'),
    ('URBAN_AND_REGIONAL_PLANNING', 'ENGINEERING_AND_CONSTRUCTION'),
    ('BUILDING_AND_CONSTRUCTION_TECHNOLOGY', 'ENGINEERING_AND_CONSTRUCTION'),

    ('CROP_SCIENCE', 'AGRICULTURE_AND_VETERINARY'),
    ('ANIMAL_SCIENCE', 'AGRICULTURE_AND_VETERINARY'),
    ('HORTICULTURE', 'AGRICULTURE_AND_VETERINARY'),
    ('AGRIBUSINESS_MANAGEMENT', 'AGRICULTURE_AND_VETERINARY'),
    ('RANGE_MANAGEMENT', 'AGRICULTURE_AND_VETERINARY'),
    ('FORESTRY', 'AGRICULTURE_AND_VETERINARY'),
    ('FISHERIES_AND_AQUACULTURE', 'AGRICULTURE_AND_VETERINARY'),
    ('FOOD_SCIENCE_AND_TECHNOLOGY', 'AGRICULTURE_AND_VETERINARY'),
    ('VETERINARY_MEDICINE', 'AGRICULTURE_AND_VETERINARY'),

    ('MEDICINE_AND_SURGERY', 'HEALTH_AND_WELFARE'),
    ('CLINICAL_MEDICINE', 'HEALTH_AND_WELFARE'),
    ('NURSING', 'HEALTH_AND_WELFARE'),
    ('PHARMACY', 'HEALTH_AND_WELFARE'),
    ('DENTISTRY', 'HEALTH_AND_WELFARE'),
    ('PUBLIC_HEALTH', 'HEALTH_AND_WELFARE'),
    ('NUTRITION_AND_DIETETICS', 'HEALTH_AND_WELFARE'),
    ('MEDICAL_LABORATORY_SCIENCE', 'HEALTH_AND_WELFARE'),
    ('COUNSELLING_PSYCHOLOGY', 'HEALTH_AND_WELFARE'),
    ('SOCIAL_WORK', 'HEALTH_AND_WELFARE'),

    ('HOSPITALITY_MANAGEMENT', 'SERVICES'),
    ('TOURISM_MANAGEMENT', 'SERVICES'),
    ('TRANSPORT_AND_LOGISTICS', 'SERVICES'),
    ('SECURITY_AND_STRATEGIC_STUDIES', 'SERVICES'),
    ('MILITARY_SCIENCE', 'SERVICES'),
    ('SPORTS_SCIENCE', 'SERVICES')
) AS c(field_name, parent_name)
JOIN field_of_study p ON p.field_name = c.parent_name;

-- Fail the migration rather than seed a half-built tree: a mistyped parent
-- name above would silently drop its children on the join.
DO $$
DECLARE
    v_roots    BIGINT;
    v_children BIGINT;
BEGIN
    SELECT count(*) FILTER (WHERE parent_field_of_study IS NULL),
           count(*) FILTER (WHERE parent_field_of_study IS NOT NULL)
    INTO v_roots, v_children
    FROM field_of_study;

    IF v_roots <> 11 OR v_children <> 91 THEN
        RAISE EXCEPTION
            'field_of_study seed incomplete: expected 11 broad fields and 91 specializations, found % and %',
            v_roots, v_children;
    END IF;
END $$;
