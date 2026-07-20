# 00 — System Overview & Architecture

## Purpose
**Internal IT Support** is an internal ticketing system with a formal approval cycle.
It lets employees raise IT tickets, routes them through an approval workflow, records a
complete audit trail, notifies stakeholders by email on every state change, and gives
admins dashboards, reports, and user management.

## Actors
| Actor      | Description                                                              |
|------------|--------------------------------------------------------------------------|
| Requestor  | Any authenticated user who creates and later closes their tickets.       |
| Approver   | `approver='Y'` user; **level 1** (Leiva) approves first, **level 2** (Rudy) second. |
| Admin      | userId **1001**; manages users, approver levels, and restrictions.       |

## Core domain objects
- **User** — login identity (`userId`, `password`, `name`).
- **TicketCategory** — `SR`, `DB`, `MR`, `BW`, `IR`.
- **UserRestriction** — bars a user from acting on a category (seed: 1003 → DB).
- **Ticket** — the work item; moves through the status lifecycle.
- **AuditLog** — immutable record of every mutation.

## High-level flow (two-stage approval, no draft)
```
Requestor creates ──▶ For Approval
For Approval ──approve(L1)──▶ For Second Approval ──approve(L2)──▶ In Process
In Process ──resolve──▶ Done/Resolved ──close(by requestor)──▶ Closed
For Approval / For Second Approval ──reject──▶ Rejected ──resubmit──▶ For Approval
For Approval / For Second Approval ──need info──▶ For Additional Info ──resubmit──▶ For Approval
```
Full rules: [08-ticket-lifecycle.md](08-ticket-lifecycle.md).

## Cross-cutting rules
- **Audit trail**: every create/update/transition writes an `AuditLog` row
  (who, what, when, old→new). See [03-audit-trail.md](03-audit-trail.md).
- **Email**: every state change enqueues a notification. See
  [04-email-notification.md](04-email-notification.md).
- **Restrictions**: services call `assertAllowed(userId, categoryCode)` before any
  category-specific action. See [02-user-maintenance.md](02-user-maintenance.md).

## API conventions
- Base path `/api`. JSON only. JWT bearer auth except `/api/auth/login`.
- Errors returned as `ErrorResponse { timestamp, status, error, message, path }`.
- Standard codes: `400` validation, `401` unauthenticated, `403` restriction/role,
  `404` not found, `409` illegal state transition.

## Feature index
| # | Feature              | Spec |
|---|----------------------|------|
| 1 | User Login           | [01-user-login.md](01-user-login.md) |
| 2 | User Maintenance     | [02-user-maintenance.md](02-user-maintenance.md) |
| 3 | Audit Trail          | [03-audit-trail.md](03-audit-trail.md) |
| 4 | Email Notification   | [04-email-notification.md](04-email-notification.md) |
| 5 | Dashboard            | [05-dashboard.md](05-dashboard.md) |
| 6 | Generate Report      | [06-generate-report.md](06-generate-report.md) |
| 7 | Specs Viewer         | [07-specs-viewer.md](07-specs-viewer.md) |
| — | Ticket Lifecycle     | [08-ticket-lifecycle.md](08-ticket-lifecycle.md) |
