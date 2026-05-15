import { useState } from 'react';
import { Link } from 'react-router-dom';

const C = {
  green: '#2C5440',
  greenL: '#EAF2ED',
  greenLL: '#F2F7F3',
  terra: '#C4612C',
  terraL: '#FAF0EB',
  t1: '#1A1A17',
  t2: '#55554D',
  t3: '#9A9991',
  bdr: 'rgba(30,54,40,.10)',
  cream: '#F5F2EA',
  white: '#FFFFFF',
  dark: '#1A1A17',
} as const;

const serif = "'DM Serif Display', Georgia, serif";

function LogoIcon() {
  return (
    <div style={{ width: 36, height: 36, background: C.green, borderRadius: 9, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
        <path d="M12 3C8.5 3 5 6.5 5 10.5c0 2.5 1.2 4.5 3 5.8V18h8v-1.7c1.8-1.3 3-3.3 3-5.8C19 6.5 15.5 3 12 3z" fill="white" opacity="0.9"/>
        <rect x="9" y="18" width="6" height="2.5" rx="1" fill="white" opacity="0.7"/>
      </svg>
    </div>
  );
}

function PhotoPlaceholder({ label, size = 'sm' }: { label?: string; size?: 'sm' | 'lg' }) {
  const iconSize = size === 'lg' ? 56 : 32;
  return (
    <div style={{ width: '100%', height: '100%', background: C.greenL, display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 6 }}>
      <svg width={iconSize} height={iconSize} viewBox="0 0 48 48" fill="none" opacity={0.25}>
        <rect x="4" y="10" width="40" height="28" rx="4" stroke={C.green} strokeWidth="1.5" fill="none"/>
        <circle cx="24" cy="24" r="8" stroke={C.green} strokeWidth="1.5" fill="none"/>
        {size === 'lg' && <path d="M4 18h40" stroke={C.green} strokeWidth="1" opacity="0.5"/>}
      </svg>
      {label && <div style={{ fontSize: 11, color: C.green, fontWeight: 700, opacity: 0.5 }}>{label}</div>}
    </div>
  );
}

const valueCards = [
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
        <path d="M11 3C8 3 5 6.5 5 10c0 2.2 1 4 2.8 5.2V17h6.4v-.8C16 15 17 13.2 17 10c0-3.5-3-7-6-7z" stroke={C.green} strokeWidth="1.5" fill="none"/>
        <rect x="10" y="17" width="2" height="2" rx="1" fill={C.green}/>
      </svg>
    ),
    title: 'Ekolojik mimari',
    desc: 'Doğa dostu malzeme ve pasif enerji tasarımıyla inşa edilmiş, çevreye minimum yük bindiren yapılar.',
  },
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
        <circle cx="8" cy="8" r="3.5" stroke={C.green} strokeWidth="1.5" fill="none"/>
        <circle cx="14" cy="8" r="3.5" stroke={C.green} strokeWidth="1.5" fill="none"/>
        <path d="M2.5 19c0-3.3 2.5-6 5.5-6h6c3 0 5.5 2.7 5.5 6" stroke={C.green} strokeWidth="1.5" fill="none"/>
      </svg>
    ),
    title: 'Ortak yaşam alanları',
    desc: 'Paylaşımlı bahçe, atölye, amfi tiyatro ve toplantı salonuyla güçlü bir komşuluk kültürü.',
  },
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
        <rect x="3" y="11" width="4.5" height="8" rx="1.5" stroke={C.green} strokeWidth="1.5" fill="none"/>
        <rect x="9" y="7" width="4.5" height="12" rx="1.5" stroke={C.green} strokeWidth="1.5" fill="none"/>
        <rect x="15" y="3" width="4.5" height="16" rx="1.5" stroke={C.green} strokeWidth="1.5" fill="none"/>
      </svg>
    ),
    title: 'Kooperatif yönetim',
    desc: '94 hanenin ortak kararlarla yönettiği şeffaf ve demokratik bir kooperatif yapısı.',
  },
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
        <circle cx="11" cy="11" r="8" stroke={C.green} strokeWidth="1.5" fill="none"/>
        <path d="M11 3v3.5M11 15.5V19M3 11h3.5M15.5 11H19" stroke={C.green} strokeWidth="1.5"/>
      </svg>
    ),
    title: "Seferihisar'da",
    desc: "İzmir'e 40 dk, Ege kıyılarına 5 km mesafede. Şehrin tüm olanaklarına yakın, doğanın içinde.",
  },
];

