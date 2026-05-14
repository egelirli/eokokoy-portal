import { Outlet, useLocation } from 'react-router-dom';
import { Bell, Settings } from 'lucide-react';
import { Sidebar } from './Sidebar';
import { useAuthStore } from '@/stores/authStore';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useNavigate } from 'react-router-dom';

const PAGE_TITLES: Record<string, string> = {
  '/dashboard': 'Dashboard',
  '/announcements': 'Duyurular',
  '/tasks': 'Talepler',
  '/dues': 'Aidat Bilgisi',
  '/messages': 'Mesajlar',
  '/forum': 'Forum',
  '/documents': 'Belgeler',
  '/polls': 'Anketler',
  '/profile': 'Profilim',
  '/settings': 'Ayarlar',
  '/notifications': 'Bildirimler',
  '/admin/users': 'Kullanıcılar',
  '/admin/properties': 'Konutlar',
  '/admin/tasks': 'Tüm Talepler',
  '/admin/dues/import': 'Aidat İçe Aktar',
  '/admin/announcements/new': 'Yeni Duyuru',
  '/admin/polls/new': 'Yeni Anket',
  '/admin/invite': 'Davetiye',
  '/admin/dashboard': 'Admin Özeti',
};

function getPageTitle(pathname: string): string {
  // Exact match first
  if (PAGE_TITLES[pathname]) return PAGE_TITLES[pathname];
  // Prefix match for dynamic segments
  const key = Object.keys(PAGE_TITLES)
    .sort((a, b) => b.length - a.length)
    .find((k) => pathname.startsWith(k));
  return key ? PAGE_TITLES[key] : 'Ekoköy Portalı';
}

function getInitials(firstName: string, lastName: string): string {
  return `${firstName[0] ?? ''}${lastName[0] ?? ''}`.toUpperCase();
}

export function Layout() {
  const { user, logout } = useAuthStore();
  const location = useLocation();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <div className="flex min-h-screen bg-background">
      <Sidebar />

      <div className="flex-1 flex flex-col min-w-0">
        {/* Top bar */}
        <header className="sticky top-0 z-10 flex items-center justify-between px-6 py-3 bg-background/95 backdrop-blur border-b border-border">
          <h1 className="font-serif text-xl text-foreground">
            {getPageTitle(location.pathname)}
          </h1>

          <div className="flex items-center gap-3">
            {/* Notification bell */}
            <button className="relative p-1.5 rounded-md text-muted-foreground hover:text-foreground hover:bg-accent transition-colors">
              <Bell size={18} />
            </button>

            {/* User dropdown */}
            {user && (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <button className="flex items-center gap-2 rounded-md px-2 py-1 hover:bg-accent transition-colors">
                    <Avatar className="h-7 w-7">
                      {user.avatarUrl && <AvatarImage src={user.avatarUrl} />}
                      <AvatarFallback className="text-[10px]">
                        {getInitials(user.firstName, user.lastName)}
                      </AvatarFallback>
                    </Avatar>
                    <span className="text-sm font-medium text-foreground hidden sm:block">
                      {user.firstName}
                    </span>
                  </button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-48">
                  <DropdownMenuItem onClick={() => navigate('/profile')}>
                    Profilim
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={() => navigate('/settings')}>
                    <Settings size={14} className="mr-2" />
                    Ayarlar
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem
                    onClick={handleLogout}
                    className="text-destructive focus:text-destructive"
                  >
                    Çıkış Yap
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            )}
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
