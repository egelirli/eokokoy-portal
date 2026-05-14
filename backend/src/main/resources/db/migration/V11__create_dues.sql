-- ============================================================
-- V11: Aidat Takibi (SPEC-12)
-- ============================================================

CREATE TABLE due_imports (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name     VARCHAR(255)  NOT NULL,
    imported_by   UUID          NOT NULL REFERENCES users(id),
    total_rows    INTEGER       NOT NULL DEFAULT 0,
    success_rows  INTEGER       NOT NULL DEFAULT 0,
    error_rows    INTEGER       NOT NULL DEFAULT 0,
    error_details JSONB,
    status        VARCHAR(20)   NOT NULL DEFAULT 'processing'
        CONSTRAINT due_imports_status_check CHECK (status IN ('processing','completed','failed')),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_due_imports_imported_by ON due_imports(imported_by);

CREATE TABLE dues (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id     UUID           NOT NULL REFERENCES properties(id),
    user_id         UUID           REFERENCES users(id),
    period_year     INTEGER        NOT NULL,
    period_month    INTEGER        CONSTRAINT dues_month_check CHECK (period_month BETWEEN 1 AND 12),
    amount          DECIMAL(10,2)  NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'unpaid'
        CONSTRAINT dues_status_check CHECK (status IN ('unpaid','paid','partially_paid','cancelled')),
    paid_amount     DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    due_date        DATE           NOT NULL,
    paid_at         DATE,
    description     VARCHAR(255),
    import_batch_id UUID           REFERENCES due_imports(id),
    created_by      UUID           NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dues_property_id ON dues(property_id);
CREATE INDEX idx_dues_user_id ON dues(user_id);
CREATE INDEX idx_dues_status ON dues(status);
CREATE INDEX idx_dues_due_date ON dues(due_date);
CREATE UNIQUE INDEX idx_dues_upsert ON dues(property_id, period_year, period_month)
    WHERE period_month IS NOT NULL;
CREATE UNIQUE INDEX idx_dues_upsert_yearly ON dues(property_id, period_year)
    WHERE period_month IS NULL;

CREATE TABLE due_payments (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    due_id         UUID          NOT NULL REFERENCES dues(id) ON DELETE CASCADE,
    amount         DECIMAL(10,2) NOT NULL,
    payment_date   DATE          NOT NULL,
    payment_method VARCHAR(100),
    reference_no   VARCHAR(100),
    notes          TEXT,
    recorded_by    UUID          NOT NULL REFERENCES users(id),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_due_payments_due_id ON due_payments(due_id);
