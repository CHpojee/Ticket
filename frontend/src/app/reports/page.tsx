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

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Generate Report</h1>
      <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm space-y-4">
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <label className="text-sm font-medium">
            From
            <input data-testid="report-from" type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="mt-1 block w-full rounded border border-slate-300 px-3 py-2" />
          </label>
          <label className="text-sm font-medium">
            To
            <input data-testid="report-to" type="date" value={to} onChange={(e) => setTo(e.target.value)} className="mt-1 block w-full rounded border border-slate-300 px-3 py-2" />
          </label>
        </div>

        <fieldset>
          <legend className="text-sm font-medium">Categories</legend>
          <div className="mt-1 flex flex-wrap gap-3">
            {CATEGORIES.map((c) => (
              <label key={c.code} className="flex items-center gap-1 text-sm">
                <input type="checkbox" checked={categories.includes(c.code)} onChange={() => toggle(categories, c.code, setCategories)} />
                {c.code}
              </label>
            ))}
          </div>
        </fieldset>

        <fieldset>
          <legend className="text-sm font-medium">Statuses</legend>
          <div className="mt-1 flex flex-wrap gap-3">
            {STATUSES.map((s) => (
              <label key={s} className="flex items-center gap-1 text-sm">
                <input type="checkbox" checked={statuses.includes(s)} onChange={() => toggle(statuses, s, setStatuses)} />
                {s}
              </label>
            ))}
          </div>
        </fieldset>

        <div className="flex items-center gap-3">
          <select data-testid="report-format" value={format} onChange={(e) => setFormat(e.target.value)} className="rounded border border-slate-300 px-3 py-2 text-sm">
            <option value="csv">CSV</option>
            <option value="xlsx">Excel (.xlsx)</option>
          </select>
          <button type="button" data-testid="report-download" disabled={busy} onClick={download} className="rounded bg-brand px-4 py-2 font-medium text-white hover:bg-brand-light disabled:opacity-50">
            {busy ? 'Generating…' : 'Download'}
          </button>
        </div>
        {error && <p data-testid="report-error" className="text-sm text-red-600">{error}</p>}
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
