# SPEC-02 — Roller & Yetki Matrisi

**Faz:** 1 | **Paket:** `user/` | **Bağımlılık:** SPEC-01

## Roller
| Kod | Seviye |
|-----|--------|
| SUPER_ADMIN | 1 |
| YONETIM_KURULU | 2 |
| EV_SAHIBI | 3 |
| AILE_BIREYI | 4 |
| KIRACI | 4 |
| CALISAN | 4 |
| ZIYARETCI | 5 |

## Veri Modeli

### roles
| Alan | Tip |
|------|-----|
| id | UUID PK |
| code | VARCHAR(50) UNIQUE |
| display_name | VARCHAR(100) |
| description | TEXT |
| is_active | BOOLEAN DEFAULT true |

### permissions
| Alan | Tip |
|------|-----|
| id | UUID PK |
| code | VARCHAR(100) UNIQUE |
| category | VARCHAR(50) |
| description | TEXT |

### role_permissions
| Alan | Tip |
|------|-----|
| role_id | FK → roles |
| permission_id | FK → permissions |
| PK | (role_id, permission_id) |

### user_roles
| Alan | Tip |
|------|-----|
| user_id | FK → users |
| role_id | FK → roles |
| assigned_at | TIMESTAMP |
| assigned_by | FK → users |
| PK | (user_id, role_id) |

## İzin Kategorileri
`USER_*` `ANNOUNCEMENT_*` `TASK_*` `MESSAGE_*` `FORUM_*` `DOCUMENT_*` `DUES_*` `PROPERTY_*` `VOTE_*` `SYSTEM_*`

## Temel Yetki Kuralları
- Çoklu rol: Bir kullanıcı birden fazla role sahip olabilir
- JWT payload'ına tüm rollerin izin birleşimi eklenir
- SUPER_ADMIN başka rolle birleştirilemez
- SUPER_ADMIN sadece mevcut SUPER_ADMIN atayabilir
- Son rol kaldırılamaz

## API Endpoints
```
GET    /api/v1/admin/roles                         # ADMIN, YK
GET    /api/v1/admin/roles/:id/permissions         # ADMIN, YK
POST   /api/v1/admin/users/:id/roles               # ADMIN, YK
DELETE /api/v1/admin/users/:id/roles/:roleId       # ADMIN, YK
GET    /api/v1/admin/users/:id/roles               # ADMIN, YK
GET    /api/v1/me/permissions                      # tüm giriş yapmışlar
```

## Flyway Seed (V1-V4)
- V1: 7 rol
- V2: tüm izin kodları
- V3: rol-izin eşleşmeleri (yetki matrisi)
- V4: ilk SUPER_ADMIN (${SUPER_ADMIN_EMAIL}'den)

## Kabul Kriterleri
- [ ] Rol atama/kaldırma çalışıyor
- [ ] Son rol kaldırma engelleniyor
- [ ] SUPER_ADMIN sadece SUPER_ADMIN atayabiliyor
- [ ] /me/permissions doğru izin listesi dönüyor
- [ ] V1-V4 migration'ları hatasız çalışıyor
