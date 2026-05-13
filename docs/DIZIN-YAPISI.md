# Ekoköy Portalı — Proje Dizin Yapısı

## Tam Yapı

```
ekokoy-portal/                          ← Git repo root
│
├── CLAUDE.md                           ← Genel proje özeti (kısa)
│
├── backend/                            ← Spring Boot projesi
│   ├── CLAUDE.md                       ← Backend kuralları (Claude Code için)
│   ├── specs/
│   │   ├── SPEC-01.md                  ← Kullanıcı Kaydı & Davet
│   │   ├── SPEC-02.md                  ← Roller & Yetki Matrisi
│   │   ├── SPEC-03.md                  ← Konut-Kullanıcı İlişkisi
│   │   ├── SPEC-04.md                  ← Kimlik Doğrulama
│   │   ├── SPEC-05.md                  ← Duyurular
│   │   ├── SPEC-06.md                  ← Ortak Dosya Servisi
│   │   ├── SPEC-07.md                  ← Task Yönetimi
│   │   ├── SPEC-08.md                  ← Mesajlaşma
│   │   ├── SPEC-09.md                  ← Forum
│   │   ├── SPEC-10.md                  ← Bildirim Servisi
│   │   ├── SPEC-11.md                  ← Belgeler
│   │   ├── SPEC-12.md                  ← Aidat Takibi
│   │   └── SPEC-13.md                  ← Oylama & Anket
│   ├── pom.xml
│   ├── .env.example
│   └── src/
│       └── main/
│           ├── java/com/ekokoy/
│           │   ├── config/             ← SecurityConfig, JwtConfig, MinioConfig
│           │   ├── common/             ← ApiResponse, GlobalExceptionHandler, JwtUtil
│           │   ├── user/               ← SPEC-01, 02, 03, 04
│           │   │   ├── controller/
│           │   │   ├── service/
│           │   │   ├── repository/
│           │   │   ├── entity/
│           │   │   └── dto/
│           │   ├── announcement/       ← SPEC-05
│           │   ├── storage/            ← SPEC-06
│           │   ├── task/               ← SPEC-07
│           │   ├── message/            ← SPEC-08
│           │   ├── forum/              ← SPEC-09
│           │   ├── notification/       ← SPEC-10
│           │   ├── document/           ← SPEC-11
│           │   ├── dues/               ← SPEC-12
│           │   └── poll/               ← SPEC-13
│           └── resources/
│               ├── application.yml
│               ├── application-dev.yml
│               ├── application-prod.yml
│               └── db/migration/       ← Flyway V1__... V2__... vb.
│
└── frontend/                           ← React (Vite) projesi
    ├── CLAUDE.md                       ← Frontend kuralları (Claude Code için)
    ├── package.json
    ├── vite.config.ts
    ├── tsconfig.json
    ├── tailwind.config.ts
    ├── .env.example
    └── src/
        ├── main.tsx
        ├── App.tsx
        ├── api/                        ← Axios instance + modül bazlı API fonksiyonları
        │   ├── client.ts
        │   ├── auth.ts
        │   ├── announcements.ts
        │   ├── tasks.ts
        │   └── ...
        ├── components/
        │   ├── ui/                     ← Shadcn bileşenleri (otomatik üretilir)
        │   └── common/                 ← Header, Sidebar, Layout, ProtectedRoute
        ├── features/                   ← Her modül kendi klasöründe
        │   ├── auth/
        │   │   ├── LoginPage.tsx
        │   │   ├── ForgotPasswordPage.tsx
        │   │   └── useAuth.ts
        │   ├── dashboard/
        │   ├── announcements/
        │   ├── tasks/
        │   ├── messages/
        │   ├── forum/
        │   ├── documents/
        │   ├── dues/
        │   ├── polls/
        │   └── admin/                  ← Admin paneli sayfaları
        ├── stores/                     ← Zustand store'lar
        │   ├── authStore.ts
        │   └── uiStore.ts
        ├── hooks/                      ← Ortak custom hook'lar
        ├── types/                      ← TypeScript tip tanımları
        │   ├── api.types.ts
        │   ├── user.types.ts
        │   └── ...
        └── lib/
            ├── utils.ts                ← cn() ve yardımcı fonksiyonlar
            └── constants.ts
```

---

## Kurulum Adımları

### 1. Repo oluştur
```bash
mkdir ekokoy-portal && cd ekokoy-portal
git init
```

### 2. CLAUDE dosyalarını kopyala
```bash
# Root CLAUDE.md (genel özet)
cp CLAUDE-ROOT.md ekokoy-portal/CLAUDE.md

# Backend
mkdir -p ekokoy-portal/backend/specs
cp backend/CLAUDE.md ekokoy-portal/backend/CLAUDE.md
cp specs/SPEC-*.md ekokoy-portal/backend/specs/

# Frontend
mkdir ekokoy-portal/frontend
cp frontend/CLAUDE.md ekokoy-portal/frontend/CLAUDE.md
```

### 3. Backend — Spring Initializr
```
https://start.spring.io

Group:    com.ekokoy
Artifact: ekokoy-portal
Java:     25
Build:    Maven

Bağımlılıklar:
✓ Spring Web
✓ Spring Security
✓ Spring Data JPA
✓ PostgreSQL Driver
✓ Flyway Migration
✓ Spring Mail
✓ Validation
✓ Lombok (opsiyonel, CLAUDE.md'de Lombok yok deniyor — eklemeyebilirsin)
```

Oluşturulan klasörü `backend/` içine aç.

### 4. Frontend — Vite
```bash
cd ekokoy-portal
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install

# Temel bağımlılıklar
npm install axios @tanstack/react-query zustand react-router-dom react-hook-form zod

# Tailwind
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p

# Shadcn/ui
npx shadcn@latest init
```

### 5. VS Code Workspace
```bash
# Tüm projeyi tek pencerede açmak için
code ekokoy-portal/

# Sadece backend üzerinde çalışmak için (Claude Code'un backend CLAUDE.md'yi görmesi için)
code ekokoy-portal/backend/

# Sadece frontend
code ekokoy-portal/frontend/
```

---

## Claude Code Komutları

```bash
# Backend — yeni spec implement et
# VS Code'da backend/ klasörü açıkken:
"CLAUDE.md ve specs/SPEC-01.md oku. Kullanıcı kaydı modülünü implement et."

# Frontend — yeni sayfa ekle
# VS Code'da frontend/ klasörü açıkken:
"CLAUDE.md oku. features/announcements/ klasörüne duyuru listesi sayfasını ekle."

# Frontend — API bağla
"CLAUDE.md oku. backend/specs/SPEC-07.md'deki task endpoint'lerini api/tasks.ts'e ekle ve TaskListPage.tsx'te kullan."
```

---

## .gitignore (root)
```
# Backend
backend/target/
backend/.env
backend/src/main/resources/application-prod.yml

# Frontend
frontend/node_modules/
frontend/dist/
frontend/.env
frontend/.env.local

# IDE
.idea/
.vscode/settings.json
*.iml
```
