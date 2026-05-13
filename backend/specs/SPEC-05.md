# SPEC-05 — Duyurular

**Faz:** 1 | **Paket:** `announcement/` | **Bağımlılık:** SPEC-02, 06

## Veri Modeli

### announcements
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| title | VARCHAR(255) | NOT NULL |
| body | TEXT | NOT NULL |
| priority | ENUM | normal\|important\|urgent |
| status | ENUM | draft\|published\|archived |
| is_public | BOOLEAN | DEFAULT false |
| target_type | ENUM | all\|role_based\|property_based |
| published_at | TIMESTAMP | nullable |
| scheduled_at | TIMESTAMP | nullable |
| expires_at | TIMESTAMP | nullable |
| created_by | UUID → users | NOT NULL |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| archived_at | TIMESTAMP | nullable |

### announcement_targets
| Alan | Tip |
|------|-----|
| id | UUID PK |
| announcement_id | UUID → announcements |
| target_type | ENUM: property\|role |
| target_id | UUID (property veya role ID) |

### announcement_attachments
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| announcement_id | UUID | NOT NULL |
| file_url | VARCHAR(512) | MinIO URL |
| file_name | VARCHAR(255) | |
| file_type | ENUM | image\|document |
| file_size | INTEGER | bytes |
| display_order | INTEGER | |
| created_at | TIMESTAMP | |

### announcement_reads
| Alan | Tip |
|------|-----|
| announcement_id | UUID PK |
| user_id | UUID PK |
| read_at | TIMESTAMP |

## API Endpoints
```
GET    /api/v1/announcements                          # authenticated
GET    /api/v1/announcements/public                   # public
GET    /api/v1/announcements/:id                      # authenticated (read kaydı oluşur)
POST   /api/v1/admin/announcements                    # ADMIN, YK
PUT    /api/v1/admin/announcements/:id                # ADMIN, YK (sadece draft)
PATCH  /api/v1/admin/announcements/:id/publish        # ADMIN, YK
PATCH  /api/v1/admin/announcements/:id/archive        # ADMIN, YK
POST   /api/v1/admin/announcements/:id/attachments    # ADMIN, YK (sadece draft)
DELETE /api/v1/admin/announcements/:id/attachments/:attachmentId  # ADMIN, YK
GET    /api/v1/admin/announcements/:id/read-status    # ADMIN, YK
```

## İş Kuralları
- Yayınlanan duyuru düzenlenemez, eklenti değiştirilemez
- target_type=property_based → en az 1 konut seçili olmalı
- urgent → tüm kanallar zorunlu (kullanıcı tercihi atlanır)
- Zamanlanmış yayın: @Scheduled job ile
- expires_at dolunca otomatik archived

## Dosya Limitleri
- Fotoğraf: max 5 MB, max 5 adet (JPG, PNG, WebP)
- Belge: max 10 MB, max 3 adet (PDF, DOC, DOCX)

## Kabul Kriterleri
- [ ] Draft → published → archived akışı çalışıyor
- [ ] all/role_based/property_based hedefleme çalışıyor
- [ ] is_public duyurular /public endpoint'inde görünüyor
- [ ] Zamanlanmış yayın scheduler çalışıyor
- [ ] Okunma kaydı oluşuyor
- [ ] Urgent bildirim tüm kanallardan gidiyor
