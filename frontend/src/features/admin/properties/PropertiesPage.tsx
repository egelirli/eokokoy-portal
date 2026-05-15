import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { cn } from '@/lib/utils';
import { getAdminProperties } from '@/api/adminProperties';
import { SkeletonList, ErrorState, EmptyState, PageHeader } from '@/components/common/QueryStates';
import type { AdminProperty, PropertyStatus } from '@/types/property.types';

const STATUS_LABEL: Record<PropertyStatus, string> = {
  sahipli: 'Sahipli',
  'kiralık': 'Kiralık',
  'boş': 'Boş',
};

const STATUS_BADGE: Record<PropertyStatus, string> = {
  sahipli: 'bg-primary/10 text-primary',
  'kiralık': 'bg-amber-light text-amber',
  'boş': 'bg-muted text-muted-foreground',
};

const RELATION_LABEL: Record<string, string> = {
  ev_sahibi: 'Sahip',
  kiraci: 'Kiracı',
  aile_bireyi: 'Aile',
};

type StatusFilter = 'all' | PropertyStatus;

function PropertyCard({ property }: { property: AdminProperty }) {
  const activeResidents = property.residents.filter(r => !r.endDate);

  return (
    <div className="rounded-xl border border-border bg-background-card p-4 hover:border-primary/30 transition-colors">
      <div className="flex items-start justify-between gap-2 mb-3">
        <div className="flex items-center gap-2">
          <span className="text-lg font-bold text-foreground">#{property.number}</span>
          {property.type && (
            <span className="text-xs text-muted-foreground">{property.type}</span>
          )}
        </div>
        <span className={cn('flex-shrink-0 rounded-full px-2.5 py-0.5 text-[11px] font-bold', STATUS_BADGE[property.status])}>
          {STATUS_LABEL[property.status]}
        </span>
      </div>

      {property.areaM2 && (
        <p className="mb-2 text-xs text-muted-foreground">{property.areaM2} m²</p>
      )}

      {activeResidents.length > 0 ? (
        <div className="space-y-1">
          {activeResidents.map(r => (
            <div key={r.id} className="flex items-center justify-between text-xs">
              <span className="text-foreground font-medium">{r.firstName} {r.lastName}</span>
              <span className="text-muted-foreground">{RELATION_LABEL[r.relationType] ?? r.relationType}</span>
            </div>
          ))}
        </div>
      ) : (
        <p className="text-xs text-muted-foreground italic">Sakin yok</p>
      )}
    </div>
  );
}

export function PropertiesPage() {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');
  const [search, setSearch] = useState('');

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['admin-properties'],
    queryFn: getAdminProperties,
    retry: 1,
  });

  const filtered = (data ?? []).filter(p => {
    const matchStatus = statusFilter === 'all' || p.status === statusFilter;
    const matchSearch = search.trim() === '' || String(p.number).includes(search.trim());
    return matchStatus && matchSearch;
  });

  const statusFilters: { value: StatusFilter; label: string }[] = [
    { value: 'all', label: 'Tümü' },
    { value: 'sahipli', label: 'Sahipli' },
    { value: 'kiralık', label: 'Kiralık' },
    { value: 'boş', label: 'Boş' },
  ];

  const counts = {
    all: data?.length ?? 0,
    sahipli: (data ?? []).filter(p => p.status === 'sahipli').length,
    kiralık: (data ?? []).filter(p => p.status === 'kiralık').length,
    boş: (data ?? []).filter(p => p.status === 'boş').length,
  };

  return (
    <div className="space-y-6 p-6">
      <PageHeader
        title="Konutlar"
        subtitle={`${counts.all} konut · ${counts.sahipli} sahipli · ${counts.kiralık} kiralık · ${counts.boş} boş`}
      />

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <input
          type="search"
          placeholder="Konut numarası ara…"
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="rounded-lg border border-border bg-background-card px-4 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none sm:w-48"
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
              {data && <span className="ml-1 opacity-60">({counts[f.value]})</span>}
            </button>
          ))}
        </div>
      </div>

      {isLoading && <SkeletonList count={6} />}

      {isError && <ErrorState onRetry={() => refetch()} />}

      {!isLoading && !isError && filtered.length === 0 && (
        <EmptyState message="Bu kriterlere uyan konut bulunamadı." />
      )}

      {!isLoading && !isError && filtered.length > 0 && (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
          {filtered
            .sort((a, b) => a.number - b.number)
            .map(p => <PropertyCard key={p.id} property={p} />)}
        </div>
      )}
    </div>
  );
}
