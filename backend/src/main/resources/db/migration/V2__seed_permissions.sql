-- ============================================================
-- V2: Tüm izin kodları seed
-- ============================================================

INSERT INTO permissions (code, category, description) VALUES
-- USER_*
('USER_VIEW',            'USER',         'Kullanıcıları listele ve görüntüle'),
('USER_CREATE',          'USER',         'Kullanıcı oluştur (davet et)'),
('USER_UPDATE',          'USER',         'Kullanıcı bilgilerini güncelle'),
('USER_DELETE',          'USER',         'Kullanıcıyı sil (soft delete)'),
('USER_ASSIGN_ROLE',     'USER',         'Kullanıcıya rol ata veya kaldır'),
('USER_VIEW_PROFILE',    'USER',         'Kendi profilini görüntüle'),
('USER_UPDATE_PROFILE',  'USER',         'Kendi profilini güncelle'),

-- ANNOUNCEMENT_*
('ANNOUNCEMENT_VIEW',    'ANNOUNCEMENT', 'Duyuruları görüntüle'),
('ANNOUNCEMENT_CREATE',  'ANNOUNCEMENT', 'Duyuru oluştur'),
('ANNOUNCEMENT_UPDATE',  'ANNOUNCEMENT', 'Duyuru güncelle'),
('ANNOUNCEMENT_DELETE',  'ANNOUNCEMENT', 'Duyuru sil'),
('ANNOUNCEMENT_PUBLISH', 'ANNOUNCEMENT', 'Duyuru yayınla'),

-- TASK_*
('TASK_VIEW',            'TASK',         'Görevleri görüntüle'),
('TASK_CREATE',          'TASK',         'Görev oluştur'),
('TASK_UPDATE',          'TASK',         'Görev güncelle'),
('TASK_DELETE',          'TASK',         'Görev sil'),
('TASK_ASSIGN',          'TASK',         'Görevi başkasına ata'),
('TASK_COMPLETE',        'TASK',         'Görevi tamamlandı olarak işaretle'),

-- MESSAGE_*
('MESSAGE_VIEW',         'MESSAGE',      'Mesajları görüntüle'),
('MESSAGE_SEND',         'MESSAGE',      'Mesaj gönder'),
('MESSAGE_DELETE',       'MESSAGE',      'Mesaj sil'),

-- FORUM_*
('FORUM_VIEW',           'FORUM',        'Forumu görüntüle'),
('FORUM_CREATE_POST',    'FORUM',        'Forum gönderisi oluştur'),
('FORUM_UPDATE_POST',    'FORUM',        'Forum gönderisi güncelle'),
('FORUM_DELETE_POST',    'FORUM',        'Forum gönderisi sil'),
('FORUM_MODERATE',       'FORUM',        'Forum moderasyonu yap'),

-- DOCUMENT_*
('DOCUMENT_VIEW',        'DOCUMENT',     'Belgeleri görüntüle'),
('DOCUMENT_UPLOAD',      'DOCUMENT',     'Belge yükle'),
('DOCUMENT_UPDATE',      'DOCUMENT',     'Belge güncelle'),
('DOCUMENT_DELETE',      'DOCUMENT',     'Belge sil'),
('DOCUMENT_ADMIN',       'DOCUMENT',     'Belge yönetimi (tüm belgeler)'),

-- DUES_*
('DUES_VIEW',            'DUES',         'Aidatları görüntüle'),
('DUES_CREATE',          'DUES',         'Aidat kaydı oluştur'),
('DUES_UPDATE',          'DUES',         'Aidat kaydı güncelle'),
('DUES_DELETE',          'DUES',         'Aidat kaydı sil'),
('DUES_PAY',             'DUES',         'Aidat öde'),
('DUES_REPORT',          'DUES',         'Aidat raporu oluştur'),

-- PROPERTY_*
('PROPERTY_VIEW',          'PROPERTY',   'Konutları görüntüle'),
('PROPERTY_CREATE',        'PROPERTY',   'Konut oluştur'),
('PROPERTY_UPDATE',        'PROPERTY',   'Konut bilgilerini güncelle'),
('PROPERTY_DELETE',        'PROPERTY',   'Konut sil'),
('PROPERTY_ASSIGN_RESIDENT','PROPERTY',  'Konuta sakin ata'),

-- VOTE_*
('VOTE_VIEW',            'VOTE',         'Oylamaları görüntüle'),
('VOTE_CREATE',          'VOTE',         'Oylama oluştur'),
('VOTE_UPDATE',          'VOTE',         'Oylama güncelle'),
('VOTE_DELETE',          'VOTE',         'Oylama sil'),
('VOTE_CAST',            'VOTE',         'Oylamaya oy ver'),

-- SYSTEM_*
('SYSTEM_ADMIN',          'SYSTEM',      'Tam sistem yönetimi'),
('SYSTEM_VIEW_LOGS',      'SYSTEM',      'Sistem günlüklerini görüntüle'),
('SYSTEM_MANAGE_SETTINGS','SYSTEM',      'Sistem ayarlarını yönet');
