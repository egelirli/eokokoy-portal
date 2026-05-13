# 🌿 Ekoköy Portalı

Seferihisar Ekoköy Kooperatifi'nin 94 hanesi için geliştirilmiş dijital yönetim portalı.

---

## Proje Hakkında

Ekoköy Portalı; sakinlerin duyurulara erişmesini, talep ve şikayetlerini iletmesini, aidat durumunu takip etmesini, kooperatif kararlarına oy kullanmasını ve yönetimle iletişim kurmasını sağlayan web tabanlı bir platformdur.

### Temel Özellikler

| Modül | Açıklama |
|-------|----------|
| 👤 Kullanıcı Yönetimi | Kayıt, davet, rol ve yetki sistemi |
| 🏠 Konut Takibi | 94 konut, sakin ilişkileri, hisse yüzdesi |
| 📢 Duyurular | Konut bazlı hedefleme, acil bildirim |
| 📋 Talep & Şikayet | Kategori, atama, durum takibi, değerlendirme |
| 💬 Mesajlaşma | Bire bir, grup ve genel kanal |
| 🗣️ Forum | Kategorili sakin tartışma platformu |
| 💰 Aidat Takibi | Excel/CSV import, ödeme kaydı |
| 🗳️ Oylama & Anket | Açık/gizli oylama, çok sorulu anket |
| 📄 Belgeler | Kategorili doküman yönetimi |
| 🔔 Bildirimler | E-posta + WhatsApp + uygulama içi |

---

## Teknoloji

### Backend
```
Java 25 LTS + Spring Boot 4.0
PostgreSQL + Spring Data JPA + Flyway
Spring Security + JWT
MinIO (dosya depolama)
Spring Mail + Meta WhatsApp Cloud API
```

### Frontend
```
React 18 + Vite + TypeScript
Shadcn/ui + Tailwind CSS
Zustand + React Query
React Hook Form + Zod
```

---

## Proje Yapısı

```
ekokoy-portal/
├── docs/               ← Teknik şartnameler ve kararlar
│   ├── specs/          ← SPEC-01 … SPEC-13 (Word)
│   └── ...
├── backend/            ← Spring Boot uygulaması
│   ├── specs/          ← SPEC-01.md … SPEC-13.md (Claude Code için)
│   └── src/
└── frontend/           ← React uygulaması
    └── src/
```

---

## Başlarken

### Gereksinimler
- Java 25
- Node.js 20+
- PostgreSQL 16+
- Docker (MinIO için)

### Backend
```bash
cd backend

# Ortam değişkenlerini ayarla
cp .env.example .env

# Veritabanını başlat (Docker)
docker run -d --name ekokoy-postgres \
  -e POSTGRES_DB=ekokoy \
  -e POSTGRES_USER=ekokoy \
  -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 postgres:16

# MinIO başlat
docker run -d --name ekokoy-minio \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  -p 9000:9000 -p 9001:9001 \
  minio/minio server /data --console-address ":9001"

# Uygulamayı başlat
./gradlew bootRun
```

### Frontend
```bash
cd frontend

# Bağımlılıkları yükle
npm install

# Ortam değişkenlerini ayarla
cp .env.example .env.local

# Geliştirme sunucusunu başlat
npm run dev
```

### Çalışıyor mu?
- Backend API: http://localhost:8080/api/v1
- Frontend: http://localhost:5173
- MinIO Console: http://localhost:9001

---

## Geliştirme

Bu proje **Spec-Driven Development** yaklaşımıyla geliştirilmektedir.

```
docs/specs/SPEC-XX.docx   ← Detaylı teknik şartname (insan okur)
backend/specs/SPEC-XX.md  ← Özet spec (Claude Code okur)
```

**Claude Code ile çalışmak için:**
```bash
# Backend üzerinde çalışırken
code ekokoy-portal/backend

# Claude Code'a
"CLAUDE.md ve specs/SPEC-07.md oku. Task yönetimi modülünü implement et."
```

---

## Modül Durumu

### Faz 1 — Temel Altyapı
| Spec | Modül | Durum |
|------|-------|-------|
| SPEC-01 | Kullanıcı Kaydı & Davet | 🔄 Geliştiriliyor |
| SPEC-02 | Roller & Yetki Matrisi | 🔄 Geliştiriliyor |
| SPEC-03 | Konut-Kullanıcı İlişkisi | ⏳ Bekliyor |
| SPEC-04 | Kimlik Doğrulama | ⏳ Bekliyor |
| SPEC-05 | Duyurular | ⏳ Bekliyor |
| SPEC-06 | Dosya Servisi | ⏳ Bekliyor |
| SPEC-12 | Aidat Takibi | ⏳ Bekliyor |
| SPEC-13 | Oylama & Anket | ⏳ Bekliyor |

### Faz 2 — Topluluk & İletişim
| Spec | Modül | Durum |
|------|-------|-------|
| SPEC-07 | Task Yönetimi | ⏳ Bekliyor |
| SPEC-08 | Mesajlaşma | ⏳ Bekliyor |
| SPEC-09 | Forum | ⏳ Bekliyor |
| SPEC-10 | Bildirim Servisi | ⏳ Bekliyor |
| SPEC-11 | Belgeler | ⏳ Bekliyor |

---

## Lisans

Bu proje Seferihisar Ekoköy Kooperatifi'ne aittir. Tüm hakları saklıdır.
