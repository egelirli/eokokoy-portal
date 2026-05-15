import client from './client';
import type { ApiResponse } from '@/types/api.types';
import type { Due, DuesSummary } from '@/types/dues.types';

export async function getMyDues(): Promise<Due[]> {
  const res = await client.get<ApiResponse<Due[]>>('/dues/my');
  return res.data.data;
}

export async function getMyDuesSummary(): Promise<DuesSummary> {
  const res = await client.get<ApiResponse<DuesSummary>>('/dues/my/summary');
  return res.data.data;
}
