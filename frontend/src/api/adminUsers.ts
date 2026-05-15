import client from './client';
import type { ApiResponse } from '@/types/api.types';
import type { AdminUser } from '@/types/admin.types';

export async function getAdminUsers(): Promise<AdminUser[]> {
  const res = await client.get<ApiResponse<AdminUser[]>>('/admin/users');
  return res.data.data;
}
