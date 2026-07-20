const COLORS: Record<string, string> = {
  'For Approval': 'bg-gold/15 text-gold-dark ring-gold/30',
  'For Second Approval': 'bg-amber-100 text-amber-800 ring-amber-200',
  Rejected: 'bg-rose-50 text-rose-700 ring-rose-200',
  'For Additional Info': 'bg-orange-50 text-orange-700 ring-orange-200',
  'In Process': 'bg-sky-50 text-sky-700 ring-sky-200',
  'Done/Resolved': 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  Closed: 'bg-ink text-white ring-ink',
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
