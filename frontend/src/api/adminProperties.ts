import client from './client';
import type { ApiResponse } from '@/types/api.types';
import type { AdminProperty } from '@/types/property.types';

export async function getAdminProperties(): Promise<AdminProperty[]> {
  const res = await client.get<ApiResponse<AdminProperty[]>>('/admin/properties');
  return res.data.data;
}

export async function getAdminPropertyById(id: string): Promise<AdminProperty> {
  const res = await client.get<ApiResponse<AdminProperty>>(`/admin/properties/${id}`);
  return res.data.data;
}
