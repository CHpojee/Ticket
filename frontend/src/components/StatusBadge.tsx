const COLORS: Record<string, string> = {
  New: 'bg-slate-200 text-slate-800',
  'For Approval': 'bg-amber-200 text-amber-900',
  Rejected: 'bg-red-200 text-red-900',
  'For Additional Info': 'bg-orange-200 text-orange-900',
  'In Process': 'bg-blue-200 text-blue-900',
  'Done/Resolved': 'bg-emerald-200 text-emerald-900',
  Closed: 'bg-green-700 text-white',
};

const StatusBadge = ({ status }: { status: string }) => (
  <span
    data-testid="status-badge"
    className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${
      COLORS[status] ?? 'bg-slate-200 text-slate-800'
    }`}
  >
    {status}
  </span>
);

export default StatusBadge;
