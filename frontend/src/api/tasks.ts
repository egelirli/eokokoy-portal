import client from './client';
import type { ApiResponse } from '@/types/api.types';
import type { Task } from '@/types/task.types';

export async function getTasks(): Promise<Task[]> {
  const res = await client.get<ApiResponse<Task[]>>('/tasks');
  return res.data.data;
}

export async function getTaskById(id: string): Promise<Task> {
  const res = await client.get<ApiResponse<Task>>(`/tasks/${id}`);
  return res.data.data;
}
