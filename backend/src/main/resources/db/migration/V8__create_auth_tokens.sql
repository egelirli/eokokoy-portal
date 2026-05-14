-- ============================================================
-- V8: refresh_tokens ve password_reset_tokens tabloları
-- ============================================================

CREATE TABLE refresh_tokens (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id),
    token_hash   VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at   TIMESTAMP WITH TIME ZONE,
    device_info  VARCHAR(255),
    ip_address   VARCHAR(45)
);

CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);

CREATE TABLE password_reset_tokens (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id),
    token_hash   VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_used      BOOLEAN      NOT NULL DEFAULT false,
    ip_address   VARCHAR(45)
);

CREATE INDEX idx_pwd_reset_tokens_user_id    ON password_reset_tokens(user_id);
CREATE INDEX idx_pwd_reset_tokens_token_hash ON password_reset_tokens(token_hash);
