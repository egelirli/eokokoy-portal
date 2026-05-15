import client from './client';
import type { ApiResponse } from '@/types/api.types';
import type { Announcement } from '@/types/announcement.types';

export async function getAnnouncements(): Promise<Announcement[]> {
  const res = await client.get<ApiResponse<Announcement[]>>('/announcements');
  return res.data.data;
}

export async function getAnnouncementById(id: string): Promise<Announcement> {
  const res = await client.get<ApiResponse<Announcement>>(`/announcements/${id}`);
  return res.data.data;
}
