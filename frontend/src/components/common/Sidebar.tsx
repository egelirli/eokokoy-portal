import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Megaphone,
  Wrench,
  CreditCard,
  MessageCircle,
  FileText,
  MessageSquare,
  Users,
  Home,
  BarChart3,
  Vote,
  LogOut,
  Bell,
  UserPlus,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/stores/authStore';
import { ADMIN_ROLES } from '@/lib/constants';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';

interface NavItem {
  icon: React.ReactNode;
  label: string;
  to: string;
  badge?: number;
}

function getInitials(firstName: string, lastName: string): string {
  return `${firstName[0] ?? ''}${lastName[0] ?? ''}`.toUpperCase();
}

function NavItemRow({ item }: { item: NavItem }) {
  return (
    <NavLink
      to={item.to}
      className={({ isActive }) =>
        cn(
          'flex items-center gap-2 px-2.5 py-2 rounded-lg text-xs transition-all cursor-pointer',
          'text-white/55 hover:bg-white/7 hover:text-white/85',
          isActive && 'bg-white/13 text-white font-semibold',
        )
      }
    >
      <span className="w-4 h-4 shrink-0 opacity-70 [.active_&]:opacity-100">{item.icon}</span>
      <span className="flex-1">{item.label}</span>
      {item.badge !== undefined && item.badge > 0 && (
        <span className="ml-auto bg-secondary text-white text-[9px] font-bold px-1.5 py-px rounded-full min-w-[16px] text-center">
          {item.badge}
        </span>
      )}
    </NavLink>
  );
}

export function Sidebar() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const isAdmin = user?.roles.some((r) => (ADMIN_ROLES as readonly string[]).includes(r)) ?? false;

  const residentNav: NavItem[] = [
    { icon: <LayoutDashboard size={16} />, label: 'Dashboard', to: '/dashboard' },
    { icon: <Megaphone size={16} />, label: 'Duyurular', to: '/announcements' },
    { icon: <Wrench size={16} />, label: 'Talepler', to: '/tasks' },
    { icon: <CreditCard size={16} />, label: 'Aidat', to: '/dues' },
    { icon: <MessageCircle size={16} />, label: 'Mesajlar', to: '/messages' },
    { icon: <MessageSquare size={16} />, label: 'Forum', to: '/forum' },
    { icon: <FileText size={16} />, label: 'Belgeler', to: '/documents' },
    { icon: <Vote size={16} />, label: 'Anketler', to: '/polls' },
  ];

  const adminNav: NavItem[] = [
    { icon: <BarChart3 size={16} />, label: 'Admin Özeti', to: '/admin/dashboard' },
    { icon: <Users size={16} />, label: 'Kullanıcılar', to: '/admin/users' },
    { icon: <Home size={16} />, label: 'Konutlar', to: '/admin/properties' },
    { icon: <Megaphone size={16} />, label: 'Duyuru Ekle', to: '/admin/announcements/new' },
    { icon: <Wrench size={16} />, label: 'Tüm Talepler', to: '/admin/tasks' },
    { icon: <CreditCard size={16} />, label: 'Aidat İçe Aktar', to: '/admin/dues/import' },
    { icon: <Vote size={16} />, label: 'Anket Oluştur', to: '/admin/polls/new' },
    { icon: <UserPlus size={16} />, label: 'Davetiye', to: '/admin/invite' },
  ];

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <aside className="w-[210px] bg-[#1E3628] flex flex-col shrink-0 overflow-y-auto h-screen sticky top-0">
      {/* Logo */}
      <div className="px-4 py-5 border-b border-white/10">
        <div className="font-serif text-[15px] text-white leading-tight">Ekoköy Portalı</div>
        <div className="text-[10px] text-white/45 tracking-wide mt-0.5">Seferihisar · 94 Hane</div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-2.5 py-2.5 flex flex-col gap-0.5">
        <p className="text-[9px] text-white/30 tracking-widest uppercase px-2 py-1.5 mt-1">
          Menü
        </p>
        {residentNav.map((item) => (
          <NavItemRow key={item.to} item={item} />
        ))}

        {isAdmin && (
          <>
            <p className="text-[9px] text-white/30 tracking-widest uppercase px-2 py-1.5 mt-3">
              Yönetim
            </p>
            {adminNav.map((item) => (
              <NavItemRow key={item.to} item={item} />
            ))}
          </>
        )}

        <div className="mt-2">
          <NavItemRow
            item={{ icon: <Bell size={16} />, label: 'Bildirimler', to: '/notifications' }}
          />
        </div>
      </nav>

      {/* User footer */}
      {user && (
        <div className="px-3 py-3 border-t border-white/10 flex items-center gap-2">
          <Avatar className="h-7 w-7 shrink-0">
            {user.avatarUrl && <AvatarImage src={user.avatarUrl} />}
            <AvatarFallback className="bg-primary/70 text-white text-[10px]">
              {getInitials(user.firstName, user.lastName)}
            </AvatarFallback>
          </Avatar>
          <div className="flex-1 min-w-0">
            <div className="text-[11.5px] text-white/75 font-semibold truncate">
              {user.firstName} {user.lastName}
            </div>
            <div className="text-[10px] text-white/35 truncate">{user.roles[0]}</div>
          </div>
          <button
            onClick={handleLogout}
            className="text-white/35 hover:text-white/70 transition-colors shrink-0"
            title="Çıkış yap"
          >
            <LogOut size={14} />
          </button>
        </div>
      )}
    </aside>
  );
}
