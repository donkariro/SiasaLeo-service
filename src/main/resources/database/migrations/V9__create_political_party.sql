-- V9: political_party — subtype of organization (joined inheritance, same
-- pattern as V1): reuses the organization id as PK + FK. Name lives on
-- organization; the ORPP certificate serial number maps to
-- organization.registration_number. Columns mirror the ORPP register of
-- fully registered political parties.

CREATE TABLE political_party (
    id                   BIGINT PRIMARY KEY REFERENCES organization (id) ON DELETE CASCADE,
    abbreviation         VARCHAR(50),
    registered_on        DATE,
    symbol               VARCHAR(255),
    colors               VARCHAR(255),
    postal_address       VARCHAR(255),
    head_office_location VARCHAR(255),
    slogan               VARCHAR(255),
    changes              VARCHAR(255),
    symbol_image_file    VARCHAR(255)
);
