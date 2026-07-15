'use client';

import { useState } from 'react';
import Protected from '@/components/Protected';
import { API_BASE, getToken } from '@/lib/api';
import { CATEGORIES, STATUSES } from '@/lib/types';

const ReportsContent = () => {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [categories, setCategories] = useState<string[]>([]);
  const [statuses, setStatuses] = useState<string[]>([]);
  const [format, setFormat] = useState('csv');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const toggle = (list: string[], value: string, setter: (v: string[]) => void) => {
    setter(list.includes(value) ? list.filter((v) => v !== value) : [...list, value]);
  };

  const download = async () => {
    setError('');
    setBusy(true);
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    categories.forEach((c) => params.append('category', c));
    statuses.forEach((s) => params.append('status', s));
    params.set('format', format);
    try {
      const res = await fetch(`${API_BASE}/api/reports/tickets?${params.toString()}`, {
        headers: { Authorization: `Bearer ${getToken()}` },
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.message ?? `Export failed (${res.status})`);
      }
      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `tickets.${format}`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export failed');
    } finally {
      setBusy(false);
    }
  };

  const chip = (checked: boolean) => `cursor-pointer rounded-full border px-3 py-1.5 text-sm transition-colors ${
    checked ? 'border-ink bg-ink text-white' : 'border-hairline text-ink hover:border-ink'
  }`;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-ink">Generate Report</h1>
        <p className="mt-1 text-muted">Export tickets to CSV or Excel, filtered to your needs</p>
      </div>

      <div className="card space-y-6 p-6">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <span className="mb-1 block text-sm font-medium text-ink">From</span>
            <input data-testid="report-from" type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="field" />
          </div>
          <div>
            <span className="mb-1 block text-sm font-medium text-ink">To</span>
            <input data-testid="report-to" type="date" value={to} onChange={(e) => setTo(e.target.value)} className="field" />
          </div>
        </div>

        <div>
          <span className="mb-2 block text-sm font-medium text-ink">Categories</span>
          <div className="flex flex-wrap gap-2">
            {CATEGORIES.map((c) => (
              <label key={c.code} className={chip(categories.includes(c.code))}>
                <input type="checkbox" className="sr-only" checked={categories.includes(c.code)} onChange={() => toggle(categories, c.code, setCategories)} />
                {c.code}
              </label>
            ))}
          </div>
        </div>

        <div>
          <span className="mb-2 block text-sm font-medium text-ink">Statuses</span>
          <div className="flex flex-wrap gap-2">
            {STATUSES.map((s) => (
              <label key={s} className={chip(statuses.includes(s))}>
                <input type="checkbox" className="sr-only" checked={statuses.includes(s)} onChange={() => toggle(statuses, s, setStatuses)} />
                {s}
              </label>
            ))}
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3 border-t border-hairline pt-5">
          <select data-testid="report-format" value={format} onChange={(e) => setFormat(e.target.value)} className="field w-auto">
            <option value="csv">CSV</option>
            <option value="xlsx">Excel (.xlsx)</option>
          </select>
          <button type="button" data-testid="report-download" disabled={busy} onClick={download} className="btn-primary">
            {busy ? 'Generating…' : 'Download'}
          </button>
          {error && <span data-testid="report-error" className="text-sm text-rausch">{error}</span>}
        </div>
      </div>
    </div>
  );
};

const ReportsPage = () => (
  <Protected>
    <ReportsContent />
  </Protected>
);

export default ReportsPage;
