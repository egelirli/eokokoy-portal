export type InvitationStatus = 'pending' | 'used' | 'expired';

export interface InvitationCreatedBy {
  id: string;
  firstName: string;
  lastName: string;
}

export interface Invitation {
  id: string;
  email: string;
  roleId: string;
  roleName: string | null;
  propertyId: string | null;
  isUsed: boolean;
  usedAt: string | null;
  expiresAt: string;
  createdAt: string;
  createdBy: InvitationCreatedBy;
}

export interface SendInvitationRequest {
  email: string;
  roleId: string;
  propertyId?: string;
}
