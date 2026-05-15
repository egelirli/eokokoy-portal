import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/stores/authStore';
import { getAnnouncements } from '@/api/announcements';
import { getTasks } from '@/api/tasks';
import { getPolls } from '@/api/polls';
import type { Announcement, AnnouncementPriority } from '@/types/announcement.types';
import type { Task, TaskStatus, TaskPriority } from '@/types/task.types';
import type { Poll } from '@/types/poll.types';

// ── Skeleton ────────────────────────────────────────────────────────────────

function Skeleton({ className }: { className?: string }) {
  return <div className={cn('animate-pulse rounded bg-muted', className)} />;
}

function CardSkeleton() {
  return (
    <div className="rounded-xl border border-border bg-background-card p-4 space-y-2">
      <Skeleton className="h-3 w-1/4" />
      <Skeleton className="h-4 w-3/4" />
      <Skeleton className="h-3 w-1/3" />
    </div>
  );
}

// ── Empty / Error states ─────────────────────────────────────────────────────

function EmptyState({ message }: { message: string }) {
  return (
    <div className="rounded-xl border border-dashed border-border bg-background-card py-10 text-center">
      <p className="text-sm text-muted-foreground">{message}</p>
    </div>
  );
}

function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="rounded-xl border border-destructive/30 bg-destructive/5 px-4 py-3 flex items-center justify-between gap-3">
      <p className="text-sm text-destructive">Veriler yüklenemedi.</p>
      <button onClick={onRetry} className="text-xs font-semibold text-destructive underline underline-offset-2 hover:opacity-80">
        Tekrar dene
      </button>
    </div>
  );
}

// ── Section wrapper ──────────────────────────────────────────────────────────

function Section({
  label,
  linkTo,
  linkLabel = 'Tümü →',
  children,
}: {
  label: string;
  linkTo: string;
  linkLabel?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-bold uppercase tracking-widest text-primary">{label}</h2>
        <Link to={linkTo} className="text-xs font-semibold text-primary hover:underline">
          {linkLabel}
        </Link>
      </div>
      {children}
    </div>
  );
}

// ── Priority / Status badges ─────────────────────────────────────────────────

const PRIORITY_LABEL: Record<AnnouncementPriority, string> = {
  normal: 'Normal',
  important: 'Önemli',
  urgent: 'Acil',
};

const PRIORITY_CLASS: Record<AnnouncementPriority, string> = {
  normal: 'bg-muted text-muted-foreground',
  important: 'bg-amber-light text-amber',
  urgent: 'bg-destructive/10 text-destructive',
};

const TASK_STATUS_LABEL: Record<TaskStatus, string> = {
  pending: 'Bekliyor',
  assigned: 'Atandı',
  in_progress: 'Devam ediyor',
  completed: 'Tamamlandı',
};

const TASK_STATUS_CLASS: Record<TaskStatus, string> = {
  pending: 'bg-muted text-muted-foreground',
  assigned: 'bg-amber-light text-amber',
  in_progress: 'bg-primary/10 text-primary',
  completed: 'bg-green-100 text-green-700',
};

const TASK_PRIORITY_CLASS: Record<TaskPriority, string> = {
  low: 'bg-muted',
  normal: 'bg-muted',
  high: 'bg-amber',
  urgent: 'bg-destructive',
};

// ── Announcement card ────────────────────────────────────────────────────────

function AnnouncementCard({ item }: { item: Announcement }) {
  const date = item.publishedAt
    ? new Date(item.publishedAt).toLocaleDateString('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' })
    : null;

  return (
    <Link
      to={`/announcements/${item.id}`}
      className="flex items-start gap-3 rounded-xl border border-border bg-background-card p-4 hover:border-primary/30 transition-colors"
    >
      <span className={cn('mt-0.5 h-2 w-2 flex-shrink-0 rounded-full', TASK_PRIORITY_CLASS[item.priority as TaskPriority] ?? 'bg-muted')} />
      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-2">
          <p className="text-sm font-semibold text-foreground line-clamp-2 leading-snug">{item.title}</p>
          <span className={cn('flex-shrink-0 rounded-full px-2 py-0.5 text-[10px] font-bold', PRIORITY_CLASS[item.priority])}>
            {PRIORITY_LABEL[item.priority]}
          </span>
        </div>
        {date && <p className="mt-1 text-xs text-muted-foreground">{date}</p>}
      </div>
    </Link>
  );
}

// ── Task card ────────────────────────────────────────────────────────────────

function TaskCard({ item }: { item: Task }) {
  const date = new Date(item.createdAt).toLocaleDateString('tr-TR', { day: 'numeric', month: 'long' });

  return (
    <Link
      to={`/tasks/${item.id}`}
      className="flex items-start gap-3 rounded-xl border border-border bg-background-card p-4 hover:border-primary/30 transition-colors"
    >
      <div className="min-w-0 flex-1">
        <div className="flex items-start justify-between gap-2">
          <p className="text-sm font-semibold text-foreground line-clamp-2 leading-snug">{item.title}</p>
          <span className={cn('flex-shrink-0 rounded-full px-2 py-0.5 text-[10px] font-bold', TASK_STATUS_CLASS[item.status])}>
            {TASK_STATUS_LABEL[item.status]}
          </span>
        </div>
        {item.categoryName && (
          <p className="mt-1 text-xs text-muted-foreground">{item.categoryName} · {date}</p>
        )}
      </div>
    </Link>
  );
}

// ── Poll card ────────────────────────────────────────────────────────────────

