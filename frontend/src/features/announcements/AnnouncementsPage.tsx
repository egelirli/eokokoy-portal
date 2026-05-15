import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { getAnnouncements } from '@/api/announcements';
import { SkeletonList, ErrorState, EmptyState, PageHeader } from '@/components/common/QueryStates';
import type { Announcement, AnnouncementPriority } from '@/types/announcement.types';

const PRIORITY_LABEL: Record<AnnouncementPriority, string> = {
  normal: 'Normal',
  important: 'Önemli',
  urgent: 'Acil',
};

const PRIORITY_BADGE: Record<AnnouncementPriority, string> = {
  normal: 'bg-muted text-muted-foreground',
  important: 'bg-amber-light text-amber',
  urgent: 'bg-destructive/10 text-destructive',
};

const PRIORITY_DOT: Record<AnnouncementPriority, string> = {
  normal: 'bg-muted-foreground',
  important: 'bg-amber',
  urgent: 'bg-destructive',
};

type Filter = 'all' | AnnouncementPriority;

function AnnouncementCard({ item }: { item: Announcement }) {
  const date = item.publishedAt
    ? new Date(item.publishedAt).toLocaleDateString('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' })
    : null;

  return (
    <Link
      to={`/announcements/${item.id}`}
      className="flex items-start gap-3 rounded-xl border border-border bg-background-card p-4 hover:border-primary/40 transition-colors"
    >
      <span className={cn('mt-1.5 h-2 w-2 flex-shrink-0 rounded-full', PRIORITY_DOT[item.priority])} />
      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-3">
          <p className="font-semibold text-foreground leading-snug line-clamp-2">{item.title}</p>
          <span className={cn('flex-shrink-0 rounded-full px-2 py-0.5 text-[10px] font-bold', PRIORITY_BADGE[item.priority])}>
            {PRIORITY_LABEL[item.priority]}
          </span>
        </div>
        <div className="mt-1.5 flex items-center gap-2 text-xs text-muted-foreground">
          {item.createdBy && (
            <span>{item.createdBy.firstName} {item.createdBy.lastName}</span>
          )}
          {date && <><span>·</span><span>{date}</span></>}
        </div>
      </div>
    </Link>
  );
}

export function AnnouncementsPage() {
  const [filter, setFilter] = useState<Filter>('all');

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['announcements'],
    queryFn: getAnnouncements,
    retry: 1,
  });

  const filtered = (data ?? []).filter(a => filter === 'all' || a.priority === filter);

  const filters: { value: Filter; label: string }[] = [
    { value: 'all', label: 'Tümü' },
    { value: 'urgent', label: 'Acil' },
    { value: 'important', label: 'Önemli' },
    { value: 'normal', label: 'Normal' },
  ];

  return (
    <div className="space-y-6 p-6">
      <PageHeader title="Duyurular" subtitle="Köy yönetiminden güncel duyurular" />

      <div className="flex flex-wrap gap-2">
        {filters.map(f => (
          <button
            key={f.value}
            onClick={() => setFilter(f.value)}
            className={cn(
              'rounded-full px-4 py-1.5 text-sm font-semibold transition-colors',
              filter === f.value
                ? 'bg-primary text-primary-foreground'
                : 'bg-background-card border border-border text-muted-foreground hover:text-foreground',
            )}
          >
            {f.label}
            {f.value !== 'all' && data && (
              <span className="ml-1.5 opacity-60">
                ({data.filter(a => a.priority === f.value).length})
              </span>
            )}
          </button>
        ))}
      </div>

      {isLoading && <SkeletonList count={5} />}

      {isError && <ErrorState onRetry={() => refetch()} />}

      {!isLoading && !isError && filtered.length === 0 && (
        <EmptyState message="Bu kategoride henüz duyuru yok." />
      )}

      {!isLoading && !isError && filtered.length > 0 && (
        <div className="space-y-3">
          {filtered.map(a => (
            <AnnouncementCard key={a.id} item={a} />
          ))}
        </div>
      )}
    </div>
  );
}
