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
    <div className="flex min-h-[calc(100vh-140px)] items-center justify-center">
      <div className="w-full max-w-sm">
        <div className="mb-6 flex flex-col items-center text-center">
          <span className="mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-ink text-2xl font-black text-gold">
            S
          </span>
          <h1 className="text-3xl font-black tracking-widest text-ink">STICK</h1>
          <p className="mt-1 text-sm text-muted">Internal IT Support — sign in to continue</p>
        </div>

        <div className="card p-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="userId" className="mb-1 block text-sm font-medium text-ink">
                User ID
              </label>
              <input
                id="userId"
                data-testid="login-userId"
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                className="field"
                autoComplete="username"
                placeholder="e.g. 1001"
              />
            </div>
            <div>
              <label htmlFor="password" className="mb-1 block text-sm font-medium text-ink">
                Password
              </label>
              <input
                id="password"
                data-testid="login-password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="field"
                autoComplete="current-password"
                placeholder="••••••"
              />
            </div>
            {error && (
              <p data-testid="login-error" className="text-sm text-rausch">{error}</p>
            )}
            <button
              type="submit"
              data-testid="login-submit"
              disabled={submitting}
              className="btn-primary w-full"
            >
              {submitting ? 'Signing in…' : 'Sign in'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
