# Ekoköy Portalı — Monorepo

94 haneli kooperatif yönetim portalı. Seferihisar / İzmir.

## Yapı
```
ekokoy-portal/
├── CLAUDE.md          ← bu dosya (genel özet)
├── backend/           ← Spring Boot 4.0 + PostgreSQL
│   ├── CLAUDE.md      ← backend kuralları
│   ├── specs/         ← SPEC-01.md → SPEC-13.md
│   └── pom.xml
└── frontend/          ← React (Vite) + TypeScript + Shadcn/ui
    ├── CLAUDE.md      ← frontend kuralları
    └── package.json
```

## API
- Backend: `http://localhost:8080/api/v1`
- Frontend: `http://localhost:5173`

## Detaylar
- Backend kuralları → `backend/CLAUDE.md`
- Frontend kuralları → `frontend/CLAUDE.md`
- Spec'ler → `backend/specs/SPEC-XX.md`

## Claude Code Kullanımı
```bash
# Backend üzerinde çalışırken VS Code'da aç:
code ekokoy-portal/backend

# Frontend üzerinde çalışırken:
code ekokoy-portal/frontend
```
