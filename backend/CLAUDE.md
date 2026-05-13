# Ekoköy Portalı — Claude Code Rehberi

## Stack
```
Backend:    Spring Boot 4.0  (Java 25)
Güvenlik:   Spring Security + JWT + Refresh Token
Veritabanı: PostgreSQL
ORM:        Spring Data JPA + Hibernate
Migration:  Flyway
Depolama:   MinIO  (self-hosted Docker)
E-posta:    Spring Mail + Thymeleaf
Build:      Gradle (Kotlin DSL)
API:        REST — /api/v1/ prefix
```

## Proje Yapısı
```
src/main/java/com/ekokoy/
├── config/          # SecurityConfig, JwtConfig, MinioConfig
├── common/          # GlobalExceptionHandler, ApiResponse, JwtUtil
└── {module}/        # Her spec için ayrı paket
    ├── controller/
    ├── service/
    ├── repository/
    ├── entity/
    └── dto/
```

Modül → Paket:
```
SPEC-01,02,03,04 → user/
SPEC-05          → announcement/
SPEC-06          → storage/
SPEC-07          → task/
SPEC-08          → message/
SPEC-09          → forum/
SPEC-10          → notification/
SPEC-11          → document/
SPEC-12          → dues/
SPEC-13          → poll/
```

## Spring Boot 4.0 Notları
- `javax.*` değil `jakarta.*` import'ları
- Spring Framework 7 tabanlı
- JSpecify null-safety anotasyonları (`@Nullable` yerine)
- Daha granüler starter'lar — sadece ihtiyaç duyulan starter'ı ekle

## Mimari Kurallar — ZORUNLU

**Katman yapısı:** Controller → Service → Repository (atlanamaz)
- Controller: HTTP mapping + DTO dönüşümü. İş mantığı yok.
- Service: Tüm iş mantığı. `@Transactional` burada.
- Repository: JpaRepository + custom query'ler.

**DTO:** Java 25 `record`. Entity asla Controller'a dönmez.
```java
public record UserResponse(UUID id, String firstName, String email) {}
```

**Response sarmalayıcı:**
```java
public record ApiResponse<T>(boolean success, T data, String message) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }
}
```

**Exception:**
```java
throw new EkokoyException("EMAIL_EXISTS", "Bu e-posta kayıtlı.", 422);
// GlobalExceptionHandler → ErrorResponse(code, message, status)
```

**Güvenlik:** Her endpoint `@PreAuthorize`. Public endpoint'ler SecurityConfig'de açıkça belirtilir.
```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAnyRole('ADMIN','YONETIM_KURULU')")
```

## Veritabanı Kuralları
- ID: `UUID` — BigSerial değil
- Tarih: `TIMESTAMP WITH TIME ZONE` (UTC)
- Soft delete: `is_deleted BOOLEAN` + `deleted_at TIMESTAMP`
- Auditing: `@CreatedDate`, `@LastModifiedDate` — tüm entity'lerde
- Alan adları: **İngilizce** (first_name, created_at, is_active...)
- Migration: `V{n}__{aciklama}.sql` — bir kez yazılır, asla değiştirilmez

## Güvenlik
```
Şifre:         bcrypt cost 12
JWT secret:    ${JWT_SECRET} — asla hard-code yok
Access token:  15 dk
Refresh token: 30 gün, DB'de SHA-256 hash
Rate limiting: Bucket4j
```

## Test — Zorunlu
- **Unit:** Service, repo mock'lu. `should_[beklenen]_when_[koşul]()`
- **Integration:** `@SpringBootTest` + Testcontainers PostgreSQL

## Claude Code Kullanımı
```bash
# Yeni spec implement et
"CLAUDE.md ve specs/SPEC-07.md oku. Kabul kriterlerini karşıla."

# Hata düzelt
"CLAUDE.md oku. task/service/TaskService.java → [sorun] düzelt."

# Özellik ekle
"CLAUDE.md ve specs/SPEC-05.md oku. Zamanlanmış yayın özelliğini ekle."
```

## Ortam Değişkenleri
```bash
DB_HOST, DB_PORT=5432, DB_NAME=ekokoy, DB_USER, DB_PASSWORD
JWT_SECRET, JWT_ACCESS_EXPIRATION=900, JWT_REFRESH_EXPIRATION=2592000
MINIO_ENDPOINT=http://localhost:9000, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_BUCKET=ekokoy
MAIL_HOST, MAIL_PORT=587, MAIL_USERNAME, MAIL_PASSWORD
APP_BASE_URL=http://localhost:8080, APP_FRONTEND_URL=http://localhost:3000
WHATSAPP_API_TOKEN, WHATSAPP_PHONE_ID
SUPER_ADMIN_EMAIL
```

## Diğer
- Lombok yok — Java 25 record yeterli
- Domain terimleri Türkçe kalır: konut, sakin, aidat vb.
- Her public metot Javadoc ile belgelenir

---
*Spec detayları: `specs/SPEC-01.md` → `specs/SPEC-13.md`*
