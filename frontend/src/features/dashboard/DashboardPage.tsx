import { useAuthStore } from '@/stores/authStore';

export function DashboardPage() {
  const { user } = useAuthStore();
  return (
    <div>
      <p className="text-muted-foreground text-sm">
        Hoş geldiniz, {user?.firstName} {user?.lastName}
      </p>
    </div>
  );
}
