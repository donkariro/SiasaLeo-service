-- V22: user_account — security domain. A login identity, optionally linked to
-- a person in the party model (one account per person). Registration is with
-- exactly one contact point — email or phone — which must be verified (see
-- verification_code, V25) before the account becomes ACTIVE. Values are
-- stored normalized (emails lower-cased, phones in E.164). username is not
-- collected at sign-up; users pick one later in a profile step, hence
-- nullable. password_hash stores only a salted hash (e.g. Jakarta Security
-- Pbkdf2PasswordHash output), never a plaintext password.

CREATE TABLE user_account (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    person_id             BIGINT UNIQUE REFERENCES person (id),
    username              VARCHAR(100),
    email                 VARCHAR(255),
    phone                 VARCHAR(20),
    password_hash         VARCHAR(255) NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING_ACTIVATION'
                              CHECK (status IN ('PENDING_ACTIVATION', 'ACTIVE',
                                                'LOCKED', 'DISABLED')),
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until          TIMESTAMP WITH TIME ZONE,
    last_login_at         TIMESTAMP WITH TIME ZONE,
    password_changed_at   TIMESTAMP WITH TIME ZONE,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE,
    -- Exactly one contact point per account.
    CHECK ((email IS NULL) <> (phone IS NULL))
);

CREATE UNIQUE INDEX idx_user_account_username ON user_account (LOWER(username));
CREATE UNIQUE INDEX idx_user_account_email ON user_account (LOWER(email));
CREATE UNIQUE INDEX idx_user_account_phone ON user_account (phone);
