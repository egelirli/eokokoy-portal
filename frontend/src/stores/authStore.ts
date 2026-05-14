import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { tokenRegistry } from '@/api/tokenRegistry';
import { REFRESH_TOKEN_STORAGE_KEY } from '@/lib/constants';
import type { User } from '@/types/user.types';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  isInitialized: boolean;

  setAuth: (user: User, accessToken: string, refreshToken: string) => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  setUser: (user: User) => void;
  logout: () => void;
  initialize: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      isInitialized: false,

      setAuth: (user, accessToken, refreshToken) => {
        tokenRegistry.set(accessToken);
        localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
        set({ user, accessToken });
      },

      setTokens: (accessToken, refreshToken) => {
        tokenRegistry.set(accessToken);
        localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
        set({ accessToken });
      },

      setUser: (user) => set({ user }),

      logout: () => {
        tokenRegistry.set(null);
        localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
        set({ user: null, accessToken: null });
      },

      initialize: () => {
        const { accessToken } = get();
        if (accessToken) tokenRegistry.set(accessToken);
        set({ isInitialized: true });
      },
    }),
    {
      name: 'ekokoy-auth',
      // Only persist user and accessToken; refresh token goes in localStorage separately
      partialize: (state) => ({ user: state.user, accessToken: state.accessToken }),
      onRehydrateStorage: () => (state) => {
        if (state?.accessToken) tokenRegistry.set(state.accessToken);
        state?.initialize();
      },
    },
  ),
);
