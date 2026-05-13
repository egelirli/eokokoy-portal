# SPEC-11 — Belgeler (Doküman Yönetimi)

**Faz:** 2 | **Paket:** `document/` | **Bağımlılık:** SPEC-02, 04, 06

## Veri Modeli

### document_categories
| Alan | Tip |
|------|-----|
| id | UUID PK |
| name | VARCHAR(100) UNIQUE |
| description | TEXT |
| access_level | ENUM: admin_only\|residents\|personal |
| upload_roles | VARCHAR[] |
| is_system | BOOLEAN |
| is_active | BOOLEAN DEFAULT true |
| display_order | INTEGER |
| created_at | TIMESTAMP |

**Sistem kategorileri:**
| Kategori | access_level | upload_roles |
|----------|-------------|--------------|
| Yönetim & Karar | admin_only | [YK, SUPER_ADMIN] |
| Hukuki & Sözleşme | admin_only | [YK, SUPER_ADMIN] |
| Teknik & Proje | residents | [YK, SUPER_ADMIN] |
| Toplantı Tutanakları | residents | [YK, SUPER_ADMIN] |
| Kişisel Belgeler | personal | [EV_SAHIBI, KIRACI] |
| Genel | residents | [tüm sakinler] |

### documents
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| category_id | UUID → document_categories | NOT NULL |
| title | VARCHAR(255) | NOT NULL |
| description | TEXT | nullable |
| file_id | UUID → files | NOT NULL (SPEC-06) |
| owner_id | UUID → users | nullable (kişisel için) |
| property_id | UUID → properties | nullable |
| uploaded_by | UUID → users | NOT NULL |
| is_deleted | BOOLEAN | DEFAULT false |
| deleted_at | TIMESTAMP | nullable |
| deleted_by | UUID → users | nullable |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

## Erişim Seviyeleri
- `admin_only`: sadece YK ve SUPER_ADMIN
- `residents`: tüm aktif sakinler
- `personal`: owner_id kullanıcısı + adminler

## İş Kuralları
- Versiyonlama yok: güncelleme = eski file soft-delete + yeni file_id
- Kişisel belge: owner_id = uploaded_by zorunlu
- Belgeler MinIO'dan silinmez (soft delete)
- is_system kategori silinemez
- property_id dolu → o konunun sakinleri görebilir

## API Endpoints
```
GET    /api/v1/documents/categories            # authenticated
GET    /api/v1/documents                       # authenticated (erişim filtreli)
GET    /api/v1/documents/:id                   # authenticated + presigned URL
POST   /api/v1/documents                       # upload_roles kontrolü
PUT    /api/v1/documents/:id                   # yükleyen veya admin
DELETE /api/v1/documents/:id                   # yükleyen veya admin
GET    /api/v1/admin/documents/categories      # ADMIN, YK
POST   /api/v1/admin/documents/categories      # SUPER_ADMIN
PATCH  /api/v1/admin/documents/categories/:id  # SUPER_ADMIN
```

## Kabul Kriterleri
- [ ] 6 sistem kategorisi migration ile oluşuyor
- [ ] access_level erişim kontrolü çalışıyor
- [ ] Kişisel belge sadece sahibi + adminler görebiliyor
- [ ] Başkası adına kişisel belge yüklenemiyor
- [ ] Güncelleme: eski file soft-delete, yeni file_id atanıyor
- [ ] is_system kategori silinemiyor
