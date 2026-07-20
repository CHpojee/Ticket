'use client';

import { useEffect, useState } from 'react';
import {
  Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer,
  Tooltip, XAxis, YAxis,
} from 'recharts';
import Protected from '@/components/Protected';
import { apiFetch } from '@/lib/api';
import type { DashboardSummary } from '@/lib/types';

const PIE_COLORS = ['#e8aa34', '#d0952a', '#626466', '#f0c675', '#8a6d1f', '#b9a06a', '#222222'];

const Card = ({ label, value, testid }: { label: string; value: number; testid: string }) => (
  <div className="card p-5 transition-shadow hover:shadow-elevate">
    <p className="text-sm text-muted">{label}</p>
    <p data-testid={testid} className="mt-2 text-3xl font-bold text-ink">{value}</p>
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

  if (error) return <p className="text-rausch">{error}</p>;
  if (!data) return <p className="text-muted">Loading…</p>;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-ink">Dashboard</h1>
        <p className="mt-1 text-muted">Overview of tickets across the approval cycle</p>
      </div>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <Card label="Total Tickets" value={data.totalTickets} testid="card-total" />
        <Card label="Total Open" value={data.totalOpen} testid="card-open" />
        <Card label="Pending Approvals" value={data.pendingApprovals} testid="card-pending" />
        <Card label="Completed" value={data.completed} testid="card-completed" />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="card p-5">
          <h2 className="mb-4 text-lg font-semibold text-ink">Tickets by Category</h2>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={data.byCategory} barCategoryGap="30%">
              <CartesianGrid strokeDasharray="3 3" stroke="#EBEBEB" vertical={false} />
              <XAxis dataKey="code" tickLine={false} axisLine={{ stroke: '#DDDDDD' }} />
              <YAxis allowDecimals={false} tickLine={false} axisLine={false} />
              <Tooltip cursor={{ fill: 'rgba(232,170,52,0.10)' }} />
              <Bar dataKey="count" fill="#e8aa34" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="card p-5">
          <h2 className="mb-4 text-lg font-semibold text-ink">Tickets by Status</h2>
          <ResponsiveContainer width="100%" height={280}>
            <PieChart>
              <Pie
                data={data.byStatus.filter((s) => s.count > 0)}
                dataKey="count"
                nameKey="status"
                innerRadius={55}
                outerRadius={95}
                paddingAngle={2}
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