const newsItems = [
  { tag: 'Etkinlik', title: 'Bahar bahçe çalışması — 20 Nisan Pazar', date: '14 Nisan 2026' },
  { tag: 'Proje', title: "Güneş paneli kurulumu Mayıs'ta başlıyor", date: '8 Nisan 2026' },
  { tag: 'Topluluk', title: 'Organik pazar her Cumartesi kurulmaya başlıyor', date: '2 Nisan 2026' },
];

const eventItems = [
  { day: '20', month: 'Nis', title: 'Kompost atölyesi', meta: 'Ortak bahçe · 10:00–12:00 · Tüm sakinler', badgeType: 'open', badge: 'Açık katılım' },
  { day: '26', month: 'Nis', title: 'Sinema gecesi — "Toprak Ana"', meta: 'Amfi tiyatro · 20:00 · Piknik minderi getirin', badgeType: 'open', badge: 'Açık katılım' },
  { day: '2', month: 'May', title: 'Genel kurul — 2026 bütçe görüşmesi', meta: 'Ortak salon · 18:00 · Sadece kayıtlı sakinler', badgeType: 'must', badge: 'Üye girişi gerekli' },
];

const heroStats = [
  { num: '94', lbl: 'Bağımsız bölüm' },
  { num: '2018', lbl: 'Kuruluş yılı' },
  { num: '5.000', lbl: 'm² ortak alan' },
  { num: '40 dk', lbl: "İzmir'e mesafe" },
];

const barStats = [
  { num: '94', lbl: 'Bağımsız bölüm' },
  { num: '187', lbl: 'Aktif sakin' },
  { num: '5.000 m²', lbl: 'Ortak alan' },
  { num: '8 yıl', lbl: 'Kuruluş' },
];

const distances = [
  'İzmir Merkez — 40 dk (40 km)',
  'Seferihisar İlçe Merkezi — 5 dk (4 km)',
  'Sığacık Koyu — 10 dk (7 km)',
  'İzmir Havalimanı — 50 dk (55 km)',
];

