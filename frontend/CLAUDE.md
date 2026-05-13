# Ekoköy Portalı — Frontend Claude Code Rehberi

## Stack
```
Framework:  React 18 + Vite
Dil:        TypeScript (strict mode)
UI:         Shadcn/ui + Tailwind CSS
State:      Zustand
API:        Axios + React Query (TanStack Query v5)
Form:       React Hook Form + Zod
Router:     React Router v6
```

## Proje Yapısı
```
src/
├── main.tsx              # entry point
├── App.tsx               # router tanımları
├── api/                  # Axios instance + her modül için api fonksiyonları
│   ├── client.ts         # Axios config, interceptor, token refresh
│   ├── auth.ts
│   ├── announcements.ts
│   └── ...
├── components/
│   ├── ui/               # Shadcn bileşenleri (dokunma — otomatik üretilir)
│   └── common/           # Ortak bileşenler (Header, Sidebar, Layout vb.)
├── features/             # Her modül için ayrı klasör
│   ├── auth/
│   │   ├── LoginPage.tsx
│   │   ├── useAuth.ts
│   │   └── authStore.ts
│   ├── announcements/
│   ├── tasks/
│   └── ...
├── hooks/                # Ortak custom hook'lar
├── stores/               # Zustand store'lar
├── types/                # TypeScript tip tanımları
│   ├── api.types.ts      # API response tipleri
│   ├── user.types.ts
│   └── ...
└── lib/
    ├── utils.ts          # Shadcn utility (cn fonksiyonu)
    └── constants.ts
```

## Mimari Kurallar — ZORUNLU

**Feature bazlı organizasyon:** Her modül `features/` altında kendi klasörüne sahip.

**API katmanı:** Tüm API çağrıları `api/` klasöründe. Bileşen içinde direkt `axios.get(...)` yok.
```typescript
// ✅ Doğru
import { getAnnouncements } from '@/api/announcements';

// ❌ Yanlış
const res = await axios.get('/api/v1/announcements');
```

**React Query:** Tüm sunucu state'i React Query ile yönetilir. `useState` ile API verisi tutulmaz.
```typescript
const { data, isLoading, error } = useQuery({
  queryKey: ['announcements'],
  queryFn: getAnnouncements,
});
```

**Zustand:** Sadece istemci state'i için (auth, UI tercihleri). Sunucu verisi React Query'de.
```typescript
// authStore.ts
interface AuthStore {
  user: User | null;
  accessToken: string | null;
  setAuth: (user: User, token: string) => void;
  logout: () => void;
}
```

**Tip güvenliği:** `any` yasak. Her API response için tip tanımlanır.
```typescript
// types/api.types.ts
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
}

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  roles: string[];
  permissions: string[];
}
```

**Form:** React Hook Form + Zod şeması zorunlu.
```typescript
const schema = z.object({
  email: z.string().email('Geçerli e-posta girin'),
  password: z.string().min(8, 'En az 8 karakter'),
});
type FormData = z.infer<typeof schema>;
```

## API İstemci Kurulumu
```typescript
// api/client.ts
const client = axios.create({ baseURL: import.meta.env.VITE_API_URL });

// Token interceptor
client.interceptors.request.use(config => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 401 → token refresh
client.interceptors.response.use(null, async error => {
  if (error.response?.status === 401) { /* refresh logic */ }
});
```

## Routing Yapısı
```typescript
// Public rotalar (giriş gerektirmez)
/login, /forgot-password, /reset-password, /apply

// Protected rotalar (giriş zorunlu)
/dashboard
/announcements, /announcements/:id
/tasks, /tasks/:id
/messages
/forum, /forum/:categorySlug, /forum/topics/:id
/documents
/dues
/polls, /polls/:id
/profile, /settings

// Admin rotalar (YONETIM_KURULU veya SUPER_ADMIN)
/admin/users, /admin/properties, /admin/announcements/new
/admin/tasks, /admin/dues/import, /admin/polls/new
```

## Yetki Kontrolü
```typescript
// Kullanıcının izni var mı?
const { permissions } = useAuthStore();
const canCreate = permissions.includes('ANNOUNCEMENT_CREATE');

// Korumalı rota bileşeni
<ProtectedRoute requiredPermission="TASK_VIEW_ALL">
  <AdminTaskList />
</ProtectedRoute>
```

## Stil Kuralları
- Renkler: Tailwind + CSS değişkeni (`bg-primary`, `text-muted-foreground`)
- Shadcn bileşenlerini `components/ui/` dışında **düzenleme**
- Responsive: mobile-first (`sm:`, `md:`, `lg:`)
- Tema renkleri SPEC tasarımından: yeşil `#2C5440`, terracotta `#C4612C`

## Ortam Değişkenleri
```bash
VITE_API_URL=http://localhost:8080/api/v1
VITE_APP_NAME=Ekoköy Portalı
```

## Claude Code Kullanımı
```bash
# Yeni sayfa/feature ekle
"CLAUDE.md oku. features/announcements/ klasörüne duyuru listesi sayfası ekle."

# Bileşen düzelt
"CLAUDE.md oku. features/tasks/TaskDetailPage.tsx dosyasındaki [sorun]'u düzelt."

# API bağlantısı
"CLAUDE.md oku. backend SPEC-07'deki task endpoint'lerini api/tasks.ts'e ekle."
```


## Tasarım Referansı

`mockups/` klasöründe iki HTML mockup var — bileşen yazarken referans al:

| Dosya | İçerik |
|-------|--------|
| `mockups/anasayfa-v3.html` | Kamuya açık ana sayfa (hero, özellikler, galeri) |
| `mockups/portal-v2.html` | Portal içi tasarım (sidebar, header, dashboard kartları, rol bazlı menü) |

### Renk Paleti (Tailwind config'e ekle)
```typescript
// tailwind.config.ts
colors: {
  primary:    { DEFAULT: '#2C5440', light: '#3D6B52', dark: '#1A3A2A' },
  secondary:  { DEFAULT: '#C4612C', light: '#D4784A' },
  background: { DEFAULT: '#F5F0E8', card: '#FFFFFF' },
  muted:      { DEFAULT: '#6B7280' },
}
```

### Mockup Kullanım Örnekleri
```bash
# Sidebar bileşeni
"CLAUDE.md oku. mockups/portal-v2.html'deki sidebar yapısını 
components/common/Sidebar.tsx bileşenine dönüştür."

# Dashboard kartları
"CLAUDE.md oku. mockups/portal-v2.html'deki dashboard kartlarını 
features/dashboard/DashboardPage.tsx'e dönüştür."

# Ana sayfa
"CLAUDE.md oku. mockups/anasayfa-v3.html'i 
features/home/HomePage.tsx'e dönüştür."
```

## Notlar
- `cn()` utility her className birleştirmesinde kullanılır (Shadcn standardı)
- `import.meta.env` ile ortam değişkenleri — `process.env` değil
- Path alias: `@/` → `src/` (vite.config.ts'de tanımlı)
- Türkçe UI metinleri — i18n şu an yok
