const COLORS: Record<string, string> = {
  New: 'bg-neutral-100 text-neutral-700 ring-neutral-200',
  'For Approval': 'bg-amber-50 text-amber-700 ring-amber-200',
  Rejected: 'bg-rose-50 text-rose-700 ring-rose-200',
  'For Additional Info': 'bg-orange-50 text-orange-700 ring-orange-200',
  'In Process': 'bg-sky-50 text-sky-700 ring-sky-200',
  'Done/Resolved': 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  Closed: 'bg-emerald-600 text-white ring-emerald-600',
};

const StatusBadge = ({ status }: { status: string }) => (
  <span
    data-testid="status-badge"
    className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${
      COLORS[status] ?? 'bg-neutral-100 text-neutral-700 ring-neutral-200'
    }`}
  >
    {status}
  </span>
);

export default StatusBadge;
