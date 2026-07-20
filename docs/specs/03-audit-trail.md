# 03 — Audit Trail

## Goal
Capture **every** system action — who did what, when, and the old→new values — with
special emphasis on ticket state transitions.

## Data model
```
audit_logs(
  id PK,
  ticket_id  FK→tickets(id) NULL,   -- null for user/restriction admin actions
  actor_id   FK→users(user_id),     -- who performed the action
  action     VARCHAR,               -- see catalog below
  field      VARCHAR NULL,          -- e.g. "status", "name"
  old_value  VARCHAR NULL,
  new_value  VARCHAR NULL,
  timestamp  TIMESTAMP              -- server time, UTC
)
```
Audit rows are **append-only** — never updated or deleted via the app.

## Action catalog
| action              | field    | old→new example            |
|---------------------|----------|----------------------------|
| `TICKET_CREATED`    | status   | `null → For Approval`      |
| `TICKET_UPDATED`    | title/description | old text → new text |
| `TICKET_APPROVED_L1`| status   | `For Approval → For Second Approval` |
| `TICKET_APPROVED_L2`| status   | `For Second Approval → In Process` |
| `TICKET_REJECTED`   | status   | `For Approval → Rejected`  |
| `TICKET_INFO_REQUESTED` | status | `For Second Approval → For Additional Info` |
| `TICKET_RESUBMITTED`| status   | `Rejected → For Approval`  |
| `TICKET_RESOLVED`   | status   | `In Process → Done/Resolved` |
| `TICKET_CLOSED`     | status   | `Done/Resolved → Closed`   |
| `USER_CREATED` / `USER_UPDATED` / `USER_DELETED` | — | (incl. `approver`, `approverLevel`, `emailAddress` changes) |
| `RESTRICTION_ADDED` / `RESTRICTION_REMOVED` | category | — |

## Design
- `AuditService.record(actorId, ticketId, action, field, oldValue, newValue)` is the single
  entry point. Called by `TicketService`, `UserService`, `RestrictionService`.
- Writing an audit row participates in the **same transaction** as the mutation, so an
  action and its audit entry commit or roll back together.
- A transition may write one row per changed field; status changes always produce a row.
- Sensitive values (passwords) are never stored — logged as `"***"` or omitted.

## API
| Method & path                         | Purpose |
|---------------------------------------|---------|
| `GET /api/tickets/{id}/audit`         | Chronological audit for one ticket |
| `GET /api/admin/audit?from=&to=&actorId=&action=` | Filtered global log (ADMIN) |

Response item:
```json
{
  "id": 42, "ticketId": 7, "actorId": "1002", "actorName": "Leiva",
  "action": "TICKET_APPROVED", "field": "status",
  "oldValue": "For Approval", "newValue": "In Process",
  "timestamp": "2026-07-15T09:31:22Z"
}
```

## Acceptance criteria
- Moving a ticket through the full lifecycle produces one ordered audit row per transition.
- Each row identifies actor, timestamp, and old/new status.
- Global log is admin-only and filterable by date/actor/action.

## Tests
- Unit: each transition method records the correct action + old/new; passwords never appear.
- Playwright: after driving a ticket New→…→Closed, the ticket audit view lists every step in order.
