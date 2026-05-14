import axios, { type InternalAxiosRequestConfig, type AxiosResponse, type AxiosError } from 'axios';
import { REFRESH_TOKEN_STORAGE_KEY } from '@/lib/constants';
import { tokenRegistry } from './tokenRegistry';

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1';

const client = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(token!);
  });
  failedQueue = [];
}

// Inject Bearer token on every request
client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenRegistry.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 401 → silent refresh → retry
client.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error);
    }

    const url = original.url ?? '';
    if (url.includes('/auth/login') || url.includes('/auth/refresh')) {
      // Auth endpoints failed — clear session
      const { useAuthStore } = await import('@/stores/authStore');
      useAuthStore.getState().logout();
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then((token) => {
        original.headers.Authorization = `Bearer ${token}`;
        return client(original);
      });
    }

    original._retry = true;
    isRefreshing = true;

    const refreshToken = localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
    if (!refreshToken) {
      isRefreshing = false;
      const { useAuthStore } = await import('@/stores/authStore');
      useAuthStore.getState().logout();
      return Promise.reject(error);
    }

    try {
      const { data } = await axios.post<{
        data: { accessToken: string; refreshToken: string };
      }>(`${BASE_URL}/auth/refresh`, { refreshToken });

      const { accessToken, refreshToken: newRefresh } = data.data;

      const { useAuthStore } = await import('@/stores/authStore');
      useAuthStore.getState().setTokens(accessToken, newRefresh);

      processQueue(null, accessToken);
      original.headers.Authorization = `Bearer ${accessToken}`;
      return client(original);
    } catch (err) {
      processQueue(err, null);
      const { useAuthStore } = await import('@/stores/authStore');
      useAuthStore.getState().logout();
      return Promise.reject(err);
    } finally {
      isRefreshing = false;
    }
  },
);

export default client;
