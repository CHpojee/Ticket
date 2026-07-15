'use client';

import {
  createContext, useContext, useEffect, useMemo, useState,
} from 'react';
import {
  apiFetch, clearToken, getToken, setToken,
} from './api';
import type { AuthUser, LoginResponse } from './types';

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  login: (userId: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      setLoading(false);
      return;
    }
    apiFetch<AuthUser>('/api/auth/me')
      .then((me) => setUser(me))
      .catch(() => clearToken())
      .finally(() => setLoading(false));
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    loading,
    login: async (userId: string, password: string) => {
      const res = await apiFetch<LoginResponse>('/api/auth/login', {
        method: 'POST',
        body: { userId, password },
        auth: false,
      });
      setToken(res.token);
      setUser(res.user);
    },
    logout: () => {
      clearToken();
      setUser(null);
    },
  }), [user, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextValue => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
