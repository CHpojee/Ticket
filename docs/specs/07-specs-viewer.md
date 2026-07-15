# 07 — Specs Viewer

## Goal
A Markdown-rendered page in the frontend that lets developers/admins browse the files in
`docs/specs/` from the running app.

## Source of truth
The files in this very directory (`docs/specs/*.md`). The viewer lists them and renders
the selected file as HTML.

## Delivery approach
Two supported options (pick per environment):

1. **Backend-served (default)** — a read-only endpoint exposes the spec files:
   | Method & path              | Purpose |
   |----------------------------|---------|
   | `GET /api/specs`           | List `{ name, title }` for each `.md` file |
   | `GET /api/specs/{name}`    | Raw markdown for one file |
   - The backend reads from a configured `app.specs.dir` (defaults to `../docs/specs`).
   - **Path traversal guard**: `name` must match `^[0-9A-Za-z._-]+\.md$` and resolve inside
     the specs dir, else `400`. No absolute paths, no `..`.

2. **Static import (fallback)** — Next.js reads the markdown at build time from a copied
   `docs/specs` folder (useful if the frontend is deployed without the backend).

## Frontend
- Route `/specs`. Left: file list (sorted by filename). Right: rendered markdown.
- Render with `react-markdown` + `remark-gfm` (tables, checklists); syntax-highlight code
  blocks. Sanitize HTML output.
- Internal `[..](xx.md)` links between specs navigate within the viewer.

## Access
Available to authenticated users; link shown in the app nav. (Restrict to admin only if
specs are considered sensitive — configurable.)

## Validation / safety
| Rule | Result |
|------|--------|
| `name` fails the filename pattern | `400` |
| Resolved path escapes specs dir | `400` |
| File not found | `404` |

## Acceptance criteria
- All `.md` files in `docs/specs/` appear in the list.
- Selecting a file renders formatted markdown (headings, tables, code).
- Path traversal attempts are rejected.

## Tests
- Unit: specs listing; traversal attempt (`../../CLAUDE.md`) → `400`; unknown file → `404`.
- Playwright: open `/specs`, list shows all files, clicking one renders its heading.
