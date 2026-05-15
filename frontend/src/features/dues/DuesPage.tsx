import { useQuery } from '@tanstack/react-query';
import { cn } from '@/lib/utils';
import { getMyDues, getMyDuesSummary } from '@/api/dues';
import { SkeletonList, ErrorState, EmptyState, PageHeader, CardSkeleton } from '@/components/common/QueryStates';
import type { Due, DueStatus } from '@/types/dues.types';

const STATUS_LABEL: Record<DueStatus, string> = {
  unpaid: 'Ödenmedi',
  paid: 'Ödendi',
  partially_paid: 'Kısmi Ödeme',
  cancelled: 'İptal',
};

const STATUS_BADGE: Record<DueStatus, string> = {
  unpaid: 'bg-destructive/10 text-destructive',
  paid: 'bg-green-100 text-green-700',
  partially_paid: 'bg-amber-light text-amber',
  cancelled: 'bg-muted text-muted-foreground',
};

function formatTL(amount: number): string {
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY', maximumFractionDigits: 0 }).format(amount);
}

function periodLabel(year: number, month: number | null): string {
  if (!month) return `${year} (Yıllık)`;
  const monthName = new Date(year, month - 1, 1).toLocaleDateString('tr-TR', { month: 'long' });
  return `${monthName} ${year}`;
}

function SummaryCard({ label, amount, variant = 'default' }: { label: string; amount: number; variant?: 'default' | 'danger' | 'success' }) {
  const amountClass = variant === 'danger' ? 'text-destructive' : variant === 'success' ? 'text-green-600' : 'text-foreground';
  return (
    <div className="rounded-xl border border-border bg-background-card p-4">
      <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-1">{label}</p>
      <p className={cn('text-xl font-bold', amountClass)}>{formatTL(amount)}</p>
    </div>
  );
}

function DueCard({ item }: { item: Due }) {
  const dueDate = new Date(item.dueDate).toLocaleDateString('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' });
  const isOverdue = item.status === 'unpaid' && new Date(item.dueDate) < new Date();

  return (
    <div className={cn(
      'rounded-xl border bg-background-card p-4',
      isOverdue ? 'border-destructive/30' : 'border-border',
    )}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="font-semibold text-foreground">{periodLabel(item.periodYear, item.periodMonth)}</p>
          {item.description && (
            <p className="mt-0.5 text-xs text-muted-foreground">{item.description}</p>
          )}
        </div>
        <span className={cn('flex-shrink-0 rounded-full px-2.5 py-0.5 text-[11px] font-bold', STATUS_BADGE[item.status])}>
          {STATUS_LABEL[item.status]}
        </span>
      </div>

      <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
        <div className="text-sm text-muted-foreground">
          Son ödeme: <span className={cn('font-medium', isOverdue ? 'text-destructive' : 'text-foreground')}>{dueDate}</span>
        </div>
        <div className="text-right">
          {item.status === 'partially_paid' ? (
            <div className="text-sm">
              <span className="text-muted-foreground">{formatTL(item.paidAmount)}</span>
              <span className="text-muted-foreground"> / </span>
              <span className="font-bold text-foreground">{formatTL(item.amount)}</span>
            </div>
          ) : (
            <span className="font-bold text-foreground">{formatTL(item.amount)}</span>
          )}
        </div>
      </div>

      {item.status === 'partially_paid' && (
        <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-muted">
          <div
            className="h-full rounded-full bg-amber"
            style={{ width: `${Math.min(100, (item.paidAmount / item.amount) * 100).toFixed(0)}%` }}
          />
        </div>
      )}
    </div>
  );
}

export function DuesPage() {
  const dues = useQuery({ queryKey: ['dues-my'], queryFn: getMyDues, retry: 1 });
  const summary = useQuery({ queryKey: ['dues-summary'], queryFn: getMyDuesSummary, retry: 1 });

  const unpaidDues = (dues.data ?? []).filter(d => d.status !== 'paid' && d.status !== 'cancelled');
  const paidDues = (dues.data ?? []).filter(d => d.status === 'paid');

  return (
    <div className="space-y-6 p-6">
      <PageHeader title="Aidatlarım" subtitle="Konut aidat durumu ve ödeme geçmişi" />

      {/* Summary */}
      {summary.isLoading && (
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          {[0, 1, 2, 3].map(i => <CardSkeleton key={i} />)}
        </div>
      )}
      {summary.isError && <ErrorState onRetry={() => summary.refetch()} message="Özet bilgisi yüklenemedi." />}
      {summary.data && (
        <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
          <SummaryCard label="Toplam borç" amount={summary.data.totalAmount} />
          <SummaryCard label="Ödenen" amount={summary.data.paidAmount} variant="success" />
          <SummaryCard label="Kalan borç" amount={summary.data.unpaidAmount} variant="danger" />
          <SummaryCard label="Vadesi geçen" amount={summary.data.overdueAmount} variant="danger" />
        </div>
      )}

      {/* Unpaid */}
      <div className="space-y-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-primary">
          Bekleyen Ödemeler
        </h2>
        {dues.isLoading && <SkeletonList count={3} />}
        {dues.isError && <ErrorState onRetry={() => dues.refetch()} />}
        {dues.isSuccess && unpaidDues.length === 0 && (
          <EmptyState message="Bekleyen ödemeniz yok." />
        )}
        {dues.isSuccess && unpaidDues.length > 0 && (
          <div className="space-y-3">
            {unpaidDues.map(d => <DueCard key={d.id} item={d} />)}
          </div>
        )}
      </div>

      {/* Paid */}
      {dues.isSuccess && paidDues.length > 0 && (
        <div className="space-y-3">
          <h2 className="text-sm font-bold uppercase tracking-wider text-muted-foreground">
            Ödeme Geçmişi
          </h2>
          <div className="space-y-3">
            {paidDues.map(d => <DueCard key={d.id} item={d} />)}
          </div>
        </div>
      )}
    </div>
  );
}