function PollCard({ item }: { item: Poll }) {
  const endsAt = item.endsAt
    ? new Date(item.endsAt).toLocaleDateString('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' })
    : null;

  return (
    <Link
      to={`/polls/${item.id}`}
      className="flex items-start justify-between gap-4 rounded-xl border border-border bg-background-card p-4 hover:border-primary/30 transition-colors"
    >
      <div className="min-w-0">
        <div className="flex items-center gap-2 mb-1">
          <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-bold text-primary">
            {item.type === 'vote' ? 'Oylama' : 'Anket'}
          </span>
          {item.hasResponded && (
            <span className="rounded-full bg-green-100 px-2 py-0.5 text-[10px] font-bold text-green-700">Yanıtlandı</span>
          )}
        </div>
        <p className="text-sm font-semibold text-foreground line-clamp-2 leading-snug">{item.title}</p>
        {endsAt && <p className="mt-1 text-xs text-muted-foreground">Bitiş: {endsAt}</p>}
      </div>
      {!item.hasResponded && (
        <span className="flex-shrink-0 rounded-lg bg-primary px-3 py-1.5 text-xs font-bold text-primary-foreground">
          Katıl
        </span>
      )}
    </Link>
  );
}

// ── Dashboard ────────────────────────────────────────────────────────────────

export function DashboardPage() {
  const { user } = useAuthStore();

  const announcements = useQuery({
    queryKey: ['announcements'],
    queryFn: getAnnouncements,
    retry: 1,
  });

  const tasks = useQuery({
    queryKey: ['tasks'],
    queryFn: getTasks,
    retry: 1,
  });

  const polls = useQuery({
    queryKey: ['polls'],
    queryFn: getPolls,
    retry: 1,
  });

  const today = new Date().toLocaleDateString('tr-TR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

  const recentAnnouncements = (announcements.data ?? []).slice(0, 4);
  const recentTasks = (tasks.data ?? []).slice(0, 4);
  const activePolls = (polls.data ?? []).filter(p => p.status === 'active');

  return (
    <div className="space-y-8 p-6">
      {/* ── Header ── */}
      <div>
        <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">{today}</p>
        <h1 className="mt-1 text-2xl font-bold text-foreground">
          Hoş geldiniz{user ? `, ${user.firstName}` : ''} 👋
        </h1>
      </div>

      {/* ── Quick stat cards ── */}
      <div className="grid grid-cols-3 gap-4">
        {[
          { label: 'Duyurular', to: '/announcements', count: announcements.data?.length, icon: '📢' },
          { label: 'Taleplerim', to: '/tasks', count: tasks.data?.length, icon: '🔧' },
          { label: 'Aktif anketler', to: '/polls', count: activePolls.length, icon: '🗳️' },
        ].map(({ label, to, count, icon }) => (
          <Link
            key={label}
            to={to}
            className="rounded-xl border border-border bg-background-card p-4 hover:border-primary/30 transition-colors"
          >
            <div className="text-2xl mb-2">{icon}</div>
            <div className="text-xl font-bold text-foreground">
              {count !== undefined ? count : <span className="animate-pulse inline-block h-5 w-8 rounded bg-muted" />}
            </div>
            <div className="text-xs text-muted-foreground mt-0.5">{label}</div>
          </Link>
        ))}
      </div>

      {/* ── Main grid: announcements + tasks ── */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Announcements */}
        <Section label="Son duyurular" linkTo="/announcements">
          {announcements.isLoading && (
            <div className="space-y-3">
              {[0, 1, 2].map(i => <CardSkeleton key={i} />)}
            </div>
          )}
          {announcements.isError && (
            <ErrorState onRetry={() => announcements.refetch()} />
          )}
          {announcements.isSuccess && recentAnnouncements.length === 0 && (
            <EmptyState message="Henüz duyuru yok." />
          )}
          {announcements.isSuccess && recentAnnouncements.length > 0 && (
            <div className="space-y-2">
              {recentAnnouncements.map(a => <AnnouncementCard key={a.id} item={a} />)}
            </div>
          )}
        </Section>

        {/* Tasks */}
        <Section label="Son taleplerim" linkTo="/tasks">
          {tasks.isLoading && (
            <div className="space-y-3">
              {[0, 1, 2].map(i => <CardSkeleton key={i} />)}
            </div>
          )}
          {tasks.isError && (
            <ErrorState onRetry={() => tasks.refetch()} />
          )}
          {tasks.isSuccess && recentTasks.length === 0 && (
            <EmptyState message="Henüz talep yok." />
          )}
          {tasks.isSuccess && recentTasks.length > 0 && (
            <div className="space-y-2">
              {recentTasks.map(t => <TaskCard key={t.id} item={t} />)}
            </div>
          )}
        </Section>
      </div>

      {/* ── Active polls (full width) ── */}
      <Section label="Aktif anketler & oylamalar" linkTo="/polls">
        {polls.isLoading && (
          <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
            {[0, 1].map(i => <CardSkeleton key={i} />)}
          </div>
        )}
        {polls.isError && (
          <ErrorState onRetry={() => polls.refetch()} />
        )}
        {polls.isSuccess && activePolls.length === 0 && (
          <EmptyState message="Şu an aktif anket veya oylama yok." />
        )}
        {polls.isSuccess && activePolls.length > 0 && (
          <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
            {activePolls.map(p => <PollCard key={p.id} item={p} />)}
          </div>
        )}
      </Section>
    </div>
  );
}
