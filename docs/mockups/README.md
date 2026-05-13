# Mockup'lar

Asıl mockup dosyaları `frontend/mockups/` klasöründedir:

```
ekokoy-portal/
└── frontend/
    └── mockups/
        ├── anasayfa-v3.html   ← Kamuya açık ana sayfa
        └── portal-v2.html    ← Portal içi tasarım
```

## Nasıl Açılır?

Tarayıcıda doğrudan açabilirsin:
```bash
# Mac / Linux
open frontend/mockups/anasayfa-v3.html
open frontend/mockups/portal-v2.html

# Windows
start frontend/mockups/anasayfa-v3.html
```

Veya VS Code'da **Live Server** eklentisi ile.

## Ne İçeriyor?

### anasayfa-v3.html
- Hero bölümü (fotoğraf + slogan)
- Özellikler kartları
- Galeri
- İletişim formu
- Kamuya açık tasarım dili

### portal-v2.html
- Sol sidebar (rol bazlı menü)
- Üst header (bildirim, profil)
- Dashboard kartları (duyuru, talep, aidat, oylama)
- Renk paleti: yeşil `#2C5440`, terracotta `#C4612C`, krem `#F5F0E8`

## Claude Code ile Kullanım

```bash
# VS Code'da frontend/ klasörü açıkken:
"CLAUDE.md oku. mockups/portal-v2.html'deki 
sidebar yapısını components/common/Sidebar.tsx'e dönüştür."
```
