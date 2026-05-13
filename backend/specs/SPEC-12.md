# SPEC-12 — Aidat Takibi

**Faz:** 1 | **Paket:** `dues/` | **Bağımlılık:** SPEC-02, 03

## Kapsam
- ✅ Excel/CSV import ile borç yükleme
- ✅ Güncel ve geçmiş borç görüntüleme
- ✅ Manuel ödeme kaydı
- ✅ Vadesi geçmiş borç hatırlatma
- ❌ Otomatik hesaplama yok
- ❌ Online ödeme/tahsilat yok

## Veri Modeli

### dues
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| property_id | UUID → properties | NOT NULL |
| user_id | UUID → users | ev sahibi |
| period_year | INTEGER | NOT NULL |
| period_month | INTEGER | 1-12, nullable (yıllık için) |
| amount | DECIMAL(10,2) | NOT NULL |
| status | ENUM | unpaid\|paid\|partially_paid\|cancelled |
| paid_amount | DECIMAL(10,2) | DEFAULT 0.00 |
| due_date | DATE | NOT NULL |
| paid_at | DATE | nullable |
| description | VARCHAR(255) | nullable |
| import_batch_id | UUID → due_imports | nullable |
| created_by | UUID → users | NOT NULL |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**Status hesaplama:** paid_amount=0→unpaid, 0<paid<amount→partially_paid, paid>=amount→paid

### due_payments
| Alan | Tip |
|------|-----|
| id | UUID PK |
| due_id | UUID → dues |
| amount | DECIMAL(10,2) |
| payment_date | DATE |
| payment_method | VARCHAR(100) nullable |
| reference_no | VARCHAR(100) nullable |
| notes | TEXT nullable |
| recorded_by | UUID → users |
| created_at | TIMESTAMP |

### due_imports
| Alan | Tip |
|------|-----|
| id | UUID PK |
| file_name | VARCHAR(255) |
| imported_by | UUID → users |
| total_rows | INTEGER |
| success_rows | INTEGER |
| error_rows | INTEGER |
| error_details | JSONB nullable |
| status | ENUM: processing\|completed\|failed |
| created_at | TIMESTAMP |
| completed_at | TIMESTAMP nullable |

## Import CSV/Excel Şeması
```
konut_no, yil, ay (nullable), tutar, son_odeme_tarihi, aciklama (nullable)
```
- Aynı konut+yıl+ay → upsert (güncelleme)
- Hatalı satırlar atlanır, import durdurmaz
- Max 5 MB

## API Endpoints
```
GET    /api/v1/dues/my                         # EV_SAHIBI
GET    /api/v1/dues/my/summary                 # EV_SAHIBI
GET    /api/v1/admin/dues                      # ADMIN, YK
GET    /api/v1/admin/dues/summary              # ADMIN, YK
GET    /api/v1/admin/properties/:id/dues       # ADMIN, YK
POST   /api/v1/admin/dues/import               # ADMIN, YK (multipart)
GET    /api/v1/admin/dues/imports              # ADMIN, YK
GET    /api/v1/admin/dues/imports/:id          # ADMIN, YK
POST   /api/v1/admin/dues/:id/payments         # ADMIN, YK
GET    /api/v1/admin/dues/:id/payments         # ADMIN, YK
DELETE /api/v1/admin/dues/payments/:id         # SUPER_ADMIN
PATCH  /api/v1/admin/dues/:id/cancel           # SUPER_ADMIN
```

## Hatırlatma (@Scheduled + SPEC-10)
- 3 gün önce: e-posta + uygulama içi
- Vade günü: e-posta + uygulama içi
- 7 gün sonra: e-posta (sahip + admin)

## Kabul Kriterleri
- [ ] CSV import çalışıyor, rapor dönüyor
- [ ] Excel (.xlsx) import çalışıyor
- [ ] Upsert (aynı dönem güncelleme) çalışıyor
- [ ] Hatalı satırlar import'u durdurmadan atlanıyor
- [ ] Ödeme kaydı → status otomatik güncelleniyor
- [ ] Kısmi ödeme (partially_paid) çalışıyor
- [ ] Ev sahibi kendi borçlarını görebiliyor
