# 08 — Ticket Lifecycle & State Machine

The authoritative state machine for tickets. Enforced centrally in `TicketStateMachine`;
illegal transitions throw `InvalidTransitionException` → HTTP `409`.

## Statuses
There is **no draft state** — a created ticket is submitted for approval immediately.
| Status                | Meaning                                                        |
|-----------------------|----------------------------------------------------------------|
| `For Approval`        | Submitted; awaiting the **first-level** approver.              |
| `For Second Approval` | First approval done; awaiting the **second-level** approver.   |
| `Rejected`            | An approver rejected; requestor may modify & resubmit.         |
| `For Additional Info` | An approver needs more info; requestor may modify & resubmit.  |
| `In Process`          | Both approvals done; work underway.                            |
| `Done/Resolved`       | Work completed.                                                |
| `Closed`              | Requestor confirmed satisfaction. Terminal.                    |

## Transition table
| From                  | Event / API                    | To                    | Who (actor)              |
|-----------------------|--------------------------------|-----------------------|--------------------------|
| — (create)            | `POST /api/tickets`            | `For Approval`        | Requestor                |
| `For Approval`        | `POST …/approve`              | `For Second Approval` | **Level-1** approver (not requestor) |
| `For Second Approval` | `POST …/approve`              | `In Process`          | **Level-2** approver (not requestor) |
| `For Approval`        | `POST …/reject`               | `Rejected`            | Level-1 approver         |
| `For Second Approval` | `POST …/reject`               | `Rejected`            | Level-2 approver         |
| `For Approval`        | `POST …/request-info`         | `For Additional Info` | Level-1 approver         |
| `For Second Approval` | `POST …/request-info`         | `For Additional Info` | Level-2 approver         |
| `Rejected`            | `POST …/submit` (resubmit)    | `For Approval`        | Requestor (owner)        |
| `For Additional Info` | `POST …/submit` (resubmit)    | `For Approval`        | Requestor (owner)        |
| `In Process`          | `POST …/resolve`              | `Done/Resolved`       | System approver (any level) |
| `Done/Resolved`       | `POST …/close`                | `Closed`              | **Original requestor only** |

Any event not listed for the current status → `409 InvalidTransitionException`.

### Editing rules
- Editing ticket fields (`PUT /api/tickets/{id}`) is allowed **only** in
  `Rejected`, `For Additional Info`, and only by the owner. Otherwise `409`.
- All other states are read-only for field edits.

## Guards applied on each transition
1. **Ownership** — resubmit/close require `actor == requestor`. Else `403`.
2. **Approver identity** — approve/reject/request-info/resolve require actor ≠ requestor **and**
   the actor to be a **system approver** (`approver = 'Y'`, see [02](02-user-maintenance.md)).
   A non-approver gets `403` `{"message":"User <id> is not a system approver"}`.
3. **Approval level** — at an approval stage the actor's `approver_level` must match the
   stage: level 1 for `For Approval`, level 2 for `For Second Approval`. Otherwise `403`
   `{"message":"User <id> is not a level-<n> approver"}`. (`resolve` needs only `approver='Y'`.)
4. **Restriction** — for every action, `assertAllowed(actorId, ticket.categoryCode)`
   (see [02](02-user-maintenance.md)). E.g. user 1003 cannot act on a `DB` ticket → `403`.
5. **Close ownership** — only the original requestor may close (`Done/Resolved → Closed`). Else `403`.

Guard precedence: authentication (`401`) → transition legality (`409`) →
role/ownership/restriction (`403`).

## Side effects (every successful transition)
1. Persist new `status`, bump `updatedAt`, set `approverId` to the acting approver on approve.
2. Write an `AuditLog` row (old→new status) — same transaction. See [03](03-audit-trail.md).
   Audit actions: `TICKET_CREATED`, `TICKET_APPROVED_L1`, `TICKET_APPROVED_L2`,
   `TICKET_REJECTED`, `TICKET_INFO_REQUESTED`, `TICKET_RESUBMITTED`, `TICKET_RESOLVED`,
   `TICKET_CLOSED`.
3. After commit, send an email notification. See [04](04-email-notification.md).

## API surface (tickets)
| Method & path                    | Action |
|----------------------------------|--------|
| `POST /api/tickets`              | Create → `For Approval` (no draft) |
| `GET /api/tickets`               | List (filter by status/category/mine) |
| `GET /api/tickets/{id}`          | Detail |
| `PUT /api/tickets/{id}`          | Edit (Rejected / For Additional Info only) |
| `POST /api/tickets/{id}/submit`  | Resubmit: Rejected/Info → For Approval |
| `POST /api/tickets/{id}/approve` | For Approval → For Second Approval → In Process |
| `POST /api/tickets/{id}/reject`  | For Approval / For Second Approval → Rejected |
| `POST /api/tickets/{id}/request-info` | For Approval / For Second Approval → For Additional Info |
| `POST /api/tickets/{id}/resolve` | In Process → Done/Resolved |
| `POST /api/tickets/{id}/close`   | Done/Resolved → Closed |
| `GET /api/tickets/{id}/audit`    | Audit trail |

Body for decision endpoints may include `{ "comment": "…" }`, stored on the audit row.

## Diagram
```
  create
    │
    ▼
┌──────────────┐ approve(L1) ┌─────────────────────┐ approve(L2) ┌────────────┐ resolve ┌───────────────┐ close ┌────────┐
│ For Approval ├────────────▶│ For Second Approval ├────────────▶│ In Process ├────────▶│ Done/Resolved ├──────▶│ Closed │
└──────┬───────┘             └──────────┬──────────┘             └────────────┘         └───────────────┘       └────────┘
       │ reject / request-info          │ reject / request-info                                ▲ (requestor only)
       ▼                                ▼
   ┌─────────┐  or  ┌──────────────────────┐
   │Rejected │      │ For Additional Info  │
   └────┬────┘      └───────────┬──────────┘
        │ resubmit              │ resubmit
        └────────▶ For Approval ◀┘
```

## Acceptance criteria
- Happy path For Approval→For Second Approval→In Process→Done/Resolved→Closed succeeds with
  a level-1 then a level-2 approver, ending with the requestor closing.
- Reject and request-info (from either stage) loop back to For Approval on resubmit.
- Every illegal transition returns `409`; ownership/approver/level/restriction violations `403`.
- A level-2 approver cannot act on the first stage (and vice versa).

## Tests
- Unit (`TicketStateMachineTest`, `TicketServiceTest`): two-stage happy path; illegal
  transitions `409`; non-owner resubmit `403`; requestor-approves-own `403`; non-approver
  `403`; wrong-level approver `403`; restricted user (1003) on DB `403`; audit + event per step.
- Playwright: end-to-end two-stage lifecycle (Paw → Leiva L1 → Rudy L2 → resolve → close)
  with audit check, plus non-approver and wrong-level gating.
