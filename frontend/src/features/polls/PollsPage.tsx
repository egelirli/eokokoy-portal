import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { getPolls } from '@/api/polls';
import { SkeletonList, ErrorState, EmptyState, PageHeader } from '@/components/common/QueryStates';
import type { Poll, PollStatus } from '@/types/poll.types';

const STATUS_LABEL: Record<PollStatus, string> = {
  draft: 'Taslak',
  active: 'Aktif',
  closed: 'Kapandı',
  cancelled: 'İptal',
};

const STATUS_BADGE: Record<PollStatus, string> = {
  draft: 'bg-muted text-muted-foreground',
  active: 'bg-primary/10 text-primary',
  closed: 'bg-muted text-muted-foreground',
  cancelled: 'bg-destructive/10 text-destructive',
};

type TabFilter = 'active' | 'all';

function PollCard({ item }: { item: Poll }) {
  const endsAt = item.endsAt
    ? new Date(item.endsAt).toLocaleDateString('tr-TR', { day: 'numeric', month: 'short', year: 'numeric' })
    : null;
  const isActive = item.status === 'active';

  return (
    <Link
      to={`/polls/${item.id}`}
      className={cn(
        'flex items-start justify-between gap-4 rounded-xl border bg-background-card p-4 transition-colors',
        isActive ? 'border-primary/30 hover:border-primary/60' : 'border-border hover:border-border/60',
      )}
    >
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2 mb-2">
          <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-bold text-muted-foreground">
            {item.type === 'vote' ? 'Oylama' : 'Anket'}
          </span>
          <span className={cn('rounded-full px-2 py-0.5 text-[10px] font-bold', STATUS_BADGE[item.status])}>
            {STATUS_LABEL[item.status]}
          </span>
          {item.isAnonymous && (
            <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-semibold text-muted-foreground">
              Anonim
            </span>
          )}
          {item.hasResponded && (
            <span className="rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-bold text-green-700">
              Yanıtlandı ✓
            </span>
          )}
        </div>

        <p className="font-semibold text-foreground leading-snug line-clamp-2">{item.title}</p>

        <div className="mt-1.5 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
          <span>{item.questionCount} soru</span>
          {endsAt && (
            <>
              <span>·</span>
              <span className={cn(isActive && !item.hasResponded ? 'text-amber font-medium' : '')}>
                Bitiş: {endsAt}
              </span>
            </>
          )}
        </div>
      </div>

      {isActive && !item.hasResponded && (
        <span className="flex-shrink-0 self-center rounded-lg bg-primary px-3 py-1.5 text-xs font-bold text-primary-foreground">
          Katıl
        </span>
      )}
    </Link>
  );
}

export function PollsPage() {
  const [tab, setTab] = useState<TabFilter>('active');

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['polls'],
    queryFn: getPolls,
    retry: 1,
  });

  const activeCount = (data ?? []).filter(p => p.status === 'active').length;
  const filtered = (data ?? []).filter(p => tab === 'all' || p.status === 'active');

  return (
    <div className="space-y-6 p-6">
      <PageHeader title="Anketler & Oylamalar" subtitle="Aktif oylama ve anketlere katılın" />

      <div className="flex gap-2">
        {([
          { value: 'active', label: `Aktif (${activeCount})` },
          { value: 'all', label: 'Tümü' },
        ] as { value: TabFilter; label: string }[]).map(t => (
          <button
            key={t.value}
            onClick={() => setTab(t.value)}
            className={cn(
              'rounded-full px-4 py-1.5 text-sm font-semibold transition-colors',
              tab === t.value
                ? 'bg-primary text-primary-foreground'
                : 'bg-background-card border border-border text-muted-foreground hover:text-foreground',
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      {isLoading && <SkeletonList count={3} />}

      {isError && <ErrorState onRetry={() => refetch()} />}

      {!isLoading && !isError && filtered.length === 0 && (
        <EmptyState message={tab === 'active' ? 'Şu an aktif oylama veya anket yok.' : 'Henüz hiç anket oluşturulmadı.'} />
      )}

      {!isLoading && !isError && filtered.length > 0 && (
        <div className="space-y-3">
          {filtered.map(p => <PollCard key={p.id} item={p} />)}
        </div>
      )}
    </div>
  );
}
