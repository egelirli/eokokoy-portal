// Henüz implement edilmemiş sayfalar — her feature kendi klasörüne taşınacak

function ComingSoon({ title }: { title: string }) {
  return (
    <div className="flex flex-col items-center justify-center p-6 py-24 text-center">
      <div className="mb-4 text-4xl">🔧</div>
      <h1 className="text-xl font-bold text-foreground">{title}</h1>
      <p className="mt-2 text-sm text-muted-foreground">Bu sayfa yakında tamamlanacak.</p>
    </div>
  );
}

export function ProfilePage() { return <ComingSoon title="Profilim" />; }
export function SettingsPage() { return <ComingSoon title="Ayarlar" />; }
export function NotificationsPage() { return <ComingSoon title="Bildirimler" />; }
export function ForgotPasswordPage() { return <ComingSoon title="Şifremi Unuttum" />; }
export function ResetPasswordPage() { return <ComingSoon title="Şifre Sıfırla" />; }
export function ApplyPage() { return <ComingSoon title="Başvuru" />; }
export function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center p-6 py-24 text-center">
      <div className="mb-4 text-4xl">🔍</div>
      <h1 className="text-xl font-bold text-foreground">Sayfa bulunamadı</h1>
      <p className="mt-2 text-sm text-muted-foreground">Aradığınız sayfa mevcut değil.</p>
    </div>
  );
}

// Admin placeholders
export function AdminDashboardPage() { return <ComingSoon title="Admin — Özet" />; }
export function AdminAnnouncementNewPage() { return <ComingSoon title="Admin — Yeni Duyuru" />; }
export function AdminTasksPage() { return <ComingSoon title="Admin — Tüm Talepler" />; }
export function AdminDuesImportPage() { return <ComingSoon title="Admin — Aidat İçe Aktar" />; }
export function AdminPollNewPage() { return <ComingSoon title="Admin — Yeni Anket" />; }
export function AdminInvitePage() { return <ComingSoon title="Admin — Davetiye" />; }
