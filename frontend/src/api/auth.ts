import client from './client';
import type { ApiResponse } from '@/types/api.types';
import type { LoginRequest, TokenResponse, User } from '@/types/user.types';

export async function login(credentials: LoginRequest): Promise<TokenResponse> {
  const { data } = await client.post<ApiResponse<TokenResponse>>('/auth/login', credentials);
  return data.data;
}

export async function refreshTokens(refreshToken: string): Promise<TokenResponse> {
  const { data } = await client.post<ApiResponse<TokenResponse>>('/auth/refresh', { refreshToken });
  return data.data;
}

export async function getMe(): Promise<User> {
  const { data } = await client.get<ApiResponse<User>>('/auth/me');
  return data.data;
}

export async function logout(refreshToken: string): Promise<void> {
  await client.post('/auth/logout', { refreshToken });
}
