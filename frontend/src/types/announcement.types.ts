export type AnnouncementPriority = 'normal' | 'important' | 'urgent';
export type AnnouncementStatus = 'draft' | 'published' | 'archived';

export interface AnnouncementAuthor {
  id: string;
  firstName: string;
  lastName: string;
}

export interface AnnouncementAttachment {
  id: string;
  fileName: string;
  fileType: 'image' | 'document';
  fileSize: number;
  displayOrder: number;
  createdAt: string;
}

export interface Announcement {
  id: string;
  title: string;
  body: string;
  priority: AnnouncementPriority;
  status: AnnouncementStatus;
  isPublic: boolean;
  publishedAt: string | null;
  scheduledAt: string | null;
  expiresAt: string | null;
  createdBy: AnnouncementAuthor;
  attachments: AnnouncementAttachment[];
  createdAt: string;
  updatedAt: string;
}
