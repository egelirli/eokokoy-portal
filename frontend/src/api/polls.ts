import client from './client';
import type { ApiResponse } from '@/types/api.types';
import type { Poll, PollDetail, PollRespondRequest } from '@/types/poll.types';

export async function getPolls(): Promise<Poll[]> {
  const res = await client.get<ApiResponse<Poll[]>>('/polls');
  return res.data.data;
}

export async function getPollById(id: string): Promise<PollDetail> {
  const res = await client.get<ApiResponse<PollDetail>>(`/polls/${id}`);
  return res.data.data;
}

export async function respondToPoll(id: string, body: PollRespondRequest): Promise<void> {
  await client.post(`/polls/${id}/respond`, body);
}
