-- ============================================================
-- V7: properties tablosunu SPEC-03'e göre güncelle + property_users oluştur
-- ============================================================

-- Yeni sütunlar ekle
ALTER TABLE properties
    ADD COLUMN number      INTEGER,
    ADD COLUMN type        VARCHAR(50),
    ADD COLUMN area_m2     DECIMAL(6,2),
    ADD COLUMN description TEXT,
    ADD COLUMN status      VARCHAR(20) NOT NULL DEFAULT 'bos';

-- V5 seed'deki unit_number değerlerini number'a kopyala
UPDATE properties SET number = CAST(unit_number AS INTEGER);

-- number NOT NULL yap
ALTER TABLE properties ALTER COLUMN number SET NOT NULL;

-- Kısıtlar
ALTER TABLE properties
    ADD CONSTRAINT properties_number_unique UNIQUE (number),
    ADD CONSTRAINT properties_number_check  CHECK (number >= 1 AND number <= 94),
    ADD CONSTRAINT properties_status_check  CHECK (status IN ('bos', 'kiralik', 'sahipli'));

-- Eski sütunları kaldır
ALTER TABLE properties
    DROP COLUMN unit_number,
    DROP COLUMN block,
    DROP COLUMN floor,
    DROP COLUMN is_active;

-- ============================================================
-- property_users tablosu
-- ============================================================
CREATE TABLE property_users (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id          UUID         NOT NULL REFERENCES properties(id),
    user_id              UUID         NOT NULL REFERENCES users(id),
    relation_type        VARCHAR(20)  NOT NULL
        CONSTRAINT property_users_relation_type_check
            CHECK (relation_type IN ('ev_sahibi', 'kiraci', 'aile_bireyi')),
    ownership_percentage DECIMAL(5,2),
    start_date           DATE         NOT NULL,
    end_date             DATE,
    created_by           UUID         NOT NULL REFERENCES users(id),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    notes                TEXT
);

CREATE INDEX idx_property_users_property_id ON property_users(property_id);
CREATE INDEX idx_property_users_user_id     ON property_users(user_id);
CREATE INDEX idx_property_users_active      ON property_users(property_id) WHERE end_date IS NULL;
