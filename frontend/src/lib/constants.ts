export const APP_NAME = import.meta.env.VITE_APP_NAME ?? 'Ekoköy Portalı';
export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1';

export const TOKEN_STORAGE_KEY = 'ekokoy_access_token';
export const REFRESH_TOKEN_STORAGE_KEY = 'ekokoy_refresh_token';

export const ROLES = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  YONETIM_KURULU: 'YONETIM_KURULU',
  EV_SAHIBI: 'EV_SAHIBI',
  KIRACI: 'KIRACI',
  AILE_BIREYI: 'AILE_BIREYI',
  CALISAN: 'CALISAN',
  ZIYARETCI: 'ZIYARETCI',
} as const;

export const ADMIN_ROLES = [ROLES.SUPER_ADMIN, ROLES.YONETIM_KURULU] as const;
