-- V1: Base party model with person and organization subtypes (joined inheritance).
-- The id is generated once in party; subtype tables reuse it as PK + FK.

CREATE TABLE party (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    party_type  VARCHAR(20) NOT NULL CHECK (party_type IN ('PERSON', 'ORGANIZATION')),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE TABLE person (
    id            BIGINT PRIMARY KEY REFERENCES party (id) ON DELETE CASCADE,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    date_of_birth DATE
);

CREATE TABLE organization (
    id                  BIGINT PRIMARY KEY REFERENCES party (id) ON DELETE CASCADE,
    name                VARCHAR(255) NOT NULL,
    registration_number VARCHAR(100)
);
