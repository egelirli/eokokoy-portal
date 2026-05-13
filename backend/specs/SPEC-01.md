# SPEC-01 — Kullanıcı Kaydı & Davet Akışı

**Faz:** 1 | **Paket:** `user/` | **Bağımlılık:** SPEC-02, 03, 04

## Veri Modeli

### users
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| first_name | VARCHAR(100) | NOT NULL |
| last_name | VARCHAR(100) | NOT NULL |
| email | VARCHAR(255) | UNIQUE NOT NULL |
| phone | VARCHAR(20) | E.164 format, nullable |
| password_hash | VARCHAR(255) | bcrypt cost 12 |
| status | ENUM | pending\|active\|inactive\|suspended |
| created_at | TIMESTAMP | NOT NULL |
| approved_at | TIMESTAMP | nullable |
| approved_by | UUID → users | nullable |
| profile_photo_url | VARCHAR(512) | nullable |
| last_login_at | TIMESTAMP | nullable |

### invitations
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| email | VARCHAR(255) | NOT NULL |
| token | VARCHAR(255) | UNIQUE, SHA-256 hash |
| role_id | UUID → roles | NOT NULL |
| property_id | UUID → properties | nullable |
| created_by | UUID → users | NOT NULL |
| created_at | TIMESTAMP | NOT NULL |
| expires_at | TIMESTAMP | created_at + 48h |
| is_used | BOOLEAN | DEFAULT false |
| used_at | TIMESTAMP | nullable |

## API Endpoints

```
POST   /api/v1/auth/apply                    # public
GET    /api/v1/admin/applications            # ADMIN, YK
PATCH  /api/v1/admin/applications/:id        # ADMIN, YK — action: approve|reject|request_info
POST   /api/v1/admin/invitations             # ADMIN, YK
GET    /api/v1/invitations/verify/:token     # public
POST   /api/v1/invitations/complete          # public
POST   /api/v1/admin/invitations/:id/resend  # ADMIN, YK
```

## İş Kuralları
- Başvuru: status=pending → admin onayı → status=active
- Davet: token 48 saat geçerli, tek kullanımlık, SecureRandom 32 byte hex
- SUPER_ADMIN rolü davet ile atanamaz
- Aynı email ile mükerrer başvuru reddedilir
- Brute force: 5 başarısız token denemesi → 15 dk kilit (Bucket4j)
- Tüm aksiyonlar audit log'a yazılır

## Kabul Kriterleri
- [ ] Başvuru formu validasyonu çalışıyor
- [ ] status=pending kaydı oluşuyor
- [ ] Admin onayla/reddet/bilgi iste işlemleri çalışıyor
- [ ] Aktivasyon e-postası gönderiliyor
- [ ] Davet token'ı 48 saat sonra geçersiz
- [ ] Aynı link iki kez kullanılamıyor
- [ ] Mükerrer başvuru hata veriyor
- [ ] Audit log yazılıyor
