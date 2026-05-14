import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { ADMIN_ROLES } from '@/lib/constants';

interface ProtectedRouteProps {
  children: React.ReactNode;
  /** If true, user must have one of SUPER_ADMIN or YONETIM_KURULU roles */
  adminOnly?: boolean;
  /** If set, user must have this permission string */
  requiredPermission?: string;
}

export function ProtectedRoute({ children, adminOnly, requiredPermission }: ProtectedRouteProps) {
  const { user, isInitialized } = useAuthStore();
  const location = useLocation();

  // Wait for persisted state to hydrate
  if (!isInitialized) return null;

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (adminOnly) {
    const hasAdminRole = user.roles.some((r) => (ADMIN_ROLES as readonly string[]).includes(r));
    if (!hasAdminRole) {
      return <Navigate to="/dashboard" replace />;
    }
  }

  if (requiredPermission && !user.permissions.includes(requiredPermission)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}
