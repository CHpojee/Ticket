# 06 — Generate Report

## Goal
Export ticket records to **CSV** or **Excel (.xlsx)**, filtered by date range, category,
and status.

## API
### `GET /api/reports/tickets`
Query params (all optional; combine with AND):
| Param      | Example              | Meaning |
|------------|----------------------|---------|
| `from`     | `2026-07-01`         | createdAt ≥ from (inclusive) |
| `to`       | `2026-07-15`         | createdAt ≤ to (inclusive, end of day) |
| `category` | `DB`                 | category code (repeatable) |
| `status`   | `Closed`             | lifecycle status (repeatable) |
| `format`   | `csv` \| `xlsx`      | default `csv` |

Response: file download.
- CSV → `Content-Type: text/csv`, `Content-Disposition: attachment; filename="tickets_<ts>.csv"`.
- XLSX → `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.

### Columns
`Ticket ID, Title, Category Code, Category Description, Status, Requestor ID,
Requestor Name, Approver Name, Created At, Updated At`.

## Design
- `ReportService.query(filter)` builds a JPA `Specification`/dynamic query from the filter.
- CSV via a small writer (RFC-4180 quoting); XLSX via **Apache POI** streaming workbook.
- Dates validated: `from ≤ to`, ISO-8601; invalid → `400`.
- Large result guard: cap rows (e.g. 50k) with a logged warning if exceeded.

## Frontend
- Route `/reports`. Filter form (date pickers, category multiselect, status multiselect,
  format toggle) → triggers download. Shows a result count preview before export.

## Validation rules
| Rule | Result |
|------|--------|
| `from > to` | `400` |
| Unknown category/status value | `400` |
| Bad date format | `400` |
| No matching rows | `200` with header-only file |

## Acceptance criteria
- Filtering by any combination returns only matching rows.
- CSV opens cleanly; special characters are quoted/escaped.
- XLSX opens in Excel with a header row and typed date cells.

## Tests
- Unit: `ReportService` filter combinations; date validation; CSV escaping.
- Playwright: apply filters, download, assert filename + non-empty content.
