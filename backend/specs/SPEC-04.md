# SPEC-04 — Kimlik Doğrulama

**Faz:** 1 | **Paket:** `user/` | **Bağımlılık:** SPEC-01, 02

## Veri Modeli

### refresh_tokens
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| user_id | UUID → users | NOT NULL |
| token_hash | VARCHAR(255) | SHA-256 hash |
| expires_at | TIMESTAMP | created_at + 30 gün |
| created_at | TIMESTAMP | |
| revoked_at | TIMESTAMP | NULL = aktif |
| device_info | VARCHAR(255) | nullable |
| ip_address | VARCHAR(45) | nullable |

### password_reset_tokens
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| user_id | UUID → users | NOT NULL |
| token_hash | VARCHAR(255) | SHA-256 hash |
| expires_at | TIMESTAMP | created_at + 1 saat |
| created_at | TIMESTAMP | |
| is_used | BOOLEAN | DEFAULT false |
| ip_address | VARCHAR(45) | nullable |

## JWT Payload
```json
{ "sub": "uuid", "email": "...", "roles": ["EV_SAHIBI"],
  "permissions": ["ANNOUNCEMENT_READ", ...],
  "property_ids": ["uuid"], "iat": 0, "exp": 0 }
```

**Access token:** 15 dk | **Refresh token:** 30 gün, rotation

## API Endpoints
```
POST   /api/v1/auth/login            # public
POST   /api/v1/auth/refresh          # public (token ile)
POST   /api/v1/auth/logout           # authenticated
POST   /api/v1/auth/forgot-password  # public
POST   /api/v1/auth/reset-password   # public (token ile)
GET    /api/v1/auth/me               # authenticated
GET    /api/v1/auth/sessions         # authenticated
DELETE /api/v1/auth/sessions/:id     # authenticated
```

## Güvenlik Kuralları
- Yanlış e-posta / şifre → aynı mesaj + aynı süre (timing attack önlemi)
- Şifre sıfırlama: e-posta sistemde yoksa da aynı yanıt (user enumeration önlemi)
- Brute force: 5 deneme → 15 dk kilit
- Refresh token rotation: her kullanımda yenilenir
- Revoke edilmiş token kullanılırsa → tüm oturumlar kapatılır
- Şifre değişince → tüm refresh token'lar revoke edilir
- CORS: sadece ${APP_FRONTEND_URL}

## Şifre Politikası
Min 8 karakter, 1 büyük + 1 küçük harf + 1 rakam, max 72 karakter

## Public Endpoint'ler (SecurityConfig)
`/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/forgot-password`,
`/api/v1/auth/reset-password`, `/api/v1/auth/apply`, `/api/v1/invitations/**`,
`/api/v1/announcements/public/**`

## Kabul Kriterleri
- [ ] Login → access + refresh token dönüyor
- [ ] Refresh rotation çalışıyor
- [ ] Revoke edilmiş token → tüm oturumlar kapatılıyor
- [ ] Şifre sıfırlama akışı çalışıyor
- [ ] Timing attack önlemi var
- [ ] Brute force kilidi çalışıyor
- [ ] Audit log yazılıyor
