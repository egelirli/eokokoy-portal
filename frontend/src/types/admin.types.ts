export type UserStatus = 'pending' | 'active' | 'inactive' | 'suspended';

export interface AdminUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  status: UserStatus;
  roles: string[];
  avatarUrl: string | null;
  createdAt: string;
  lastLoginAt: string | null;
}
