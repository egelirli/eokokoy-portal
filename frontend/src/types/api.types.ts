export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}
