-- ============================================================
-- V4: İlk SUPER_ADMIN kullanıcısı seed
-- Şifre: Admin123! (BCrypt cost 12) — ilk girişte değiştirilmeli
-- E-posta: SUPER_ADMIN_EMAIL env değişkeninden alınır
-- ============================================================

INSERT INTO users (
    id, first_name, last_name, email, password_hash, status, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    'Süper',
    'Admin',
    '${super_admin_email}',
    '${super_admin_password_hash}',
    'active',
    NOW(),
    NOW()
);

INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, NOW()
FROM users u, roles r
WHERE u.email = '${super_admin_email}'
  AND r.code  = 'SUPER_ADMIN';
