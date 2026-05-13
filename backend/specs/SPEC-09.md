# SPEC-09 — Forum

**Faz:** 2 | **Paket:** `forum/` | **Bağımlılık:** SPEC-02, 04

## Veri Modeli

### forum_categories
| Alan | Tip |
|------|-----|
| id | UUID PK |
| name | VARCHAR(100) UNIQUE |
| description | TEXT |
| slug | VARCHAR(120) UNIQUE |
| is_system | BOOLEAN |
| is_active | BOOLEAN DEFAULT true |
| display_order | INTEGER |
| created_at | TIMESTAMP |

**Sistem kategorileri:** Genel Tartışma, İlan & Takas, Gündem, Öneri & Şikayet, Etkinlik

### forum_topics
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| category_id | UUID → forum_categories | NOT NULL |
| title | VARCHAR(255) | NOT NULL |
| body | TEXT | max 10.000 karakter |
| created_by | UUID → users | NOT NULL |
| is_pinned | BOOLEAN | DEFAULT false |
| is_locked | BOOLEAN | DEFAULT false |
| is_deleted | BOOLEAN | DEFAULT false |
| deleted_at | TIMESTAMP | nullable |
| deleted_by | UUID → users | nullable |
| reply_count | INTEGER | DEFAULT 0 (denormalize) |
| view_count | INTEGER | DEFAULT 0 |
| last_reply_at | TIMESTAMP | nullable |
| last_reply_by | UUID → users | nullable |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | nullable |

### forum_replies
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| topic_id | UUID → forum_topics | NOT NULL |
| body | TEXT | max 5.000 karakter |
| created_by | UUID → users | NOT NULL |
| reply_to_id | UUID → forum_replies | nullable |
| is_deleted | BOOLEAN | DEFAULT false |
| deleted_at | TIMESTAMP | nullable |
| deleted_by | UUID → users | nullable |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | nullable |

## Erişim
- Sadece aktif sakinler — ziyaretçi erişemez (401)
- Konu düzenleme: 30 dakika içinde
- Yanıtı olan konu yazar tarafından silinemez
- Kilitli konuya yanıt yazılamaz

## Sıralama
1. is_pinned=true (kendi aralarında last_reply_at DESC)
2. Normal (last_reply_at DESC)

## API Endpoints
```
GET    /api/v1/forum/categories                      # authenticated
GET    /api/v1/forum/categories/:slug/topics         # authenticated
GET    /api/v1/forum/topics/:id                      # authenticated (view_count++)
POST   /api/v1/forum/topics                          # sakinler
PATCH  /api/v1/forum/topics/:id                      # yazar 30dk
DELETE /api/v1/forum/topics/:id                      # yazar(yanıtsız)/admin
POST   /api/v1/forum/topics/:id/replies              # sakinler
PATCH  /api/v1/forum/replies/:id                     # yazar 30dk
DELETE /api/v1/forum/replies/:id                     # yazar/admin
PATCH  /api/v1/admin/forum/topics/:id/pin            # ADMIN, YK
PATCH  /api/v1/admin/forum/topics/:id/lock           # ADMIN, YK
PATCH  /api/v1/admin/forum/topics/:id/move           # ADMIN, YK
POST   /api/v1/admin/forum/categories                # SUPER_ADMIN
PATCH  /api/v1/admin/forum/categories/:id            # SUPER_ADMIN
```

## Kabul Kriterleri
- [ ] Ziyaretçi 401 alıyor
- [ ] Sabitlenmiş konular listenin başında
- [ ] reply_count ve last_reply_at güncelleniyor
- [ ] 30 dk düzenleme kuralı uygulanıyor
- [ ] Yanıtı olan konu silinemiyor (yazar)
- [ ] Kilitli konuya yanıt yazılamıyor
- [ ] is_system kategori silinemiyor
