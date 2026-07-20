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
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-ink">Tickets</h1>
        <p className="mt-1 text-muted">Raise a request and track it through the approval cycle</p>
      </div>

      <form onSubmit={handleCreate} data-testid="create-form" className="card p-6">
        <h2 className="mb-4 text-lg font-semibold text-ink">New Ticket</h2>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
          <input
            data-testid="ticket-title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Title"
            required
            className="field"
          />
          <input
            data-testid="ticket-description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Description"
            className="field md:col-span-2"
          />
        </div>
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <select
            data-testid="ticket-category"
            value={newCategory}
            onChange={(e) => setNewCategory(e.target.value)}
            className="field w-auto"
          >
            {CATEGORIES.map((c) => (
              <option key={c.code} value={c.code}>{`${c.code} — ${c.description}`}</option>
            ))}
          </select>
          <button type="submit" data-testid="ticket-create" className="btn-primary">
            Submit Ticket
          </button>
          {formError && (
            <span data-testid="create-error" className="text-sm text-rausch">{formError}</span>
          )}
        </div>
      </form>

      <div className="flex flex-wrap items-center gap-3">
        <label className="flex items-center gap-2 text-sm text-ink">
          <input
            type="checkbox"
            checked={mine}
            onChange={(e) => setMine(e.target.checked)}
            className="h-4 w-4 accent-rausch"
          />
          My tickets
        </label>
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="rounded-full border border-hairline px-3.5 py-1.5 text-sm text-ink focus:border-ink focus:outline-none"
        >
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <select
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          className="rounded-full border border-hairline px-3.5 py-1.5 text-sm text-ink focus:border-ink focus:outline-none"
        >
          <option value="">All categories</option>
          {CATEGORIES.map((c) => <option key={c.code} value={c.code}>{c.code}</option>)}
        </select>
      </div>

      {error && <p className="text-rausch">{error}</p>}

      <div className="card overflow-hidden">
        <table className="w-full text-sm">
          <thead className="border-b border-hairline bg-neutral-50 text-left text-muted">
            <tr>
              <th className="px-4 py-3 font-medium">#</th>
              <th className="px-4 py-3 font-medium">Title</th>
              <th className="px-4 py-3 font-medium">Category</th>
              <th className="px-4 py-3 font-medium">Status</th>
              <th className="px-4 py-3 font-medium">Requestor</th>
            </tr>
          </thead>
          <tbody data-testid="tickets-table">
            {tickets.map((t) => (
              <tr key={t.id} className="border-t border-hairline transition-colors hover:bg-neutral-50">
                <td className="px-4 py-3 text-muted">{t.id}</td>
                <td className="px-4 py-3">
                  <Link href={`/tickets/${t.id}`} className="font-medium text-ink hover:text-rausch">
                    {t.title}
                  </Link>
                </td>
                <td className="px-4 py-3">{t.categoryCode}</td>
                <td className="px-4 py-3"><StatusBadge status={t.status} /></td>
                <td className="px-4 py-3">{t.requestorName}</td>
              </tr>
            ))}
            {tickets.length === 0 && (
              <tr><td colSpan={5} className="px-4 py-10 text-center text-neutral-400">No tickets yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

const TicketsPage = () => (
  <Protected>
    <TicketsContent />
  </Protected>
);

export default TicketsPage;
