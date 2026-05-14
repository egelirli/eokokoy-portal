-- ============================================================
-- V6: invitations ve audit_logs tabloları
-- ============================================================

CREATE TABLE invitations (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    role_id     UUID         NOT NULL REFERENCES roles(id),
    property_id UUID         REFERENCES properties(id),
    created_by  UUID         NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used     BOOLEAN      NOT NULL DEFAULT false,
    used_at     TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_invitations_token ON invitations(token);
CREATE INDEX idx_invitations_email ON invitations(email);

CREATE TABLE audit_logs (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID         REFERENCES users(id),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   UUID,
    details     TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_actor_id   ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity     ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
