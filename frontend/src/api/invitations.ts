import client from './client';
import type { ApiResponse } from '@/types/api.types';
import type { Invitation, SendInvitationRequest } from '@/types/invitation.types';

export async function sendInvitation(body: SendInvitationRequest): Promise<Invitation> {
  const res = await client.post<ApiResponse<Invitation>>('/admin/invitations', body);
  return res.data.data;
}

export async function resendInvitation(id: string): Promise<void> {
  await client.post(`/admin/invitations/${id}/resend`);
}
