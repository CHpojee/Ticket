'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';

/** Client-side guard: redirects to /login when there is no authenticated user. */
const Protected = ({ children, adminOnly = false }: {
  children: React.ReactNode;
  adminOnly?: boolean;
}) => {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    if (!user) router.replace('/login');
    else if (adminOnly && user.role !== 'ROLE_ADMIN') router.replace('/dashboard');
  }, [user, loading, adminOnly, router]);

  if (loading) return <p className="text-slate-500">Loading…</p>;
  if (!user) return null;
  if (adminOnly && user.role !== 'ROLE_ADMIN') return null;
  // eslint-disable-next-line react/jsx-no-useless-fragment
  return <>{children}</>;
};

export default Protected;
