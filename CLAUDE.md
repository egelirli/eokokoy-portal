# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Ekoköy Portalı — Monorepo

94-unit cooperative management portal for Seferihisar Ekoköy, İzmir. Monorepo with Spring Boot 4.0 backend and React frontend.

Detailed rules are in sub-directories:
- **Backend rules** → `backend/CLAUDE.md` (build tool: Gradle Kotlin DSL)
- **Frontend rules** → `frontend/CLAUDE.md`
- **Feature specs** → `backend/specs/SPEC-01.md` through `SPEC-13.md`

## Commands

### Backend (`cd backend`)
```bash
# Run dev server
./gradlew bootRun

# Build
./gradlew build -x test

# Run all tests
./gradlew test

# Run single test class
./gradlew test --tests "com.ekokoy.user.UserServiceTest"

# Run single test method
./gradlew test --tests "com.ekokoy.user.UserServiceTest.should_return_user_when_valid_id"
```

### Frontend (`cd frontend`)
```bash
# Install dependencies
npm install

# Dev server (http://localhost:5173)
npm run dev

# Type check
npm run typecheck

# Lint
npm run lint

# Build
npm run build
```

## Architecture Overview

### Request Flow
```
Frontend (React) → api/ (Axios + interceptors) → Backend Controller → Service → Repository → PostgreSQL
                                                                                     ↓
                                                                              MinIO (files)
```

- All API calls go through `frontend/src/api/client.ts`, which handles JWT injection and 401→token-refresh automatically.
- Backend enforces `Controller → Service → Repository` layering strictly. Business logic lives only in `@Service` classes; controllers do HTTP mapping and DTO conversion only.
- Every endpoint requires `@PreAuthorize`. Public endpoints are explicitly whitelisted in `SecurityConfig`.

### Authentication & Authorization
- JWT access token (15 min) + refresh token (30 days, SHA-256 hashed in DB, rotated on use).
- JWT payload carries `roles`, `permissions`, and `property_ids` so the frontend can gate UI without extra API calls.
- Permission checks on the frontend use `useAuthStore().permissions.includes('PERMISSION_NAME')` with a `<ProtectedRoute>` wrapper.
- 7 roles in hierarchy: `SUPER_ADMIN > YONETIM_KURULU > EV_SAHIBI = KIRACI = AILE_BIREYI = CALISAN > ZIYARETCI`.

### Data Model Conventions
- Primary keys: `UUID` everywhere.
- Soft delete: `is_deleted + deleted_at` on all deletable entities — no physical deletes.
- Dates: `TIMESTAMP WITH TIME ZONE` (UTC). Auditing via `@CreatedDate` / `@LastModifiedDate`.
- Database field names in English (`first_name`, `created_at`); Turkish domain terms allowed in Java/TS (`konut`, `sakin`, `aidat`).
- Flyway migrations: `V{n}__{description}.sql` — write once, never modify.
- Seed data: V1–V4 seed roles/permissions/mappings/SUPER_ADMIN; V5 seeds 94 properties.

### Frontend State Management
- **Server state** (API data): React Query only — no `useState` for fetched data.
- **Client state** (auth, UI prefs): Zustand stores in `src/stores/`.
- **Forms**: React Hook Form + Zod schema validation on every form.

### File Storage
- MinIO (self-hosted S3): four buckets — `ekokoy-public`, `ekokoy-private`, `ekokoy-admin`, `ekokoy-profiles`.
- Upload flow: backend issues presigned URL → client PUTs directly to MinIO → client calls confirm endpoint.
- Only confirmed files persist; unconfirmed files auto-delete after 24 h.

### Notifications
Three channels: in-app (sync DB write), email (async, 3 retries), WhatsApp (async, opt-in only). `announcement.urgent` events always use all channels regardless of user preferences.

### Module-to-Spec Mapping
| Package | Specs |
|---|---|
| `user/` | SPEC-01 (invitations), SPEC-02 (roles/permissions), SPEC-03 (property relations), SPEC-04 (auth) |
| `announcement/` | SPEC-05 |
| `storage/` | SPEC-06 |
| `task/` | SPEC-07 |
| `message/` | SPEC-08 |
| `forum/` | SPEC-09 |
| `notification/` | SPEC-10 |
| `document/` | SPEC-11 |
| `dues/` | SPEC-12 |
| `poll/` | SPEC-13 |

### API Base URLs
- Backend: `http://localhost:8080/api/v1`
- Frontend: `http://localhost:5173`
