-- V23: security_role and user_security_role — authorization side of the
-- security domain. security_role is the lookup of grantable roles (mapped to
-- groups by the Jakarta Security identity store); user_security_role is the
-- junction assigning roles to accounts. Seeded with a bootstrap pair of
-- roles; further roles are added as the application grows.

CREATE TABLE security_role (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_name   VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO security_role (role_name, description)
VALUES ('ADMINISTRATOR', 'Full administrative access to the system'),
       ('USER', 'Standard authenticated user access');

CREATE TABLE user_security_role (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_account_id  BIGINT NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    security_role_id BIGINT NOT NULL REFERENCES security_role (id),
    granted_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_account_id, security_role_id)
);

CREATE INDEX idx_user_security_role_account ON user_security_role (user_account_id);
CREATE INDEX idx_user_security_role_role ON user_security_role (security_role_id);
