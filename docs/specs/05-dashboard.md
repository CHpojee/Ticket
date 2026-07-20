# 05 — Dashboard

## Goal
At-a-glance metrics and charts: metric cards (total open, pending approvals, completed)
plus charts breaking tickets down by **Category** and by **Status**.

## Metric definitions
| Card              | Definition |
|-------------------|-----------|
| Total Open        | status ∈ {For Approval, For Second Approval, For Additional Info, Rejected, In Process} |
| Pending Approvals | status = For Approval |
| Completed         | status ∈ {Done/Resolved, Closed} |
| Total Tickets     | all tickets (context) |

## Charts
- **By Category** — bar chart, count per `SR/DB/MR/BW/IR` (all 5 categories always shown, 0 if none).
- **By Status** — pie/donut, count per status across the 7 lifecycle states.

## API
### `GET /api/dashboard/summary`
```json
{
  "totalTickets": 12,
  "totalOpen": 7,
  "pendingApprovals": 3,
  "completed": 5,
  "byCategory": [ {"code":"SR","description":"Service Request","count":4}, ... ],
  "byStatus":   [ {"status":"For Approval","count":3}, ... ]
}
```
- Computed via repository aggregate queries (`GROUP BY`), not in-memory loops.
- Available to any authenticated user. (Optional scope: a non-admin sees only tickets they
  requested or can approve; admin sees all. Default MVP: all users see global counts.)

## Frontend
- Route `/dashboard`. Metric cards using Tailwind; charts via Recharts.
- Loading + empty states; numbers link to a filtered ticket list where sensible.

## Acceptance criteria
- Cards reflect live counts and match the raw ticket data.
- Both charts render with all categories/statuses represented.
- Counts derived from `GROUP BY` queries.

## Tests
- Unit: `DashboardService` returns correct aggregates for a known seeded set.
- Playwright: create/submit/approve tickets, then dashboard cards/charts reflect the change.
