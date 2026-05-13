# SPEC-07 — Task Yönetimi (Talep & Şikayet)

**Faz:** 2 | **Paket:** `task/` | **Bağımlılık:** SPEC-02, 06

## Veri Modeli

### task_categories
| Alan | Tip |
|------|-----|
| id | UUID PK |
| name | VARCHAR(100) UNIQUE |
| description | TEXT |
| is_system | BOOLEAN |
| is_active | BOOLEAN DEFAULT true |
| display_order | INTEGER |
| created_at | TIMESTAMP |

**Sistem kategorileri (is_system=true):** Bakım & Onarım, Temizlik, Güvenlik, Öneri, Diğer

### tasks
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| task_number | VARCHAR(20) | TLK-2026-0042 |
| title | VARCHAR(255) | NOT NULL |
| description | TEXT | NOT NULL |
| category_id | UUID → task_categories | NOT NULL |
| priority | ENUM | low\|normal\|high\|urgent |
| status | ENUM | pending\|assigned\|in_progress\|completed |
| property_id | UUID → properties | nullable |
| location_detail | VARCHAR(255) | nullable |
| created_by | UUID → users | NOT NULL |
| assigned_to | UUID → users | nullable |
| assigned_by | UUID → users | nullable |
| assigned_at | TIMESTAMP | nullable |
| completed_at | TIMESTAMP | nullable |
| completion_note | TEXT | nullable |
| rating | INTEGER | 1-5, nullable |
| rating_comment | TEXT | nullable |
| rated_at | TIMESTAMP | nullable |
| sla_deadline | TIMESTAMP | nullable |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### task_attachments
`id, task_id, file_id → files, uploaded_by, created_at`

### task_comments
`id, task_id, user_id, body TEXT, is_internal BOOLEAN, created_at`

### task_status_history
`id, task_id, from_status, to_status, changed_by, note, created_at`

## Durum Makinesi
```
pending → assigned (admin atar)
pending → in_progress (admin direkt alır)
assigned → in_progress (atanan başlar)
in_progress → completed (atanan tamamlar)
completed → in_progress (admin geri alır)
```

## İş Kuralları
- Atama: CALISAN veya herhangi aktif kullanıcı (sakin dahil)
- Rating: tamamlandıktan 7 gün içinde, bir kez, değiştirilemez
- is_internal=true yorumlar sakin göremez
- SLA: kategori bazlı otomatik deadline, @Scheduled aşım tespiti
- is_system kategori silinemez

## API Endpoints
```
POST   /api/v1/tasks                         # sakinler
GET    /api/v1/tasks                         # sakin: kendi, admin: tümü
GET    /api/v1/tasks/:id                     # yetki kontrolü
PATCH  /api/v1/admin/tasks/:id/assign        # ADMIN, YK
PATCH  /api/v1/tasks/:id/status              # atanan + admin
POST   /api/v1/tasks/:id/comments            # taraflar
POST   /api/v1/tasks/:id/attachments         # presigned URL
POST   /api/v1/tasks/:id/rating              # konu sahibi, 7 gün içinde
GET    /api/v1/admin/tasks/stats             # ADMIN, YK
GET    /api/v1/admin/task-categories         # ADMIN, YK
POST   /api/v1/admin/task-categories         # SUPER_ADMIN
```

## Kabul Kriterleri
- [ ] task_number otomatik üretiliyor
- [ ] Durum makinesi geçersiz geçişleri engelliyor
- [ ] Status history yazılıyor
- [ ] is_internal yorum sakinlere görünmüyor
- [ ] Rating 7 gün sonra kapanıyor
- [ ] SLA aşım bildirimi tetikleniyor
- [ ] is_system kategori silinemiyor
