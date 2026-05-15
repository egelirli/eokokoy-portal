export type DueStatus = 'unpaid' | 'paid' | 'partially_paid' | 'cancelled';

export interface Due {
  id: string;
  propertyId: string;
  propertyNumber: number | null;
  periodYear: number;
  periodMonth: number | null;
  amount: number;
  status: DueStatus;
  paidAmount: number;
  dueDate: string;
  paidAt: string | null;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DuesSummary {
  totalAmount: number;
  paidAmount: number;
  unpaidAmount: number;
  overdueAmount: number;
}
