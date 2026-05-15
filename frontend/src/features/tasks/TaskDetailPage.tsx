import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { getTaskById } from '@/api/tasks';
import { Spinner, ErrorState } from '@/components/common/QueryStates';
import type { TaskStatus, TaskPriority, TaskDetail } from '@/types/task.types';

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

const PRIORITY_LABEL: Record<TaskPriority, string> = {
  low: 'Düşük',
  normal: 'Normal',
  high: 'Yüksek',
  urgent: 'Acil',
};

const PRIORITY_BADGE: Record<TaskPriority, string> = {
  low: 'bg-muted text-muted-foreground',
  normal: 'bg-muted text-muted-foreground',
  high: 'bg-amber-light text-amber',
  urgent: 'bg-destructive/10 text-destructive',
};

function MetaRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 py-2.5 border-b border-border last:border-0">
      <span className="text-sm text-muted-foreground flex-shrink-0">{label}</span>
      <span className="text-sm font-medium text-foreground text-right">{value}</span>
    </div>
  );
}

function StarRating({ value }: { value: number | null }) {
  if (!value) return <span className="text-muted-foreground">Henüz değerlendirilmedi</span>;
  return (
    <span className="flex items-center gap-0.5">
      {[1, 2, 3, 4, 5].map(i => (
        <svg key={i} className={cn('h-4 w-4', i <= value ? 'text-amber' : 'text-muted')} viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
        </svg>
      ))}
    </span>
  );
}

function CommentBubble({ comment }: { comment: TaskDetail['comments'][number] }) {
  const date = new Date(comment.createdAt).toLocaleDateString('tr-TR', {
    day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
  });
  return (
    <div className="rounded-xl border border-border bg-background-card p-4">
      <div className="flex items-center justify-between mb-2">
        <span className="text-sm font-semibold text-foreground">
          {comment.createdBy.firstName} {comment.createdBy.lastName}
        </span>
        <span className="text-xs text-muted-foreground">{date}</span>
      </div>
      <p className="text-sm text-foreground leading-relaxed">{comment.body}</p>
    </div>
  );
}

export function TaskDetailPage() {
  const { id } = useParams<{ id: string }>();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['tasks', id],
    queryFn: () => getTaskById(id!) as unknown as Promise<TaskDetail>,
    enabled: !!id,
    retry: 1,
  });

  const task = data as TaskDetail | undefined;

  return (
    <div className="p-6 max-w-3xl space-y-6">
      <Link
        to="/tasks"
        className="inline-flex items-center gap-1.5 text-sm font-semibold text-primary hover:underline"
      >
        ← Taleplerime dön
      </Link>

      {isLoading && <Spinner />}

      {isError && <ErrorState onRetry={() => refetch()} />}

      {task && (
        <>
          {/* Header */}
          <div className="space-y-2">
            {task.taskNumber && (
              <span className="text-xs font-bold tracking-widest text-muted-foreground uppercase">
                {task.taskNumber}
              </span>
            )}
            <h1 className="text-xl font-bold text-foreground leading-snug">{task.title}</h1>
            <div className="flex flex-wrap gap-2">
              <span className={cn('rounded-full px-2.5 py-0.5 text-[11px] font-bold', STATUS_BADGE[task.status])}>
                {STATUS_LABEL[task.status]}
              </span>
              <span className={cn('rounded-full px-2.5 py-0.5 text-[11px] font-bold', PRIORITY_BADGE[task.priority])}>
                {PRIORITY_LABEL[task.priority]}
              </span>
            </div>
          </div>

          {/* Description */}
          <div className="rounded-xl border border-border bg-background-card p-4">
            <h2 className="mb-2 text-xs font-bold uppercase tracking-wider text-muted-foreground">Açıklama</h2>
            <p className="text-sm text-foreground leading-relaxed whitespace-pre-wrap">{task.description}</p>
          </div>

          {/* Meta */}
          <div className="rounded-xl border border-border bg-background-card px-4">
            <MetaRow label="Kategori" value={task.categoryName ?? '—'} />
            <MetaRow
              label="Oluşturulma"
              value={new Date(task.createdAt).toLocaleDateString('tr-TR', {
                day: 'numeric', month: 'long', year: 'numeric',
              })}
            />
            <MetaRow
              label="Atanan"
              value={task.assignedTo
                ? `${task.assignedTo.firstName} ${task.assignedTo.lastName}`
                : 'Henüz atanmadı'}
            />
            {task.locationDetail && (
              <MetaRow label="Konum" value={task.locationDetail} />
            )}
            {task.completedAt && (
              <MetaRow
                label="Tamamlanma"
                value={new Date(task.completedAt).toLocaleDateString('tr-TR', {
                  day: 'numeric', month: 'long', year: 'numeric',
                })}
              />
            )}
            {task.status === 'completed' && (
              <MetaRow label="Değerlendirme" value={<StarRating value={task.rating ?? null} />} />
            )}
          </div>

          {/* Comments */}
          {task.comments && task.comments.length > 0 && (
            <div className="space-y-3">
              <h2 className="text-sm font-bold uppercase tracking-wider text-muted-foreground">
                Yorumlar ({task.comments.filter(c => !c.isInternal).length})
              </h2>
              {task.comments
                .filter(c => !c.isInternal)
                .map(c => <CommentBubble key={c.id} comment={c} />)}
            </div>
          )}

          {/* Status history */}
          {task.statusHistory && task.statusHistory.length > 0 && (
            <div className="space-y-2">
              <h2 className="text-sm font-bold uppercase tracking-wider text-muted-foreground">Durum Geçmişi</h2>
              <div className="rounded-xl border border-border bg-background-card divide-y divide-border">
                {task.statusHistory.map(h => (
                  <div key={h.id} className="flex items-center justify-between px-4 py-3 text-sm">
                    <div className="flex items-center gap-2">
                      {h.fromStatus && (
                        <>
                          <span className="text-muted-foreground">{STATUS_LABEL[h.fromStatus]}</span>
                          <span className="text-muted-foreground">→</span>
                        </>
                      )}
                      <span className="font-semibold text-foreground">{STATUS_LABEL[h.toStatus]}</span>
                    </div>
                    <span className="text-xs text-muted-foreground">
                      {new Date(h.createdAt).toLocaleDateString('tr-TR', {
                        day: 'numeric', month: 'short',
                      })}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
