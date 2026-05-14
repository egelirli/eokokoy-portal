-- ============================================================
-- V12: Oylama & Anket (SPEC-13)
-- ============================================================

CREATE TABLE polls (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    type           VARCHAR(20)  NOT NULL
        CONSTRAINT polls_type_check CHECK (type IN ('vote','survey')),
    title          VARCHAR(255) NOT NULL,
    description    TEXT,
    status         VARCHAR(20)  NOT NULL DEFAULT 'draft'
        CONSTRAINT polls_status_check CHECK (status IN ('draft','active','closed','cancelled')),
    is_anonymous   BOOLEAN      NOT NULL DEFAULT FALSE,
    eligible_roles TEXT[]       NOT NULL,
    starts_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at        TIMESTAMP WITH TIME ZONE,
    created_by     UUID         NOT NULL REFERENCES users(id),
    closed_by      UUID         REFERENCES users(id),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_polls_status    ON polls(status);
CREATE INDEX idx_polls_starts_at ON polls(starts_at);
CREATE INDEX idx_polls_ends_at   ON polls(ends_at);
CREATE INDEX idx_polls_created_by ON polls(created_by);

CREATE TABLE poll_questions (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id        UUID        NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    question_text  TEXT        NOT NULL,
    question_type  VARCHAR(20) NOT NULL
        CONSTRAINT poll_questions_type_check
            CHECK (question_type IN ('yes_no','single_choice','multiple_choice','text')),
    is_required    BOOLEAN     NOT NULL DEFAULT TRUE,
    question_order INTEGER     NOT NULL DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_poll_questions_poll_id ON poll_questions(poll_id);

CREATE TABLE poll_options (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id  UUID         NOT NULL REFERENCES poll_questions(id) ON DELETE CASCADE,
    option_text  VARCHAR(500) NOT NULL,
    option_order INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_poll_options_question_id ON poll_options(question_id);

CREATE TABLE poll_responses (
    id           UUID  PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id      UUID  NOT NULL REFERENCES polls(id),
    question_id  UUID  NOT NULL REFERENCES poll_questions(id),
    user_id      UUID  NOT NULL REFERENCES users(id),
    option_id    UUID  REFERENCES poll_options(id),
    text_answer  TEXT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_poll_responses_poll_id      ON poll_responses(poll_id);
CREATE INDEX idx_poll_responses_user_id      ON poll_responses(user_id);
CREATE INDEX idx_poll_responses_question_id  ON poll_responses(question_id);
CREATE UNIQUE INDEX idx_poll_responses_unique_text
    ON poll_responses(poll_id, question_id, user_id)
    WHERE option_id IS NULL;
