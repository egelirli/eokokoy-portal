# SPEC-06 — Ortak Dosya Servisi

**Faz:** 1 | **Paket:** `storage/` | **Bağımlılık:** SPEC-02, 04

## MinIO Bucket'ları
| Bucket | Erişim | Kimler |
|--------|--------|--------|
| ekokoy-public | Public read | Herkes |
| ekokoy-private | Private | Giriş yapmış sakinler |
| ekokoy-admin | Private | YK, SUPER_ADMIN |
| ekokoy-profiles | Private | Sahip + adminler |

## Veri Modeli

### files
| Alan | Tip | Kısıt |
|------|-----|-------|
| id | UUID | PK |
| bucket | VARCHAR(50) | NOT NULL |
| object_key | VARCHAR(512) | NOT NULL |
| original_name | VARCHAR(255) | NOT NULL |
| mime_type | VARCHAR(100) | NOT NULL (Apache Tika ile doğrulama) |
| file_size | BIGINT | bytes |
| file_type | ENUM | image\|document\|other |
| thumbnail_key | VARCHAR(512) | nullable (image için) |
| compressed_key | VARCHAR(512) | nullable (image için) |
| uploaded_by | UUID → users | NOT NULL |
| uploaded_at | TIMESTAMP | NOT NULL |
| is_deleted | BOOLEAN | DEFAULT false |
| deleted_at | TIMESTAMP | nullable |
| deleted_by | UUID → users | nullable |
| checksum | VARCHAR(64) | SHA-256 |

**Object key şablonu:** `{module}/{year}/{month}/{uuid}.{ext}`

## Upload Akışı (Presigned URL)
1. `POST /api/v1/files/upload-url` → presigned PUT URL (15 dk)
2. İstemci → MinIO'ya doğrudan PUT
3. `POST /api/v1/files/:id/confirm` → is_confirmed=true
4. Fotoğrafsa: @Async sıkıştırma + thumbnail

## Fotoğraf İşleme (Thumbnailator, @Async)
- Orijinal: korunur
- Sıkıştırma: WebP %85, max 1920px → compressed_key
- Thumbnail: 200x200 crop center WebP → thumbnail_key
- HEIC → JPEG dönüşümü

## API Endpoints
```
POST   /api/v1/files/upload-url     # authenticated
POST   /api/v1/files/:id/confirm    # authenticated
GET    /api/v1/files/:id            # bucket erişim kuralına göre
DELETE /api/v1/files/:id            # sahip veya admin
```

## İş Kuralları
- Dosyalar MinIO'dan asla fiziksel silinmez (soft delete)
- Checksum eşleşirse duplicate — yeni MinIO nesnesi oluşmaz
- MIME type: Apache Tika ile doğrulama (uzantıya güvenilmez)
- Onaylanmamış (is_confirmed=false) dosyalar 24 saat sonra soft-delete

## Dosya Limitleri
- Fotoğraf: 10 MB (JPG, PNG, WebP, HEIC)
- Belge: 20 MB (PDF, DOC, DOCX, XLS, XLSX)

## Kabul Kriterleri
- [ ] Presigned URL üretiliyor
- [ ] Confirm endpoint çalışıyor
- [ ] Duplicate checksum tespiti çalışıyor
- [ ] Async fotoğraf işleme çalışıyor (thumbnail + compress)
- [ ] Bucket erişim matrisi doğru uygulanıyor
- [ ] Soft delete çalışıyor, MinIO'dan silinmiyor