export function HomePage() {
  const [mode, setMode] = useState<'visitor' | 'member'>('visitor');
  const isMember = mode === 'member';

  return (
    <div style={{ fontFamily: "'Inter', system-ui, sans-serif", background: C.cream, color: C.t1, minHeight: '100vh' }}>

      {/* ── Navbar ── */}
      <nav style={{ background: C.white, borderBottom: `1px solid ${C.bdr}`, padding: '0 40px', display: 'flex', alignItems: 'center', height: 62, position: 'sticky', top: 0, zIndex: 100, gap: 0 }}>
        <a href="/" style={{ display: 'flex', alignItems: 'center', gap: 11, marginRight: 'auto', textDecoration: 'none' }}>
          <LogoIcon />
          <div>
            <div style={{ fontFamily: serif, fontSize: 15, color: C.t1, lineHeight: 1.2 }}>Ekoköy Portalı</div>
            <div style={{ fontSize: 10, color: C.t3 }}>Seferihisar · Ekolojik Yaşam</div>
          </div>
        </a>

        <div style={{ display: 'flex', gap: 2, marginRight: 16 }}>
          {['Ana sayfa', 'Hakkımızda', 'Galeri', 'Haberler', 'Etkinlikler', 'İletişim'].map(label => (
            <a key={label} href="#" style={{ padding: '7px 14px', borderRadius: 8, fontSize: 13, color: label === 'Ana sayfa' ? C.green : C.t2, fontWeight: label === 'Ana sayfa' ? 700 : 400, textDecoration: 'none' }}>
              {label}
            </a>
          ))}
        </div>

        <div style={{ display: 'flex', gap: 5, marginRight: 10 }}>
          {(['visitor', 'member'] as const).map(m => (
            <button key={m} onClick={() => setMode(m)} style={{ padding: '6px 13px', borderRadius: 20, fontSize: 12, fontWeight: 700, cursor: 'pointer', border: `1px solid ${mode === m ? 'rgba(44,84,64,.2)' : C.bdr}`, background: mode === m ? C.greenL : 'transparent', color: mode === m ? C.green : C.t3, transition: 'all .15s', fontFamily: 'inherit' }}>
              {m === 'visitor' ? 'Ziyaretçi' : 'Üye girişi'}
            </button>
          ))}
        </div>

        <Link to="/login" style={{ padding: '9px 20px', background: isMember ? C.terra : C.green, color: '#fff', borderRadius: 9, fontSize: 13, fontWeight: 700, textDecoration: 'none', whiteSpace: 'nowrap', transition: 'opacity .15s' }}>
          {isMember ? 'Dashboard →' : 'Giriş yap'}
        </Link>
      </nav>

      {/* ── Hero ── */}
      <section style={{ background: C.green, padding: '80px 40px 68px', textAlign: 'center', position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'relative', zIndex: 1 }}>
          <div style={{ display: 'inline-block', background: 'rgba(255,255,255,.14)', color: 'rgba(255,255,255,.82)', fontSize: 11.5, fontWeight: 700, letterSpacing: '.07em', textTransform: 'uppercase', padding: '6px 16px', borderRadius: 20, marginBottom: 22, border: '1px solid rgba(255,255,255,.18)' }}>
            Seferihisar · Ege · Ekolojik Yaşam
          </div>
          <h1 style={{ fontFamily: serif, fontSize: 46, color: '#fff', lineHeight: 1.12, marginBottom: 16, maxWidth: 560, marginLeft: 'auto', marginRight: 'auto' }}>
            Doğayla uyumlu,<br />komşulukla güçlenen<br />bir yaşam
          </h1>
          <p style={{ fontSize: 15.5, color: 'rgba(255,255,255,.7)', lineHeight: 1.7, maxWidth: 430, margin: '0 auto 36px' }}>
            Seferihisar'ın kalbinde 94 haneli ekolojik yaşam köyü. Ortak bahçeler, paylaşılan değerler ve Ege'nin huzuru.
          </p>
          <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
            <button style={{ padding: '13px 32px', background: '#fff', color: C.green, borderRadius: 10, fontSize: 14.5, fontWeight: 700, cursor: 'pointer', border: 'none', fontFamily: 'inherit', transition: 'opacity .15s' }}>
              Köyü keşfet
            </button>
            <Link to={isMember ? '/dashboard' : '/apply'} style={{ padding: '13px 32px', background: 'transparent', color: '#fff', borderRadius: 10, fontSize: 14.5, fontWeight: 700, border: '1.5px solid rgba(255,255,255,.32)', textDecoration: 'none', transition: 'border-color .15s' }}>
              {isMember ? "Dashboard'a git" : 'Başvuru yap'}
            </Link>
          </div>
          <div style={{ display: 'flex', justifyContent: 'center', marginTop: 52, borderTop: '1px solid rgba(255,255,255,.14)', paddingTop: 30, flexWrap: 'wrap' }}>
            {heroStats.map(({ num, lbl }, i) => (
              <div key={lbl} style={{ padding: '0 36px', textAlign: 'center', borderRight: i < heroStats.length - 1 ? '1px solid rgba(255,255,255,.14)' : 'none' }}>
                <div style={{ fontFamily: serif, fontSize: 32, color: '#fff', lineHeight: 1 }}>{num}</div>
                <div style={{ fontSize: 11, color: 'rgba(255,255,255,.5)', marginTop: 6, textTransform: 'uppercase', letterSpacing: '.05em' }}>{lbl}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Values ── */}
      <section style={{ padding: '64px 40px', background: C.white }}>
        <div style={{ textAlign: 'center', marginBottom: 40 }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: C.green, letterSpacing: '.09em', textTransform: 'uppercase', marginBottom: 10 }}>Neden ekolojik köy?</div>
          <div style={{ fontFamily: serif, fontSize: 32, color: C.t1, lineHeight: 1.18, marginBottom: 12 }}>Şehrin koşuşturmasından, Ege'nin dinginliğine</div>
          <p style={{ fontSize: 15, color: C.t2, lineHeight: 1.7, maxWidth: 540, margin: '0 auto' }}>Birlikte üretmek, birlikte karar vermek ve doğanın döngülerine uyumlu bir hayat.</p>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 16 }}>
          {valueCards.map(({ icon, title, desc }) => (
            <div key={title} style={{ background: C.white, border: `1px solid ${C.bdr}`, borderRadius: 16, padding: '24px 22px' }}>
              <div style={{ width: 44, height: 44, borderRadius: 11, background: C.greenL, display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16 }}>
                {icon}
              </div>
              <div style={{ fontSize: 15, fontWeight: 700, color: C.t1, marginBottom: 8 }}>{title}</div>
              <div style={{ fontSize: 13, color: C.t2, lineHeight: 1.65 }}>{desc}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ── Gallery ── */}
      <section style={{ padding: '64px 40px', background: C.cream }}>
        <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginBottom: 28, gap: 16, flexWrap: 'wrap' }}>
          <div>
            <div style={{ fontSize: 11, fontWeight: 700, color: C.green, letterSpacing: '.09em', textTransform: 'uppercase', marginBottom: 10 }}>Fotoğraflar</div>
            <div style={{ fontFamily: serif, fontSize: 32, color: C.t1, lineHeight: 1.18 }}>Köyden kareler</div>
          </div>
          <a href="#" style={{ fontSize: 13, fontWeight: 700, color: C.green, textDecoration: 'none' }}>Tüm galeri →</a>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr', height: 200, gap: 10 }}>
          <div style={{ borderRadius: 14, overflow: 'hidden' }}><PhotoPlaceholder label="Ana bahçe" size="lg" /></div>
          <div style={{ borderRadius: 14, overflow: 'hidden' }}><PhotoPlaceholder /></div>
          <div style={{ borderRadius: 14, overflow: 'hidden' }}><PhotoPlaceholder /></div>
        </div>
      </section>

      {/* ── News + Events ── */}
      <section style={{ padding: '64px 40px', background: C.white }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 48, alignItems: 'start' }}>
          <div>
            <div style={{ fontSize: 11, fontWeight: 700, color: C.green, letterSpacing: '.09em', textTransform: 'uppercase', marginBottom: 10 }}>Haberler</div>
            <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginBottom: 20 }}>
              <div style={{ fontFamily: serif, fontSize: 28, color: C.t1, lineHeight: 1.18 }}>Köyden son haberler</div>
              <a href="#" style={{ fontSize: 13, fontWeight: 700, color: C.green, textDecoration: 'none', whiteSpace: 'nowrap', marginLeft: 12 }}>Tümü →</a>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {newsItems.map(({ tag, title, date }) => (
                <div key={title} style={{ background: C.white, border: `1px solid ${C.bdr}`, borderRadius: 14, overflow: 'hidden', display: 'flex', cursor: 'pointer' }}>
                  <div style={{ width: 110, flexShrink: 0, background: C.greenL, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <svg width="28" height="28" viewBox="0 0 48 48" fill="none" opacity={0.3}>
                      <rect x="4" y="10" width="40" height="28" rx="4" stroke={C.green} strokeWidth="1.5" fill="none"/>
                      <circle cx="24" cy="24" r="8" stroke={C.green} strokeWidth="1.5" fill="none"/>
                    </svg>
                  </div>
                  <div style={{ padding: '14px 16px', flex: 1 }}>
                    <span style={{ display: 'inline-block', fontSize: 10.5, fontWeight: 700, color: C.green, background: C.greenL, padding: '3px 9px', borderRadius: 10, marginBottom: 7 }}>{tag}</span>
                    <div style={{ fontSize: 14, fontWeight: 700, color: C.t1, lineHeight: 1.35, marginBottom: 6 }}>{title}</div>
                    <div style={{ fontSize: 12, color: C.t3 }}>{date}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div>
            <div style={{ fontSize: 11, fontWeight: 700, color: C.green, letterSpacing: '.09em', textTransform: 'uppercase', marginBottom: 10 }}>Etkinlikler</div>
            <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginBottom: 20 }}>
              <div style={{ fontFamily: serif, fontSize: 28, color: C.t1, lineHeight: 1.18 }}>Yaklaşan etkinlikler</div>
              <a href="#" style={{ fontSize: 13, fontWeight: 700, color: C.green, textDecoration: 'none', whiteSpace: 'nowrap', marginLeft: 12 }}>Takvim →</a>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {eventItems.map(({ day, month, title, meta, badgeType, badge }) => (
                <div key={title} style={{ background: C.white, border: `1px solid ${C.bdr}`, borderRadius: 13, padding: '16px 18px', display: 'flex', gap: 16, alignItems: 'flex-start', cursor: 'pointer' }}>
                  <div style={{ textAlign: 'center', width: 50, flexShrink: 0, background: C.greenL, borderRadius: 11, padding: '9px 0' }}>
                    <div style={{ fontFamily: serif, fontSize: 26, color: C.green, lineHeight: 1 }}>{day}</div>
                    <div style={{ fontSize: 10, color: C.green, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '.05em' }}>{month}</div>
                  </div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: 14.5, fontWeight: 700, color: C.t1, marginBottom: 4 }}>{title}</div>
                    <div style={{ fontSize: 12.5, color: C.t3 }}>{meta}</div>
                    <span style={{ fontSize: 10.5, fontWeight: 700, padding: '3px 9px', borderRadius: 10, display: 'inline-block', marginTop: 8, background: badgeType === 'open' ? C.greenL : '#FEF5E4', color: badgeType === 'open' ? C.green : '#7A4410' }}>
                      {badge}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ── Stats bar ── */}
      <section style={{ background: C.green, padding: '52px 40px' }}>
        <div style={{ display: 'flex', justifyContent: 'center', maxWidth: 760, margin: '0 auto', flexWrap: 'wrap' }}>
          {barStats.map(({ num, lbl }, i) => (
            <div key={lbl} style={{ flex: 1, minWidth: 140, textAlign: 'center', borderRight: i < barStats.length - 1 ? '1px solid rgba(255,255,255,.14)' : 'none', padding: '0 28px' }}>
              <div style={{ fontFamily: serif, fontSize: 38, color: '#fff', lineHeight: 1 }}>{num}</div>
              <div style={{ fontSize: 12, color: 'rgba(255,255,255,.5)', marginTop: 7, textTransform: 'uppercase', letterSpacing: '.04em' }}>{lbl}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ── Location ── */}
      <section style={{ padding: '64px 40px', background: C.white }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 48, alignItems: 'start' }}>
          <div>
            <div style={{ fontSize: 11, fontWeight: 700, color: C.green, letterSpacing: '.09em', textTransform: 'uppercase', marginBottom: 10 }}>Konum</div>
            <div style={{ fontFamily: serif, fontSize: 32, color: C.t1, lineHeight: 1.18, marginBottom: 12 }}>Seferihisar'ın kalbinde</div>
            <p style={{ fontSize: 14.5, color: C.t2, lineHeight: 1.75, marginBottom: 20 }}>
              İzmir'e 40 dakika, Sığacık Koyuna 5 kilometre mesafede. Şehrin tüm olanaklarına yakın ama kentin gürültüsünden uzak.
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {distances.map(text => (
                <div key={text} style={{ display: 'flex', alignItems: 'center', gap: 11, fontSize: 13.5, color: C.t2 }}>
                  <span style={{ width: 9, height: 9, borderRadius: '50%', background: C.green, flexShrink: 0, display: 'block' }} />
                  {text}
                </div>
              ))}
            </div>
          </div>
          <div>
            <div style={{ background: C.greenL, borderRadius: 18, height: 240, marginTop: 28, position: 'relative', overflow: 'hidden', border: `1px solid ${C.bdr}` }}>
              <svg width="100%" height="100%" viewBox="0 0 400 240" style={{ position: 'absolute', inset: 0, opacity: 0.12 }}>
                <path d="M0 120 Q50 90 100 120 Q150 150 200 120 Q250 90 300 120 Q350 150 400 120" stroke={C.green} strokeWidth="1.5" fill="none"/>
                <path d="M0 100 Q50 70 100 100 Q150 130 200 100 Q250 70 300 100 Q350 130 400 100" stroke={C.green} strokeWidth="1" fill="none"/>
                <path d="M0 140 Q50 110 100 140 Q150 170 200 140 Q250 110 300 140 Q350 170 400 140" stroke={C.green} strokeWidth="1" fill="none"/>
                <circle cx="200" cy="120" r="35" stroke={C.green} strokeWidth="1.5" fill="none"/>
                <circle cx="200" cy="120" r="70" stroke={C.green} strokeWidth="0.5" fill="none"/>
              </svg>
              <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)' }}>
                <div style={{ width: 34, height: 34, background: C.green, borderRadius: '50% 50% 50% 0', transform: 'rotate(-45deg)' }}>
                  <div style={{ width: 13, height: 13, background: '#fff', borderRadius: '50%', position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)' }} />
                </div>
              </div>
              <div style={{ background: C.white, border: `1px solid ${C.bdr}`, borderRadius: 9, padding: '8px 15px', position: 'absolute', fontSize: 13, fontWeight: 700, color: C.t1, top: 18, left: 18 }}>
                📍 Ekolojik Yaşam Köyü
              </div>
              <button style={{ position: 'absolute', bottom: 16, right: 16, background: C.green, color: '#fff', border: 'none', borderRadius: 8, padding: '8px 16px', fontSize: 12.5, fontWeight: 700, cursor: 'pointer', fontFamily: 'inherit' }}>
                Büyük haritada aç
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* ── CTA (hidden for members) ── */}
      {!isMember && (
        <section style={{ padding: '0 40px 64px' }}>
          <div style={{ background: C.terraL, borderRadius: 22, padding: '56px 40px', textAlign: 'center', border: '1px solid rgba(196,97,44,.14)' }}>
            <div style={{ fontSize: 11, fontWeight: 700, color: C.terra, letterSpacing: '.09em', textTransform: 'uppercase', marginBottom: 10 }}>Bir parça olun</div>
            <div style={{ fontFamily: serif, fontSize: 30, color: C.t1, marginBottom: 12, lineHeight: 1.2 }}>
              Köyümüzün bir parçası<br />olmak ister misiniz?
            </div>
            <p style={{ fontSize: 14.5, color: C.t2, lineHeight: 1.7, maxWidth: 460, margin: '0 auto 30px' }}>
              Başvurunuzu iletin, yönetim ekibimiz en kısa sürede sizinle iletişime geçsin. Tüm sorularınızı cevaplamaktan memnuniyet duyarız.
            </p>
            <Link to="/apply" style={{ padding: '14px 38px', background: C.terra, color: '#fff', borderRadius: 11, fontSize: 15, fontWeight: 700, textDecoration: 'none', display: 'inline-block' }}>
              Başvuru formunu doldur
            </Link>
          </div>
        </section>
      )}

      {/* ── Footer ── */}
      <footer style={{ background: C.dark, padding: '52px 40px 28px', marginTop: isMember ? 40 : 0 }}>
        <div style={{ display: 'grid', gridTemplateColumns: '2.2fr 1fr 1fr 1fr', gap: 36, marginBottom: 36 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
              <LogoIcon />
              <div style={{ fontFamily: serif, fontSize: 17, color: '#fff' }}>Ekoköy Portalı</div>
            </div>
            <div style={{ fontSize: 13, color: 'rgba(255,255,255,.38)', lineHeight: 1.65, maxWidth: 220 }}>
              Seferihisar'da doğayla uyumlu, komşulukla güçlenen bir yaşam topluluğu.
            </div>
            <div style={{ display: 'flex', gap: 8, marginTop: 18 }}>
              {[
                <svg key="ig" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.5)" strokeWidth="1.5"><rect x="2" y="2" width="20" height="20" rx="5"/><circle cx="12" cy="12" r="4"/><circle cx="17.5" cy="6.5" r="1" fill="rgba(255,255,255,.5)" stroke="none"/></svg>,
                <svg key="fb" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.5)" strokeWidth="1.5"><path d="M18 2h-3a5 5 0 00-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 011-1h3z"/></svg>,
                <svg key="yt" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,.5)" strokeWidth="1.5"><path d="M22.54 6.42a2.78 2.78 0 00-1.95-1.96C18.88 4 12 4 12 4s-6.88 0-8.59.46A2.78 2.78 0 001.46 6.42 29 29 0 001 12a29 29 0 00.46 5.58 2.78 2.78 0 001.95 1.96C5.12 20 12 20 12 20s6.88 0 8.59-.46a2.78 2.78 0 001.95-1.96A29 29 0 0023 12a29 29 0 00-.46-5.58z"/><polygon points="9.75 15.02 15.5 12 9.75 8.98 9.75 15.02" fill="rgba(255,255,255,.5)" stroke="none"/></svg>,
              ].map((icon, i) => (
                <div key={i} style={{ width: 30, height: 30, borderRadius: 7, background: 'rgba(255,255,255,.08)', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
                  {icon}
                </div>
              ))}
            </div>
          </div>

          <div>
            <div style={{ fontSize: 10.5, fontWeight: 700, color: 'rgba(255,255,255,.38)', letterSpacing: '.08em', textTransform: 'uppercase', marginBottom: 14 }}>Sayfalar</div>
            {['Ana sayfa', 'Hakkımızda', 'Galeri', 'Haberler', 'Etkinlikler', 'Sık sorulanlar'].map(label => (
              <a key={label} href="#" style={{ display: 'block', fontSize: 13.5, color: 'rgba(255,255,255,.58)', marginBottom: 8, textDecoration: 'none' }}>{label}</a>
            ))}
          </div>

          <div>
            <div style={{ fontSize: 10.5, fontWeight: 700, color: 'rgba(255,255,255,.38)', letterSpacing: '.08em', textTransform: 'uppercase', marginBottom: 14 }}>Topluluk</div>
            {[
              { label: 'Üye girişi', href: '/login' },
              { label: 'Başvuru yap', href: '/apply' },
              { label: 'Kooperatif', href: '#' },
              { label: 'Yönetim kurulu', href: '#' },
              { label: 'Gizlilik politikası', href: '#' },
            ].map(({ label, href }) => (
              <a key={label} href={href} style={{ display: 'block', fontSize: 13.5, color: 'rgba(255,255,255,.58)', marginBottom: 8, textDecoration: 'none' }}>{label}</a>
            ))}
          </div>

          <div>
            <div style={{ fontSize: 10.5, fontWeight: 700, color: 'rgba(255,255,255,.38)', letterSpacing: '.08em', textTransform: 'uppercase', marginBottom: 14 }}>İletişim</div>
            <a href="mailto:info@ekoyasam.com" style={{ display: 'block', fontSize: 13.5, color: 'rgba(255,255,255,.58)', marginBottom: 8, textDecoration: 'none' }}>info@ekoyasam.com</a>
            <a href="tel:+902320000000" style={{ display: 'block', fontSize: 13.5, color: 'rgba(255,255,255,.58)', marginBottom: 8, textDecoration: 'none' }}>0232 xxx xx xx</a>
            <a href="#" style={{ display: 'block', fontSize: 13.5, color: 'rgba(255,255,255,.58)', marginBottom: 8, textDecoration: 'none' }}>Seferihisar / İzmir</a>
            <a href="#" style={{ marginTop: 12, display: 'inline-block', padding: '8px 16px', background: 'rgba(255,255,255,.08)', borderRadius: 8, color: 'rgba(255,255,255,.7)', fontSize: 12.5, textDecoration: 'none' }}>İletişim formu →</a>
          </div>
        </div>

        <div style={{ borderTop: '1px solid rgba(255,255,255,.08)', paddingTop: 20, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
          <span style={{ fontSize: 12, color: 'rgba(255,255,255,.28)' }}>© 2026 Ekolojik Yaşam Köyü Kooperatifi. Tüm hakları saklıdır.</span>
          <span style={{ fontSize: 12, color: 'rgba(255,255,255,.28)' }}>Gizlilik · Çerez politikası · KVKK</span>
        </div>
      </footer>
    </div>
  );
}
