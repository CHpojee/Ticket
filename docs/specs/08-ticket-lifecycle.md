# 08 — Ticket Lifecycle & State Machine

The authoritative state machine for tickets. Enforced centrally in `TicketStateMachine`;
illegal transitions throw `InvalidTransitionException` → HTTP `409`.

## Statuses
| Status               | Meaning                                                        |
|----------------------|----------------------------------------------------------------|
| `New`                | Draft. Saved only by the requestor; not yet submitted.         |
| `For Approval`       | Officially submitted; awaiting approver decision.              |
| `Rejected`           | Approver rejected; requestor may modify & resubmit.            |
| `For Additional Info`| Approver needs more info; requestor may modify & resubmit.     |
| `In Process`         | Approved; work underway (auto-entered on approval).            |
| `Done/Resolved`      | Work completed.                                                |
| `Closed`             | Requestor confirmed satisfaction. Terminal.                    |

## Transition table
| From                  | Event / API                    | To                    | Who (actor)              |
|-----------------------|--------------------------------|-----------------------|--------------------------|
| — (create)            | `POST /api/tickets`            | `New`                 | Requestor                |
| `New`                 | `POST …/submit`               | `For Approval`        | Requestor (owner)        |
| `For Approval`        | `POST …/approve`              | `In Process`          | System approver (`Y`, not requestor) |
| `For Approval`        | `POST …/reject`               | `Rejected`            | System approver          |
| `For Approval`        | `POST …/request-info`         | `For Additional Info` | System approver          |
| `Rejected`            | `POST …/submit` (resubmit)    | `For Approval`        | Requestor (owner)        |
| `For Additional Info` | `POST …/submit` (resubmit)    | `For Approval`        | Requestor (owner)        |
| `In Process`          | `POST …/resolve`              | `Done/Resolved`       | System approver          |
| `Done/Resolved`       | `POST …/close`                | `Closed`              | **Original requestor only** |

Any event not listed for the current status → `409 InvalidTransitionException`.

### Editing rules
- Editing ticket fields (`PUT /api/tickets/{id}`) is allowed **only** in
  `New`, `Rejected`, `For Additional Info`, and only by the owner. Otherwise `409`.
- `Closed` and `Done/Resolved` are read-only for field edits.

## Guards applied on each transition
1. **Ownership** — submit/resubmit/close require `actor == requestor`. Else `403`.
2. **Approver identity** — approve/reject/request-info/resolve require actor ≠ requestor **and**
   the actor to be a **system approver** (`approver = 'Y'`, see [02](02-user-maintenance.md)).
   A non-approver gets `403` `{"message":"User <id> is not a system approver"}`.
3. **Restriction** — for every action, `assertAllowed(actorId, ticket.categoryCode)`
   (see [02](02-user-maintenance.md)). E.g. user 1003 cannot act on a `DB` ticket → `403`.
4. **Close ownership** — only the original requestor may close (`Done/Resolved → Closed`). Else `403`.

Guard precedence: authentication (`401`) → transition legality (`409`) →
role/ownership/restriction (`403`).

## Side effects (every successful transition)
1. Persist new `status`, bump `updatedAt`, set `approverId` on first approve/reject.
2. Write an `AuditLog` row (old→new status) — same transaction. See [03](03-audit-trail.md).
3. After commit, send an email notification. See [04](04-email-notification.md).

## API surface (tickets)
| Method & path                    | Action |
|----------------------------------|--------|
| `POST /api/tickets`              | Create draft (`New`) |
| `GET /api/tickets`               | List (filter by status/category/mine) |
| `GET /api/tickets/{id}`          | Detail |
| `PUT /api/tickets/{id}`          | Edit (allowed states only) |
| `POST /api/tickets/{id}/submit`  | New/Rejected/Info → For Approval |
| `POST /api/tickets/{id}/approve` | For Approval → In Process |
| `POST /api/tickets/{id}/reject`  | For Approval → Rejected |
| `POST /api/tickets/{id}/request-info` | For Approval → For Additional Info |
| `POST /api/tickets/{id}/resolve` | In Process → Done/Resolved |
| `POST /api/tickets/{id}/close`   | Done/Resolved → Closed |
| `GET /api/tickets/{id}/audit`    | Audit trail |

Body for decision endpoints may include `{ "comment": "…" }`, stored on the audit row.

## Diagram
```
        create
          │
          ▼
       ┌──────┐  submit   ┌──────────────┐  approve   ┌────────────┐  resolve  ┌───────────────┐  close   ┌────────┐
       │ New  ├──────────▶│ For Approval ├───────────▶│ In Process ├──────────▶│ Done/Resolved ├─────────▶│ Closed │
       └──────┘           └──────┬───────┘            └────────────┘           └───────────────┘          └────────┘
                                 │                                                      ▲ (requestor only)
                    reject ┌─────┴───────┐ request-info
                           ▼             ▼
                      ┌─────────┐  ┌──────────────────────┐
                      │Rejected │  │ For Additional Info  │
                      └────┬────┘  └───────────┬──────────┘
                           │ resubmit          │ resubmit
                           └─────────▶ For Approval ◀──────┘
```

## Acceptance criteria
- Happy path New→For Approval→In Process→Done/Resolved→Closed succeeds with proper actors.
- Reject and request-info loop back to For Approval on resubmit.
- Every illegal transition returns `409`; ownership/restriction violations return `403`.
- Only the original requestor can close.

## Tests
- Unit (`TicketStateMachineTest`, `TicketServiceTest`): full happy path; each illegal
  transition `409`; non-owner submit/close `403`; requestor-approves-own-ticket `403`;
  restricted user (1003) acting on DB `403`; audit + email fired per transition.
- Playwright: end-to-end lifecycle with two users (requestor + approver) and audit check.
