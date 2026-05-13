# SPEC-03 — Konut-Kullanıcı İlişkisi

**Faz:** 1 | **Paket:** `user/` | **Bağımlılık:** SPEC-01, 02

## Veri Modeli

### properties
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| number | INTEGER | 1-94, UNIQUE |
| type | VARCHAR(50) | nullable |
| area_m2 | DECIMAL(6,2) | nullable |
| description | TEXT | nullable |
| status | ENUM | sahipli\|kiralık\|boş (otomatik) |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### property_users
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| property_id | UUID → properties | NOT NULL |
| user_id | UUID → users | NOT NULL |
| relation_type | ENUM | ev_sahibi\|kiraci\|aile_bireyi |
| ownership_percentage | DECIMAL(5,2) | ev_sahibi için zorunlu |
| start_date | DATE | NOT NULL |
| end_date | DATE | NULL = aktif |
| created_by | UUID → users | NOT NULL |
| created_at | TIMESTAMP | |
| notes | TEXT | nullable |

**Aktif kayıt:** `end_date IS NULL`

## İş Kuralları
- Bir konutta max 1 aktif kiracı
- Aile bireyi için konutta aktif ev_sahibi şart
- Kiracı ekleme sadece admin/YK yapar
- Bir kişi birden fazla konuta sahip olabilir
- İlişki sonlanınca: başka aktif konut yoksa rol kaldırılır
- 1-94 numara aralığı zorunlu
- ownership_percentage toplamı 100.00'ı geçemez

## API Endpoints
```
GET    /api/v1/admin/properties                        # ADMIN, YK
GET    /api/v1/admin/properties/:id                   # ADMIN, YK
GET    /api/v1/properties/mine                        # tüm sakinler
POST   /api/v1/admin/properties/:id/residents         # ADMIN, YK
PATCH  /api/v1/admin/properties/:propertyId/residents/:relationId/end  # ADMIN, YK
GET    /api/v1/admin/properties/:id/history           # ADMIN, YK
GET    /api/v1/admin/users/:id/properties             # ADMIN, YK
```

## Flyway Seed
- V5: 94 konut kaydı (number 1-94, status=boş)

## Kabul Kriterleri
- [ ] 94 konut migration ile oluşuyor
- [ ] Ev sahibi ekleme + EV_SAHIBI rolü atanıyor
- [ ] Kiracı ekleme: mevcut kiracı varsa hata
- [ ] Aile bireyi: ev sahibi yoksa hata
- [ ] ownership_percentage toplamı %100 kontrolü
- [ ] İlişki sonlanınca rol kaldırma mantığı çalışıyor
- [ ] Konut durumu (status) otomatik güncelleniyor
