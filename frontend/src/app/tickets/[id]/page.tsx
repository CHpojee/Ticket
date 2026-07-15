'use client';

import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import Protected from '@/components/Protected';
import StatusBadge from '@/components/StatusBadge';
import { apiFetch, ApiError } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import type { AuditEntry, Ticket } from '@/lib/types';

const Action = ({
  label, onClick, testid, danger = false,
}: {
  label: string; onClick: () => void; testid: string; danger?: boolean;
}) => (
  <button
    type="button"
    data-testid={testid}
    onClick={onClick}
    className={`rounded px-3 py-1.5 text-sm font-medium text-white ${
      danger ? 'bg-red-600 hover:bg-red-700' : 'bg-brand hover:bg-brand-light'
    }`}
  >
    {label}
  </button>
);

const TicketDetailContent = ({ id }: { id: string }) => {
  const { user } = useAuth();
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [audit, setAudit] = useState<AuditEntry[]>([]);
  const [comment, setComment] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(() => {
    apiFetch<Ticket>(`/api/tickets/${id}`).then(setTicket).catch((e) => setError(e.message));
    apiFetch<AuditEntry[]>(`/api/tickets/${id}/audit`).then(setAudit).catch(() => {});
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const act = async (action: string) => {
    setError('');
    try {
      await apiFetch<Ticket>(`/api/tickets/${id}/${action}`, {
        method: 'POST',
        body: { comment },
      });
      setComment('');
      load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Action failed');
    }
  };

  if (!ticket) return <p className="text-slate-500">{error || 'Loading…'}</p>;

  const isOwner = user?.userId === ticket.requestorId;
  const { status } = ticket;
  const canSubmit = isOwner && ['New', 'Rejected', 'For Additional Info'].includes(status);
  const canDecide = !isOwner && status === 'For Approval';
  const canResolve = !isOwner && status === 'In Process';
  const canClose = isOwner && status === 'Done/Resolved';

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-bold">
          Ticket #
          {ticket.id}
        </h1>
        <StatusBadge status={ticket.status} />
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <h2 data-testid="ticket-title" className="text-lg font-semibold">{ticket.title}</h2>
        <p className="mt-1 text-slate-600">{ticket.description}</p>
        <dl className="mt-3 grid grid-cols-2 gap-2 text-sm md:grid-cols-4">
          <div><dt className="text-slate-400">Category</dt><dd>{ticket.categoryDescription}</dd></div>
          <div><dt className="text-slate-400">Requestor</dt><dd>{ticket.requestorName}</dd></div>
          <div><dt className="text-slate-400">Approver</dt><dd>{ticket.approverName ?? '—'}</dd></div>
          <div>
            <dt className="text-slate-400">Status</dt>
            <dd data-testid="detail-status">{ticket.status}</dd>
          </div>
        </dl>
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="mb-3 font-semibold">Actions</h2>
        {error && <p data-testid="action-error" className="mb-2 text-sm text-red-600">{error}</p>}
        <input
          data-testid="action-comment"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder="Optional comment"
          className="mb-3 w-full rounded border border-slate-300 px-3 py-2 text-sm"
        />
        <div className="flex flex-wrap gap-2">
          {canSubmit && <Action label="Submit for Approval" testid="act-submit" onClick={() => act('submit')} />}
          {canDecide && <Action label="Approve" testid="act-approve" onClick={() => act('approve')} />}
          {canDecide && <Action label="Reject" testid="act-reject" danger onClick={() => act('reject')} />}
          {canDecide && <Action label="Request Info" testid="act-request-info" onClick={() => act('request-info')} />}
          {canResolve && <Action label="Mark Resolved" testid="act-resolve" onClick={() => act('resolve')} />}
          {canClose && <Action label="Close Ticket" testid="act-close" onClick={() => act('close')} />}
          {!canSubmit && !canDecide && !canResolve && !canClose && (
            <span className="text-sm text-slate-400">No actions available for you in this state.</span>
          )}
        </div>
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="mb-3 font-semibold">Audit Trail</h2>
        <table className="w-full text-sm">
          <thead className="text-left text-slate-400">
            <tr>
              <th className="py-1">When</th>
              <th className="py-1">Actor</th>
              <th className="py-1">Action</th>
              <th className="py-1">Change</th>
            </tr>
          </thead>
          <tbody data-testid="audit-table">
            {audit.map((a) => (
              <tr key={a.id} className="border-t border-slate-100">
                <td className="py-1">{new Date(a.timestamp).toLocaleString()}</td>
                <td className="py-1">{a.actorName}</td>
                <td className="py-1">{a.action}</td>
                <td className="py-1 text-slate-500">
                  {a.field}
                  {a.oldValue || a.newValue ? `: ${a.oldValue ?? '∅'} → ${a.newValue ?? '∅'}` : ''}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

const TicketDetailPage = () => {
  const params = useParams();
  const id = Array.isArray(params.id) ? params.id[0] : params.id;
  return (
    <Protected>
      <TicketDetailContent id={id ?? ''} />
    </Protected>
  );
};

export default TicketDetailPage;
