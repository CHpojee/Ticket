'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import Protected from '@/components/Protected';
import StatusBadge from '@/components/StatusBadge';
import { apiFetch, ApiError } from '@/lib/api';
import { CATEGORIES, STATUSES, type Ticket } from '@/lib/types';

const TicketsContent = () => {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [mine, setMine] = useState(false);
  const [status, setStatus] = useState('');
  const [category, setCategory] = useState('');
  const [error, setError] = useState('');

  // Create form
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [newCategory, setNewCategory] = useState('SR');
  const [formError, setFormError] = useState('');

  const load = useCallback(() => {
    const params = new URLSearchParams();
    if (mine) params.set('mine', 'true');
    if (status) params.set('status', status);
    if (category) params.set('category', category);
    apiFetch<Ticket[]>(`/api/tickets?${params.toString()}`)
      .then(setTickets)
      .catch((e) => setError(e.message));
  }, [mine, status, category]);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError('');
    try {
      await apiFetch<Ticket>('/api/tickets', {
        method: 'POST',
        body: { title, description, categoryCode: newCategory },
      });
      setTitle('');
      setDescription('');
      setNewCategory('SR');
      load();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Failed to create ticket');
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Tickets</h1>

      <form
        onSubmit={handleCreate}
        data-testid="create-form"
        className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm"
      >
        <h2 className="mb-3 font-semibold">New Ticket</h2>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
          <input
            data-testid="ticket-title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Title"
            required
            className="rounded border border-slate-300 px-3 py-2"
          />
          <input
            data-testid="ticket-description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Description"
            className="rounded border border-slate-300 px-3 py-2 md:col-span-2"
          />
        </div>
        <div className="mt-3 flex items-center gap-3">
          <select
            data-testid="ticket-category"
            value={newCategory}
            onChange={(e) => setNewCategory(e.target.value)}
            className="rounded border border-slate-300 px-3 py-2"
          >
            {CATEGORIES.map((c) => (
              <option key={c.code} value={c.code}>{`${c.code} — ${c.description}`}</option>
            ))}
          </select>
          <button
            type="submit"
            data-testid="ticket-create"
            className="rounded bg-brand px-4 py-2 font-medium text-white hover:bg-brand-light"
          >
            Create Draft
          </button>
          {formError && <span data-testid="create-error" className="text-sm text-red-600">{formError}</span>}
        </div>
      </form>

      <div className="flex flex-wrap items-center gap-3">
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={mine} onChange={(e) => setMine(e.target.checked)} />
          My tickets
        </label>
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="rounded border border-slate-300 px-2 py-1 text-sm"
        >
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <select
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          className="rounded border border-slate-300 px-2 py-1 text-sm"
        >
          <option value="">All categories</option>
          {CATEGORIES.map((c) => <option key={c.code} value={c.code}>{c.code}</option>)}
        </select>
      </div>

      {error && <p className="text-red-600">{error}</p>}

      <table className="w-full overflow-hidden rounded-lg border border-slate-200 bg-white text-sm shadow-sm">
        <thead className="bg-slate-100 text-left">
          <tr>
            <th className="px-3 py-2">#</th>
            <th className="px-3 py-2">Title</th>
            <th className="px-3 py-2">Category</th>
            <th className="px-3 py-2">Status</th>
            <th className="px-3 py-2">Requestor</th>
          </tr>
        </thead>
        <tbody data-testid="tickets-table">
          {tickets.map((t) => (
            <tr key={t.id} className="border-t border-slate-100 hover:bg-slate-50">
              <td className="px-3 py-2">{t.id}</td>
              <td className="px-3 py-2">
                <Link href={`/tickets/${t.id}`} className="text-brand underline">{t.title}</Link>
              </td>
              <td className="px-3 py-2">{t.categoryCode}</td>
              <td className="px-3 py-2"><StatusBadge status={t.status} /></td>
              <td className="px-3 py-2">{t.requestorName}</td>
            </tr>
          ))}
          {tickets.length === 0 && (
            <tr><td colSpan={5} className="px-3 py-6 text-center text-slate-400">No tickets</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

const TicketsPage = () => (
  <Protected>
    <TicketsContent />
  </Protected>
);

export default TicketsPage;
