'use client';

import { useCallback, useEffect, useState } from 'react';
import Protected from '@/components/Protected';
import { apiFetch, ApiError } from '@/lib/api';
import { CATEGORIES, type UserDetail } from '@/lib/types';

const AdminContent = () => {
  const [users, setUsers] = useState<UserDetail[]>([]);
  const [error, setError] = useState('');

  const [userId, setUserId] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [formError, setFormError] = useState('');

  const load = useCallback(() => {
    apiFetch<UserDetail[]>('/api/admin/users').then(setUsers).catch((e) => setError(e.message));
  }, []);

  useEffect(() => { load(); }, [load]);

  const createUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError('');
    try {
      await apiFetch<UserDetail>('/api/admin/users', {
        method: 'POST',
        body: { userId, name, password },
      });
      setUserId('');
      setName('');
      setPassword('');
      load();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Failed to create user');
    }
  };

  const addRestriction = async (uid: string, code: string) => {
    setError('');
    try {
      await apiFetch(`/api/admin/users/${uid}/restrictions`, {
        method: 'POST',
        body: { ticketCategoryCode: code },
      });
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to add restriction');
    }
  };

  const removeRestriction = async (uid: string, code: string) => {
    setError('');
    try {
      await apiFetch(`/api/admin/users/${uid}/restrictions/${code}`, { method: 'DELETE' });
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to remove restriction');
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">User Maintenance</h1>

      <form
        onSubmit={createUser}
        data-testid="user-form"
        className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm"
      >
        <h2 className="mb-3 font-semibold">New User</h2>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-4">
          <input data-testid="user-id" value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="User ID" required className="rounded border border-slate-300 px-3 py-2" />
          <input data-testid="user-name" value={name} onChange={(e) => setName(e.target.value)} placeholder="Name" required className="rounded border border-slate-300 px-3 py-2" />
          <input data-testid="user-password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Password" required className="rounded border border-slate-300 px-3 py-2" />
          <button type="submit" data-testid="user-create" className="rounded bg-brand px-4 py-2 font-medium text-white hover:bg-brand-light">Create</button>
        </div>
        {formError && <p data-testid="user-error" className="mt-2 text-sm text-red-600">{formError}</p>}
      </form>

      {error && <p className="text-red-600">{error}</p>}

      <div className="space-y-3">
        {users.map((u) => (
          <div key={u.userId} data-testid={`user-row-${u.userId}`} className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <span className="font-semibold">{u.name}</span>
                <span className="ml-2 text-sm text-slate-500">{u.userId}</span>
                {u.role === 'ROLE_ADMIN' && (
                  <span className="ml-2 rounded bg-brand px-2 py-0.5 text-xs text-white">Admin</span>
                )}
              </div>
            </div>
            <div className="mt-3">
              <p className="text-sm font-medium text-slate-600">Restricted categories:</p>
              <div className="mt-1 flex flex-wrap items-center gap-2">
                {u.restrictions.length === 0 && <span className="text-sm text-slate-400">None</span>}
                {u.restrictions.map((code) => (
                  <span key={code} className="flex items-center gap-1 rounded bg-red-100 px-2 py-0.5 text-xs text-red-800">
                    {code}
                    <button
                      type="button"
                      data-testid={`remove-${u.userId}-${code}`}
                      onClick={() => removeRestriction(u.userId, code)}
                      className="font-bold"
                      aria-label={`Remove ${code} restriction`}
                    >
                      ×
                    </button>
                  </span>
                ))}
                <select
                  data-testid={`add-restriction-${u.userId}`}
                  defaultValue=""
                  onChange={(e) => { if (e.target.value) addRestriction(u.userId, e.target.value); e.target.value = ''; }}
                  className="rounded border border-slate-300 px-2 py-1 text-xs"
                >
                  <option value="">+ Add restriction</option>
                  {CATEGORIES.filter((c) => !u.restrictions.includes(c.code)).map((c) => (
                    <option key={c.code} value={c.code}>{c.code}</option>
                  ))}
                </select>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

const AdminPage = () => (
  <Protected adminOnly>
    <AdminContent />
  </Protected>
);

export default AdminPage;
