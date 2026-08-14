-- V24: permission and security_role_permission — fine-grained authorization.
-- A permission is the right to perform an action on a resource (e.g.
-- resource 'CANDIDACY', action 'APPROVE'); permission_code is the stable
-- machine identifier used in code checks (e.g. 'CANDIDACY_APPROVE'), while
-- permission_name is the human-readable label. security_role_permission
-- assigns permissions to roles; a user's effective permissions are the union
-- over their roles (user_account -> user_security_role -> security_role ->
-- security_role_permission -> permission).

CREATE TABLE permission (
    permission_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL,
    resource        VARCHAR(100) NOT NULL,
    action          VARCHAR(50)  NOT NULL,
    description     VARCHAR(255),
    UNIQUE (resource, action)
);

CREATE TABLE security_role_permission (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    security_role_id BIGINT NOT NULL REFERENCES security_role (id) ON DELETE CASCADE,
    permission_id    BIGINT NOT NULL REFERENCES permission (permission_id),
    granted_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (security_role_id, permission_id)
);

CREATE INDEX idx_security_role_permission_role ON security_role_permission (security_role_id);
CREATE INDEX idx_security_role_permission_permission ON security_role_permission (permission_id);
