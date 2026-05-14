-- ============================================================
-- V1: Şema oluştur + 7 rol seed
-- ============================================================

CREATE TABLE roles (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   BOOLEAN     NOT NULL DEFAULT true
);

CREATE TABLE permissions (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    category    VARCHAR(50)  NOT NULL,
    description TEXT
);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id),
    permission_id UUID NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    email             VARCHAR(255) NOT NULL UNIQUE,
    phone             VARCHAR(20),
    password_hash     VARCHAR(255),
    status            VARCHAR(20)  NOT NULL DEFAULT 'pending'
        CONSTRAINT users_status_check CHECK (status IN ('pending','active','inactive','suspended')),
    approved_at       TIMESTAMP WITH TIME ZONE,
    approved_by       UUID REFERENCES users(id),
    profile_photo_url VARCHAR(512),
    last_login_at     TIMESTAMP WITH TIME ZONE,
    is_deleted        BOOLEAN      NOT NULL DEFAULT false,
    deleted_at        TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    user_id     UUID NOT NULL REFERENCES users(id),
    role_id     UUID NOT NULL REFERENCES roles(id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    assigned_by UUID REFERENCES users(id),
    PRIMARY KEY (user_id, role_id)
);

-- Seed 7 rol
INSERT INTO roles (code, display_name, description) VALUES
('SUPER_ADMIN',     'Süper Admin',      'Tam sistem yöneticisi — başka rolle birleştirilemez'),
('YONETIM_KURULU',  'Yönetim Kurulu',   'Kooperatif yönetim kurulu üyesi'),
('EV_SAHIBI',       'Ev Sahibi',        'Konut sahibi'),
('AILE_BIREYI',     'Aile Bireyi',      'Ev sahibinin aile bireyi'),
('KIRACI',          'Kiracı',           'Konut kiracısı'),
('CALISAN',         'Çalışan',          'Kooperatif çalışanı'),
('ZIYARETCI',       'Ziyaretçi',        'Geçici ziyaretçi — sadece görüntüleme yetkisi');
