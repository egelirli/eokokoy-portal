-- ============================================================
-- V9: Duyurular (SPEC-05)
-- ============================================================

CREATE TABLE announcements (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    title        VARCHAR(255) NOT NULL,
    body         TEXT         NOT NULL,
    priority     VARCHAR(20)  NOT NULL DEFAULT 'normal'
        CONSTRAINT announcements_priority_check CHECK (priority IN ('normal','important','urgent')),
    status       VARCHAR(20)  NOT NULL DEFAULT 'draft'
        CONSTRAINT announcements_status_check CHECK (status IN ('draft','published','archived')),
    is_public    BOOLEAN      NOT NULL DEFAULT false,
    target_type  VARCHAR(20)  NOT NULL DEFAULT 'all'
        CONSTRAINT announcements_target_type_check CHECK (target_type IN ('all','role_based','property_based')),
    published_at TIMESTAMP WITH TIME ZONE,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    expires_at   TIMESTAMP WITH TIME ZONE,
    archived_at  TIMESTAMP WITH TIME ZONE,
    created_by   UUID         NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE announcement_targets (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id  UUID        NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    target_entity_type VARCHAR(20) NOT NULL
        CONSTRAINT announcement_targets_type_check CHECK (target_entity_type IN ('property','role')),
    target_id        UUID        NOT NULL
);

CREATE INDEX idx_announcement_targets_announcement_id ON announcement_targets(announcement_id);

CREATE TABLE announcement_attachments (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    announcement_id UUID        NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    file_url        VARCHAR(512) NOT NULL,
    file_name       VARCHAR(255),
    file_type       VARCHAR(20)  NOT NULL
        CONSTRAINT announcement_attachments_type_check CHECK (file_type IN ('image','document')),
    file_size       INTEGER,
    display_order   INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_announcement_attachments_announcement_id ON announcement_attachments(announcement_id);

CREATE TABLE announcement_reads (
    announcement_id UUID NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    read_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (announcement_id, user_id)
);

CREATE INDEX idx_announcement_reads_user_id ON announcement_reads(user_id);
