export type TaskPriority = 'low' | 'normal' | 'high' | 'urgent';
export type TaskStatus = 'pending' | 'assigned' | 'in_progress' | 'completed';

export interface TaskUser {
  id: string;
  firstName: string;
  lastName: string;
}

export interface Task {
  id: string;
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
  createdAt: string;
  updatedAt: string;
}
