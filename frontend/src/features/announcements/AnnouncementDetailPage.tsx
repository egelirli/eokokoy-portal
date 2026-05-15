import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { getAnnouncementById } from '@/api/announcements';
import { Spinner, ErrorState } from '@/components/common/QueryStates';
import type { AnnouncementPriority } from '@/types/announcement.types';

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

function FileIcon() {
  return (
    <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
      <polyline points="14 2 14 8 20 8"/>
    </svg>
  );
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function AnnouncementDetailPage() {
  const { id } = useParams<{ id: string }>();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['announcements', id],
    queryFn: () => getAnnouncementById(id!),
    enabled: !!id,
    retry: 1,
  });

  return (
    <div className="p-6 max-w-3xl">
      <Link
        to="/announcements"
        className="mb-6 inline-flex items-center gap-1.5 text-sm font-semibold text-primary hover:underline"
      >
        ← Duyurulara dön
      </Link>

      {isLoading && <Spinner />}

      {isError && <ErrorState onRetry={() => refetch()} />}

      {data && (
        <article className="space-y-6">
          <div className="space-y-3">
            <div className="flex flex-wrap items-center gap-2">
              <span className={cn('rounded-full px-2.5 py-0.5 text-[11px] font-bold', PRIORITY_BADGE[data.priority])}>
                {PRIORITY_LABEL[data.priority]}
              </span>
              {data.isPublic && (
                <span className="rounded-full bg-muted px-2.5 py-0.5 text-[11px] font-semibold text-muted-foreground">
                  Halka açık
                </span>
              )}
            </div>

            <h1 className="text-2xl font-bold text-foreground leading-tight">{data.title}</h1>

            <div className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
              {data.createdBy && (
                <span className="font-medium">
                  {data.createdBy.firstName} {data.createdBy.lastName}
                </span>
              )}
              {data.publishedAt && (
                <>
                  <span>·</span>
                  <span>
                    {new Date(data.publishedAt).toLocaleDateString('tr-TR', {
                      day: 'numeric', month: 'long', year: 'numeric',
                    })}
                  </span>
                </>
              )}
              {data.expiresAt && (
                <>
                  <span>·</span>
                  <span className="text-amber">
                    {new Date(data.expiresAt) < new Date() ? 'Süresi doldu' : `Son: ${new Date(data.expiresAt).toLocaleDateString('tr-TR', { day: 'numeric', month: 'short' })}`}
                  </span>
                </>
              )}
            </div>
          </div>

          <hr className="border-border" />

          <div className="prose prose-sm max-w-none text-foreground leading-relaxed whitespace-pre-wrap">
            {data.body}
          </div>

          {data.attachments && data.attachments.length > 0 && (
            <div className="space-y-2">
              <h2 className="text-sm font-bold uppercase tracking-wider text-muted-foreground">
                Ekler ({data.attachments.length})
              </h2>
              <div className="space-y-2">
                {data.attachments.map(att => (
                  <a
                    key={att.id}
                    href="#"
                    className="flex items-center gap-3 rounded-lg border border-border bg-background-card px-4 py-3 text-sm hover:border-primary/40 transition-colors"
                  >
                    <span className="text-muted-foreground"><FileIcon /></span>
                    <span className="flex-1 font-medium text-foreground">{att.fileName}</span>
                    <span className="text-xs text-muted-foreground">{formatBytes(att.fileSize)}</span>
                  </a>
                ))}
              </div>
            </div>
          )}
        </article>
      )}
    </div>
  );
}
