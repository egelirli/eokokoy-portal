-- ============================================================
-- V10: Ortak Dosya Servisi (SPEC-06)
-- ============================================================

CREATE TABLE files (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    bucket         VARCHAR(50)  NOT NULL,
    object_key     VARCHAR(512) NOT NULL,
    original_name  VARCHAR(255) NOT NULL,
    mime_type      VARCHAR(100) NOT NULL,
    file_size      BIGINT,
    file_type      VARCHAR(20)  NOT NULL
        CONSTRAINT files_file_type_check CHECK (file_type IN ('image','document','other')),
    thumbnail_key  VARCHAR(512),
    compressed_key VARCHAR(512),
    uploaded_by    UUID         NOT NULL REFERENCES users(id),
    uploaded_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_confirmed   BOOLEAN      NOT NULL DEFAULT false,
    is_deleted     BOOLEAN      NOT NULL DEFAULT false,
    deleted_at     TIMESTAMP WITH TIME ZONE,
    deleted_by     UUID         REFERENCES users(id),
    checksum       VARCHAR(64),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_files_uploaded_by   ON files(uploaded_by);
CREATE INDEX idx_files_checksum      ON files(checksum) WHERE checksum IS NOT NULL;
CREATE INDEX idx_files_is_confirmed  ON files(is_confirmed, is_deleted, uploaded_at);
