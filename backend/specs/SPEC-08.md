# SPEC-08 — Mesajlaşma

**Faz:** 2 | **Paket:** `message/` | **Bağımlılık:** SPEC-02, 04

## Veri Modeli

### conversations
| Alan | Tip |
|------|-----|
| id | UUID PK |
| type | ENUM: direct\|group\|channel |
| name | VARCHAR(100) nullable (group/channel için) |
| description | TEXT nullable |
| created_by | UUID → users |
| is_active | BOOLEAN DEFAULT true |
| created_at | TIMESTAMP |
| updated_at | TIMESTAMP (son mesajda güncellenir) |

### conversation_members
| Alan | Tip |
|------|-----|
| id | UUID PK |
| conversation_id | UUID → conversations |
| user_id | UUID → users |
| role | ENUM: admin\|member |
| joined_at | TIMESTAMP |
| left_at | TIMESTAMP nullable |
| last_read_at | TIMESTAMP nullable |
| muted_until | TIMESTAMP nullable |

### messages
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| conversation_id | UUID → conversations | NOT NULL |
| sender_id | UUID → users | NOT NULL |
| body | TEXT | max 4000 karakter |
| is_deleted | BOOLEAN | DEFAULT false |
| deleted_at | TIMESTAMP | nullable |
| deleted_by | UUID → users | nullable |
| reply_to_id | UUID → messages | nullable |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | nullable (düzenlenince) |

## İş Kuralları
- Genel kanal: seed data, tüm aktif kullanıcılar otomatik üye
- İki kullanıcı arasında sadece bir aktif direct konuşma
- Grup: max 50 üye, admin/YK oluşturur
- Kullanıcı kendi mesajını 5 dk içinde düzenleyebilir/silebilir
- Admin her zaman silebilir (soft delete)
- Silinen mesaj: "Bu mesaj silindi" — içerik gizlenir

## Polling Stratejisi
- Aktif ekran: 5 saniye
- Arka plan: 30 saniye
- Endpoint: `/messages/new?after_id=...` (cursor bazlı)

## API Endpoints
```
GET    /api/v1/conversations                          # authenticated
POST   /api/v1/conversations/direct                  # authenticated
POST   /api/v1/admin/conversations/group             # ADMIN, YK
GET    /api/v1/conversations/:id/messages            # üyeler
GET    /api/v1/conversations/:id/messages/new        # polling
POST   /api/v1/conversations/:id/messages            # üyeler
PATCH  /api/v1/conversations/:id/messages/:id        # 5 dk içinde
DELETE /api/v1/conversations/:id/messages/:id        # sahip 5dk / admin her zaman
PATCH  /api/v1/conversations/:id/read                # last_read_at güncelle
GET    /api/v1/conversations/unread-count            # dashboard rozeti
POST   /api/v1/admin/conversations/:id/members       # ADMIN, YK
DELETE /api/v1/admin/conversations/:id/members/:id   # ADMIN, YK
PATCH  /api/v1/conversations/:id/mute               # authenticated
```

## Seed Data
Genel kanal tek bir kez oluşturulur. Tüm aktif kullanıcılar üye.

## Kabul Kriterleri
- [ ] Direct konuşma: mevcut varsa yeni oluşturulmuyor
- [ ] Genel kanal seed ile oluşuyor, yeni kullanıcı otomatik ekleniyor
- [ ] Polling cursor bazlı çalışıyor
- [ ] 5 dk kuralı uygulanıyor
- [ ] last_read_at güncelleniyor, okunmamış sayısı doğru
