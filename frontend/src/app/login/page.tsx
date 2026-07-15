'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import { ApiError } from '@/lib/api';

const LoginPage = () => {
  const { user, login } = useAuth();
  const router = useRouter();
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (user) router.replace('/dashboard');
  }, [user, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await login(userId, password);
      router.replace('/dashboard');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Login failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto mt-16 max-w-sm rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <h1 className="mb-1 text-2xl font-bold text-brand">Internal IT Support</h1>
      <p className="mb-6 text-sm text-slate-500">Sign in with your user ID</p>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="userId" className="block text-sm font-medium">
            User ID
            <input
              id="userId"
              data-testid="login-userId"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              autoComplete="username"
            />
          </label>
        </div>
        <div>
          <label htmlFor="password" className="block text-sm font-medium">
            Password
            <input
              id="password"
              data-testid="login-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
              autoComplete="current-password"
            />
          </label>
        </div>
        {error && (
          <p data-testid="login-error" className="text-sm text-red-600">{error}</p>
        )}
        <button
          type="submit"
          data-testid="login-submit"
          disabled={submitting}
          className="w-full rounded bg-brand py-2 font-medium text-white hover:bg-brand-light disabled:opacity-50"
        >
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </div>
  );
};

export default LoginPage;
