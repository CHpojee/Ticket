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
    className={danger
      ? 'inline-flex items-center justify-center rounded-lg border border-rausch px-4 py-2 text-sm font-semibold text-rausch transition-colors hover:bg-rausch/5'
      : 'btn-primary text-sm'}
  >
    {label}
  </button>
);

const Field = ({ label, value, testid }: { label: string; value: string; testid?: string }) => (
  <div>
    <dt className="text-xs uppercase tracking-wide text-muted">{label}</dt>
    <dd data-testid={testid} className="mt-0.5 text-ink">{value}</dd>
  </div>
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

  if (!ticket) return <p className="text-muted">{error || 'Loading…'}</p>;

  const isOwner = user?.userId === ticket.requestorId;
  const isApprover = user?.approver ?? false;
  const { status } = ticket;
  const canSubmit = isOwner && ['New', 'Rejected', 'For Additional Info'].includes(status);
  const canDecide = !isOwner && isApprover && status === 'For Approval';
  const canResolve = !isOwner && isApprover && status === 'In Process';
  const canClose = isOwner && status === 'Done/Resolved';
  const hasActions = canSubmit || canDecide || canResolve || canClose;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-3xl font-bold text-ink">{`Ticket #${ticket.id}`}</h1>
        <StatusBadge status={ticket.status} />
      </div>

      <div className="card p-6">
        <h2 data-testid="ticket-title" className="text-xl font-semibold text-ink">{ticket.title}</h2>
        <p className="mt-1 text-neutral-700">{ticket.description}</p>
        <dl className="mt-5 grid grid-cols-2 gap-4 md:grid-cols-4">
          <Field label="Category" value={ticket.categoryDescription} />
          <Field label="Requestor" value={ticket.requestorName} />
          <Field label="Approver" value={ticket.approverName ?? '—'} testid="detail-approver" />
          <Field label="Status" value={ticket.status} testid="detail-status" />
        </dl>
      </div>

      <div className="card p-6">
        <h2 className="mb-4 text-lg font-semibold text-ink">Actions</h2>
        {error && <p data-testid="action-error" className="mb-3 text-sm text-rausch">{error}</p>}
        <input
          data-testid="action-comment"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder="Optional comment"
          className="field mb-4"
        />
        <div className="flex flex-wrap gap-2">
          {canSubmit && <Action label="Submit for Approval" testid="act-submit" onClick={() => act('submit')} />}
          {canDecide && <Action label="Approve" testid="act-approve" onClick={() => act('approve')} />}
          {canDecide && <Action label="Reject" testid="act-reject" danger onClick={() => act('reject')} />}
          {canDecide && <Action label="Request Info" testid="act-request-info" onClick={() => act('request-info')} />}
          {canResolve && <Action label="Mark Resolved" testid="act-resolve" onClick={() => act('resolve')} />}
          {canClose && <Action label="Close Ticket" testid="act-close" onClick={() => act('close')} />}
          {!hasActions && (
            <span className="text-sm text-muted">No actions available for you in this state.</span>
          )}
        </div>
      </div>

      <div className="card p-6">
        <h2 className="mb-4 text-lg font-semibold text-ink">Audit Trail</h2>
        <table className="w-full text-sm">
          <thead className="text-left text-muted">
            <tr>
              <th className="pb-2 font-medium">When</th>
              <th className="pb-2 font-medium">Actor</th>
              <th className="pb-2 font-medium">Action</th>
              <th className="pb-2 font-medium">Change</th>
            </tr>
          </thead>
          <tbody data-testid="audit-table">
            {audit.map((a) => (
              <tr key={a.id} className="border-t border-hairline">
                <td className="py-2 text-neutral-600">{new Date(a.timestamp).toLocaleString()}</td>
                <td className="py-2">{a.actorName}</td>
                <td className="py-2">
                  <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-xs font-medium text-ink">
                    {a.action}
                  </span>
                </td>
                <td className="py-2 text-muted">
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
