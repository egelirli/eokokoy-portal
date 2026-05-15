import client from './client';
import type { ApiResponse } from '@/types/api.types';
import type { Poll } from '@/types/poll.types';

export async function getPolls(): Promise<Poll[]> {
  const res = await client.get<ApiResponse<Poll[]>>('/polls');
  return res.data.data;
}

export async function getPollById(id: string): Promise<Poll> {
  const res = await client.get<ApiResponse<Poll>>(`/polls/${id}`);
  return res.data.data;
}
