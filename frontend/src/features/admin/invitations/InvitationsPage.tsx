import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQuery, useMutation } from '@tanstack/react-query';
import { cn } from '@/lib/utils';
import { sendInvitation } from '@/api/invitations';
import { getRoles } from '@/api/roles';
import { PageHeader } from '@/components/common/QueryStates';

const schema = z.object({
  email: z.string().email('Geçerli bir e-posta adresi girin'),
  roleId: z.string().min(1, 'Rol seçimi zorunludur'),
});
type FormData = z.infer<typeof schema>;

export function InvitationsPage() {
  const { data: roles = [], isLoading: rolesLoading } = useQuery({
    queryKey: ['admin-roles'],
    queryFn: getRoles,
    staleTime: 5 * 60 * 1000,
    retry: 1,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const mutation = useMutation({
    mutationFn: sendInvitation,
    onSuccess: () => reset(),
  });

  // SUPER_ADMIN davet ile atanamaz (SPEC-01); backend zaten aktif rolleri döner
  const invitableRoles = roles.filter(r => r.code !== 'SUPER_ADMIN');

  return (
    <div className="space-y-6 p-6">
      <PageHeader
        title="Davetiyeler"
        subtitle="Yeni kullanıcıları sisteme davet edin"
      />

      <form
        onSubmit={handleSubmit(data => mutation.mutate(data))}
        className="rounded-2xl border border-border bg-background-card p-6 space-y-4"
      >
        <h2 className="text-base font-semibold text-foreground">Davet Gönder</h2>

        <div className="flex flex-col gap-3 sm:flex-row">
          <div className="flex-1 space-y-1">
            <input
              {...register('email')}
              type="email"
              placeholder="ornek@eposta.com"
              className={cn(
                'w-full rounded-lg border bg-background px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:border-primary transition-colors',
                errors.email ? 'border-destructive' : 'border-border',
              )}
            />
            {errors.email && (
              <p className="text-xs text-destructive">{errors.email.message}</p>
            )}
          </div>

          <div className="space-y-1 sm:w-52">
            <select
              {...register('roleId')}
              disabled={rolesLoading}
              className={cn(
                'w-full rounded-lg border bg-background px-4 py-2.5 text-sm text-foreground focus:outline-none focus:border-primary transition-colors disabled:opacity-60',
                errors.roleId ? 'border-destructive' : 'border-border',
              )}
            >
              <option value="">{rolesLoading ? 'Yükleniyor…' : 'Rol seçin…'}</option>
              {invitableRoles.map(r => (
                <option key={r.id} value={r.id}>{r.displayName}</option>
              ))}
            </select>
            {errors.roleId && (
              <p className="text-xs text-destructive">{errors.roleId.message}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={mutation.isPending || rolesLoading}
            className="flex-shrink-0 rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {mutation.isPending ? 'Gönderiliyor…' : 'Davet Gönder'}
          </button>
        </div>

        {mutation.isError && (
          <p className="text-sm text-destructive">Davet gönderilemedi. Lütfen tekrar deneyin.</p>
        )}
        {mutation.isSuccess && (
          <p className="text-sm text-green-600">Davet başarıyla gönderildi.</p>
        )}
      </form>
    </div>
  );
}
