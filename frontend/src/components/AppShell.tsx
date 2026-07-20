'use client';

import { useAuth } from '@/lib/auth';
import Sidebar from './Sidebar';
import Header from './Header';

/**
 * Flexy-style admin shell: fixed left sidebar + top header on authenticated pages,
 * and a plain centered canvas for unauthenticated pages (login).
 */
const AppShell = ({ children }: { children: React.ReactNode }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return <div className="p-8 text-muted">Loading…</div>;
  }

  if (!user) {
    return <main className="min-h-screen px-4">{children}</main>;
  }

  return (
    <div className="min-h-screen">
      <Sidebar />
      <div className="flex min-h-screen flex-col md:pl-64">
        <Header />
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 md:px-8">{children}</main>
      </div>
    </div>
  );
};

export default AppShell;
