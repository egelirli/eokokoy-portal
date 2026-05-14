-- ============================================================
-- V3: Rol-izin eşleşmeleri (yetki matrisi)
-- ============================================================

-- SUPER_ADMIN: tüm izinler
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN';

-- YONETIM_KURULU: sistem yönetimi hariç tüm izinler
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'YONETIM_KURULU'
  AND p.code IN (
    'USER_VIEW', 'USER_CREATE', 'USER_UPDATE', 'USER_DELETE', 'USER_ASSIGN_ROLE',
    'USER_VIEW_PROFILE', 'USER_UPDATE_PROFILE',
    'ANNOUNCEMENT_VIEW', 'ANNOUNCEMENT_CREATE', 'ANNOUNCEMENT_UPDATE',
    'ANNOUNCEMENT_DELETE', 'ANNOUNCEMENT_PUBLISH',
    'TASK_VIEW', 'TASK_CREATE', 'TASK_UPDATE', 'TASK_DELETE', 'TASK_ASSIGN', 'TASK_COMPLETE',
    'MESSAGE_VIEW', 'MESSAGE_SEND', 'MESSAGE_DELETE',
    'FORUM_VIEW', 'FORUM_CREATE_POST', 'FORUM_UPDATE_POST', 'FORUM_DELETE_POST', 'FORUM_MODERATE',
    'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD', 'DOCUMENT_UPDATE', 'DOCUMENT_DELETE', 'DOCUMENT_ADMIN',
    'DUES_VIEW', 'DUES_CREATE', 'DUES_UPDATE', 'DUES_DELETE', 'DUES_PAY', 'DUES_REPORT',
    'PROPERTY_VIEW', 'PROPERTY_CREATE', 'PROPERTY_UPDATE', 'PROPERTY_DELETE', 'PROPERTY_ASSIGN_RESIDENT',
    'VOTE_VIEW', 'VOTE_CREATE', 'VOTE_UPDATE', 'VOTE_DELETE', 'VOTE_CAST',
    'SYSTEM_VIEW_LOGS'
  );

-- EV_SAHIBI: konut sahibi yetkileri
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'EV_SAHIBI'
  AND p.code IN (
    'USER_VIEW_PROFILE', 'USER_UPDATE_PROFILE',
    'ANNOUNCEMENT_VIEW',
    'TASK_VIEW', 'TASK_COMPLETE',
    'MESSAGE_VIEW', 'MESSAGE_SEND',
    'FORUM_VIEW', 'FORUM_CREATE_POST', 'FORUM_UPDATE_POST', 'FORUM_DELETE_POST',
    'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD',
    'DUES_VIEW', 'DUES_PAY',
    'PROPERTY_VIEW',
    'VOTE_VIEW', 'VOTE_CAST'
  );

-- AILE_BIREYI: temel sakin yetkileri
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'AILE_BIREYI'
  AND p.code IN (
    'USER_VIEW_PROFILE', 'USER_UPDATE_PROFILE',
    'ANNOUNCEMENT_VIEW',
    'TASK_VIEW',
    'MESSAGE_VIEW', 'MESSAGE_SEND',
    'FORUM_VIEW', 'FORUM_CREATE_POST', 'FORUM_UPDATE_POST',
    'DOCUMENT_VIEW',
    'DUES_VIEW',
    'PROPERTY_VIEW',
    'VOTE_VIEW', 'VOTE_CAST'
  );

-- KIRACI: kiracı yetkileri (aile bireyine benzer + aidat ödeme)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'KIRACI'
  AND p.code IN (
    'USER_VIEW_PROFILE', 'USER_UPDATE_PROFILE',
    'ANNOUNCEMENT_VIEW',
    'TASK_VIEW',
    'MESSAGE_VIEW', 'MESSAGE_SEND',
    'FORUM_VIEW', 'FORUM_CREATE_POST', 'FORUM_UPDATE_POST',
    'DOCUMENT_VIEW',
    'DUES_VIEW', 'DUES_PAY',
    'PROPERTY_VIEW',
    'VOTE_VIEW', 'VOTE_CAST'
  );

-- CALISAN: görev odaklı çalışan yetkileri
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CALISAN'
  AND p.code IN (
    'USER_VIEW_PROFILE', 'USER_UPDATE_PROFILE',
    'ANNOUNCEMENT_VIEW',
    'TASK_VIEW', 'TASK_CREATE', 'TASK_UPDATE', 'TASK_COMPLETE',
    'MESSAGE_VIEW', 'MESSAGE_SEND',
    'FORUM_VIEW', 'FORUM_CREATE_POST',
    'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD',
    'DUES_VIEW',
    'PROPERTY_VIEW',
    'VOTE_VIEW'
  );

-- ZIYARETCI: sadece görüntüleme
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ZIYARETCI'
  AND p.code IN (
    'ANNOUNCEMENT_VIEW',
    'FORUM_VIEW',
    'PROPERTY_VIEW'
  );
