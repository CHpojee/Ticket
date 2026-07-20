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
  const [emailAddress, setEmailAddress] = useState('');
  const [approver, setApprover] = useState(false);
  const [approverLevel, setApproverLevel] = useState(1);
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
        body: {
          userId,
          name,
          password,
          approver,
          approverLevel: approver ? approverLevel : null,
          emailAddress,
        },
      });
      setUserId('');
      setName('');
      setPassword('');
      setEmailAddress('');
      setApprover(false);
      setApproverLevel(1);
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
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-ink">User Maintenance</h1>
        <p className="mt-1 text-muted">Manage users and category restrictions</p>
      </div>

      <form onSubmit={createUser} data-testid="user-form" className="card p-6">
        <h2 className="mb-4 text-lg font-semibold text-ink">New User</h2>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
          <input data-testid="user-id" value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="User ID" required className="field" />
          <input data-testid="user-name" value={name} onChange={(e) => setName(e.target.value)} placeholder="Name" required className="field" />
          <input data-testid="user-password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Password" required className="field" />
          <input data-testid="user-email" type="email" value={emailAddress} onChange={(e) => setEmailAddress(e.target.value)} placeholder="Email address (optional)" className="field md:col-span-2" />
          <label className="flex items-center gap-2 text-sm text-ink">
            <input data-testid="user-approver" type="checkbox" checked={approver} onChange={(e) => setApprover(e.target.checked)} className="h-4 w-4 accent-gold" />
            System approver
          </label>
          {approver && (
            <label className="flex items-center gap-2 text-sm text-ink">
              Level
              <select
                data-testid="user-approver-level"
                value={approverLevel}
                onChange={(e) => setApproverLevel(Number(e.target.value))}
                className="rounded-lg border border-hairline px-2 py-1"
              >
                <option value={1}>1 — first approval</option>
                <option value={2}>2 — second approval</option>
              </select>
            </label>
          )}
        </div>
        <div className="mt-3">
          <button type="submit" data-testid="user-create" className="btn-primary">Create User</button>
        </div>
        {formError && <p data-testid="user-error" className="mt-3 text-sm text-rausch">{formError}</p>}
      </form>

      {error && <p className="text-rausch">{error}</p>}

      <div className="space-y-3">
        {users.map((u) => (
          <div key={u.userId} data-testid={`user-row-${u.userId}`} className="card p-5">
            <div className="flex items-center gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-full bg-ink text-sm font-semibold text-white">
                {u.name.charAt(0)}
              </span>
              <div>
                <span className="font-semibold text-ink">{u.name}</span>
                <span className="ml-2 text-sm text-muted">{u.userId}</span>
                <span className="ml-2 text-sm text-muted">{u.emailAddress ?? 'no email'}</span>
              </div>
              {u.role === 'ROLE_ADMIN' && (
                <span className="ml-1 rounded-full bg-rausch px-2.5 py-0.5 text-xs font-semibold text-white">
                  Admin
                </span>
              )}
              {u.approver && (
                <span className="ml-1 rounded-full bg-gold/20 px-2.5 py-0.5 text-xs font-semibold text-gold-dark ring-1 ring-inset ring-gold/40">
                  {u.approverLevel ? `Approver · L${u.approverLevel}` : 'Approver'}
                </span>
              )}
            </div>
            <div className="mt-4 border-t border-hairline pt-4">
              <p className="text-sm font-medium text-ink">Restricted categories</p>
              <div className="mt-2 flex flex-wrap items-center gap-2">
                {u.restrictions.length === 0 && <span className="text-sm text-muted">None</span>}
                {u.restrictions.map((code) => (
                  <span key={code} className="flex items-center gap-1.5 rounded-full bg-rose-50 px-2.5 py-1 text-xs font-medium text-rose-700 ring-1 ring-inset ring-rose-200">
                    {code}
                    <button
                      type="button"
                      data-testid={`remove-${u.userId}-${code}`}
                      onClick={() => removeRestriction(u.userId, code)}
                      className="font-bold leading-none hover:text-rose-900"
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
                  className="rounded-full border border-hairline px-3 py-1 text-xs text-ink focus:border-ink focus:outline-none"
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
