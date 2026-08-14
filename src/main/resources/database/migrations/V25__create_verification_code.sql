-- V25: verification_code — one-time codes proving control of the email/phone
-- a user_account (V22) registered with. code_hash is the SHA-256 hex of a
-- 6-digit code; codes are short-lived and attempt-limited (enforced by
-- RegistrationService). consumed_at IS NULL means still usable.

CREATE TABLE verification_code (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_account_id BIGINT NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    code_hash       VARCHAR(64) NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at     TIMESTAMP WITH TIME ZONE,
    attempts        INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_verification_code_user_account_id ON verification_code (user_account_id);
