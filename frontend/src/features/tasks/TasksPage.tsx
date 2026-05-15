import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { getTasks } from '@/api/tasks';
import { SkeletonList, ErrorState, EmptyState, PageHeader } from '@/components/common/QueryStates';
import type { Task, TaskStatus, TaskPriority } from '@/types/task.types';

const STATUS_LABEL: Record<TaskStatus, string> = {
  pending: 'Bekliyor',
  assigned: 'Atandı',
  in_progress: 'Devam Ediyor',
  completed: 'Tamamlandı',
};

const STATUS_BADGE: Record<TaskStatus, string> = {
  pending: 'bg-muted text-muted-foreground',
  assigned: 'bg-amber-light text-amber',
  in_progress: 'bg-primary/10 text-primary',
  completed: 'bg-green-100 text-green-700',
};

const PRIORITY_DOT: Record<TaskPriority, string> = {
  low: 'bg-muted-foreground/40',
  normal: 'bg-muted-foreground',
  high: 'bg-amber',
  urgent: 'bg-destructive',
};

type StatusFilter = 'all' | TaskStatus;

const STATUS_FILTERS: { value: StatusFilter; label: string }[] = [
  { value: 'all', label: 'Tümü' },
  { value: 'pending', label: 'Bekliyor' },
  { value: 'in_progress', label: 'Devam Ediyor' },
  { value: 'completed', label: 'Tamamlandı' },
];

function TaskCard({ item }: { item: Task }) {
  const date = new Date(item.createdAt).toLocaleDateString('tr-TR', {
    day: 'numeric', month: 'short', year: 'numeric',
  });

  return (
    <Link
      to={`/tasks/${item.id}`}
      className="flex items-start gap-3 rounded-xl border border-border bg-background-card p-4 hover:border-primary/40 transition-colors"
    >
      <span className={cn('mt-1.5 h-2 w-2 flex-shrink-0 rounded-full', PRIORITY_DOT[item.priority])} />
      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            {item.taskNumber && (
              <span className="mb-1 block text-[10px] font-bold tracking-wide text-muted-foreground">
                {item.taskNumber}
              </span>
            )}
            <p className="font-semibold text-foreground leading-snug line-clamp-2">{item.title}</p>
          </div>
          <span className={cn('flex-shrink-0 rounded-full px-2 py-0.5 text-[10px] font-bold', STATUS_BADGE[item.status])}>
            {STATUS_LABEL[item.status]}
          </span>
        </div>
        <div className="mt-1.5 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
          {item.categoryName && <span>{item.categoryName}</span>}
          {item.categoryName && <span>·</span>}
          <span>{date}</span>
          {item.assignedTo && (
            <>
              <span>·</span>
              <span>Atanan: {item.assignedTo.firstName} {item.assignedTo.lastName}</span>
            </>
          )}
        </div>
      </div>
    </Link>
  );
}

export function TasksPage() {
  const [filter, setFilter] = useState<StatusFilter>('all');

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['tasks'],
    queryFn: getTasks,
    retry: 1,
  });

  const filtered = (data ?? []).filter(t => filter === 'all' || t.status === filter);

  return (
    <div className="space-y-6 p-6">
      <PageHeader
        title="Taleplerim"
        subtitle="Oluşturduğunuz destek talepleri"
        action={
          <Link
            to="/tasks/new"
            className="rounded-lg bg-primary px-4 py-2 text-sm font-bold text-primary-foreground hover:opacity-90 transition-opacity"
          >
            + Yeni talep
          </Link>
        }
      />

      <div className="flex flex-wrap gap-2">
        {STATUS_FILTERS.map(f => (
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
                ({data.filter(t => t.status === f.value).length})
              </span>
            )}
          </button>
        ))}
      </div>

      {isLoading && <SkeletonList count={4} />}

      {isError && <ErrorState onRetry={() => refetch()} />}

      {!isLoading && !isError && filtered.length === 0 && (
        <EmptyState message="Bu durumda henüz talep yok." />
      )}

      {!isLoading && !isError && filtered.length > 0 && (
        <div className="space-y-3">
          {filtered.map(t => <TaskCard key={t.id} item={t} />)}
        </div>
      )}
    </div>
  );
}
