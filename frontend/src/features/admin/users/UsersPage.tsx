import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { cn } from '@/lib/utils';
import { getAdminUsers } from '@/api/adminUsers';
import { SkeletonList, ErrorState, EmptyState, PageHeader } from '@/components/common/QueryStates';
import type { AdminUser, UserStatus } from '@/types/admin.types';

const STATUS_LABEL: Record<UserStatus, string> = {
  pending: 'Onay Bekliyor',
  active: 'Aktif',
  inactive: 'Pasif',
  suspended: 'Askıya Alındı',
};

const STATUS_BADGE: Record<UserStatus, string> = {
  pending: 'bg-amber-light text-amber',
  active: 'bg-green-100 text-green-700',
  inactive: 'bg-muted text-muted-foreground',
  suspended: 'bg-destructive/10 text-destructive',
};

type StatusFilter = 'all' | UserStatus;

const ROLE_SHORT: Record<string, string> = {
  SUPER_ADMIN: 'SA',
  YONETIM_KURULU: 'YK',
  EV_SAHIBI: 'ES',
  KIRACI: 'K',
  AILE_BIREYI: 'AB',
  CALISAN: 'Ç',
  ZIYARETCI: 'Z',
};

function Avatar({ user }: { user: AdminUser }) {
  const initials = `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
  return (
    <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
      {initials}
    </div>
  );
}

function UserRow({ user }: { user: AdminUser }) {
  const joinDate = new Date(user.createdAt).toLocaleDateString('tr-TR', {
    day: 'numeric', month: 'short', year: 'numeric',
  });

  return (
    <div className="flex items-center gap-4 rounded-xl border border-border bg-background-card px-4 py-3 hover:border-primary/30 transition-colors">
      <Avatar user={user} />

      <div className="min-w-0 flex-1">
        <p className="font-semibold text-foreground leading-snug">
          {user.firstName} {user.lastName}
        </p>
        <p className="text-xs text-muted-foreground truncate">{user.email}</p>
      </div>

      <div className="flex flex-wrap items-center gap-1.5">
        {user.roles.map(r => (
          <span key={r} className="rounded bg-primary/10 px-1.5 py-0.5 text-[10px] font-bold text-primary">
            {ROLE_SHORT[r] ?? r}
          </span>
        ))}
      </div>

      <div className="hidden text-xs text-muted-foreground sm:block">{joinDate}</div>

      <span className={cn('flex-shrink-0 rounded-full px-2.5 py-0.5 text-[11px] font-bold', STATUS_BADGE[user.status])}>
        {STATUS_LABEL[user.status]}
      </span>
    </div>
  );
}

export function UsersPage() {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');
  const [search, setSearch] = useState('');

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['admin-users'],
    queryFn: getAdminUsers,
    retry: 1,
  });

  const filtered = (data ?? []).filter(u => {
    const matchStatus = statusFilter === 'all' || u.status === statusFilter;
    const matchSearch = search.trim() === '' || (
      `${u.firstName} ${u.lastName} ${u.email}`.toLowerCase().includes(search.toLowerCase())
    );
    return matchStatus && matchSearch;
  });

  const statusFilters: { value: StatusFilter; label: string }[] = [
    { value: 'all', label: 'Tümü' },
    { value: 'active', label: 'Aktif' },
    { value: 'pending', label: 'Bekliyor' },
    { value: 'inactive', label: 'Pasif' },
    { value: 'suspended', label: 'Askıda' },
  ];

  return (
    <div className="space-y-6 p-6">
      <PageHeader
        title="Kullanıcılar"
        subtitle={data ? `${data.length} kayıtlı kullanıcı` : 'Tüm kullanıcılar'}
      />

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <input
          type="search"
          placeholder="İsim veya e-posta ara…"
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="flex-1 rounded-lg border border-border bg-background-card px-4 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none"
        />
        <div className="flex flex-wrap gap-2">
          {statusFilters.map(f => (
            <button
              key={f.value}
              onClick={() => setStatusFilter(f.value)}
              className={cn(
                'rounded-full px-3 py-1.5 text-xs font-semibold transition-colors',
                statusFilter === f.value
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-background-card border border-border text-muted-foreground hover:text-foreground',
              )}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {isLoading && <SkeletonList count={5} />}

      {isError && <ErrorState onRetry={() => refetch()} />}

      {!isLoading && !isError && filtered.length === 0 && (
        <EmptyState message="Bu kriterlere uyan kullanıcı bulunamadı." />
      )}

      {!isLoading && !isError && filtered.length > 0 && (
        <div className="space-y-2">
          {filtered.map(u => <UserRow key={u.id} user={u} />)}
        </div>
      )}
    </div>
  );
}
