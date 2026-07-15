'use client';

import { useEffect, useState } from 'react';
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer,
  Tooltip, XAxis, YAxis,
} from 'recharts';
import Protected from '@/components/Protected';
import { apiFetch } from '@/lib/api';
import type { DashboardSummary } from '@/lib/types';

const PIE_COLORS = ['#94a3b8', '#f59e0b', '#ef4444', '#fb923c', '#3b82f6', '#10b981', '#15803d'];

const Card = ({ label, value, testid }: { label: string; value: number; testid: string }) => (
  <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
    <p className="text-sm text-slate-500">{label}</p>
    <p data-testid={testid} className="mt-1 text-3xl font-bold text-brand">{value}</p>
  </div>
);

const DashboardContent = () => {
  const [data, setData] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    apiFetch<DashboardSummary>('/api/dashboard/summary')
      .then(setData)
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <p className="text-red-600">{error}</p>;
  if (!data) return <p className="text-slate-500">Loading…</p>;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Dashboard</h1>
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <Card label="Total Tickets" value={data.totalTickets} testid="card-total" />
        <Card label="Total Open" value={data.totalOpen} testid="card-open" />
        <Card label="Pending Approvals" value={data.pendingApprovals} testid="card-pending" />
        <Card label="Completed" value={data.completed} testid="card-completed" />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <h2 className="mb-3 font-semibold">Tickets by Category</h2>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={data.byCategory}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="code" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" fill="#3b5bdb" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <h2 className="mb-3 font-semibold">Tickets by Status</h2>
          <ResponsiveContainer width="100%" height={280}>
            <PieChart>
              <Pie
                data={data.byStatus.filter((s) => s.count > 0)}
                dataKey="count"
                nameKey="status"
                outerRadius={90}
                label
              >
                {data.byStatus.filter((s) => s.count > 0).map((entry, i) => (
                  <Cell key={entry.status} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
};

const DashboardPage = () => (
  <Protected>
    <DashboardContent />
  </Protected>
);

export default DashboardPage;
