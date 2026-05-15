export type TaskPriority = 'low' | 'normal' | 'high' | 'urgent';
export type TaskStatus = 'pending' | 'assigned' | 'in_progress' | 'completed';

export interface TaskUser {
  id: string;
  firstName: string;
  lastName: string;
}

export interface Task {
  id: string;
  taskNumber: string;
  title: string;
  description: string;
  priority: TaskPriority;
  status: TaskStatus;
  categoryId: string;
  categoryName: string | null;
  propertyId: string | null;
  locationDetail: string | null;
  createdBy: TaskUser;
  assignedTo: TaskUser | null;
  completedAt: string | null;
  slaDeadline: string | null;
  rating: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface TaskComment {
  id: string;
  body: string;
  isInternal: boolean;
  createdBy: TaskUser;
  createdAt: string;
}

export interface TaskStatusHistory {
  id: string;
  fromStatus: TaskStatus | null;
  toStatus: TaskStatus;
  changedBy: TaskUser;
  note: string | null;
  createdAt: string;
}

export interface TaskDetail extends Task {
  comments: TaskComment[];
  statusHistory: TaskStatusHistory[];
  ratingComment: string | null;
  ratedAt: string | null;
}
