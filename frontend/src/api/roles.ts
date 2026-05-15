import client from './client';
import type { ApiResponse } from '@/types/api.types';
import type { Role } from '@/types/role.types';

export async function getRoles(): Promise<Role[]> {
  const res = await client.get<ApiResponse<Role[]>>('/admin/roles');
  return res.data.data;
}
