# 02 — User Maintenance & Restrictions

## Goal
Admin UI + API to manage the `users` table and assign/modify `user_restrictions`
(category-specific bans). Enforce restrictions across the whole app.

## Access
All endpoints under `/api/admin/**` require `ROLE_ADMIN` (userId 1001). Non-admins get `403`.

## Data model
```
users(user_id PK,
      password,
      name,
      approver      CHAR(1)  -- 'Y' = system approver, otherwise NULL
      approver_level INT      -- approval stage: 1 = first, 2 = second (NULL if not an approver)
      email_address VARCHAR  -- nullable; recipient for notifications
)
user_restrictions(id PK,
                  user_id FK→users(user_id),
                  ticket_category_code FK→ticket_categories(code),
                  UNIQUE(user_id, ticket_category_code))
```
Seed restriction: **1003 → DB**. Seed approvers: **1002 (Leiva) `approver='Y'`, level 1**;
**1003 (Rudy) `approver='Y'`, level 2**.

### Approver business rule (two-stage)
A user is a **system approver if and only if `approver` equals exactly `'Y'`**. Approval is
**sequential** by `approver_level`: a **level-1** approver acts on the first stage
(`For Approval`), then a **level-2** approver acts on the second stage
(`For Second Approval`). Only system approvers may approve / reject / request-info / resolve,
and only at the stage matching their level (see [08-ticket-lifecycle.md](08-ticket-lifecycle.md)).
Both `approver` and `approverLevel` are exposed on login and carried in the JWT so the UI can
gate the correct stage. Admins set them via User Maintenance.

## API — Users

| Method & path                 | Purpose                    | Notes |
|-------------------------------|----------------------------|-------|
| `GET /api/admin/users`        | List all users             | password never returned |
| `GET /api/admin/users/{id}`   | Get one user + restrictions| |
| `POST /api/admin/users`       | Create user                | `{userId,name,password,approver,approverLevel,emailAddress}`; password hashed |
| `PUT /api/admin/users/{id}`   | Update fields              | any of name/password/approver/approverLevel/emailAddress; password re-hashed if present |
| `DELETE /api/admin/users/{id}`| Delete user                | blocked if user owns tickets → `409` |

## API — Restrictions

| Method & path                                            | Purpose |
|----------------------------------------------------------|---------|
| `GET /api/admin/users/{id}/restrictions`                 | List category codes the user is barred from |
| `POST /api/admin/users/{id}/restrictions`                | Add `{ticketCategoryCode}` |
| `DELETE /api/admin/users/{id}/restrictions/{categoryCode}`| Remove a restriction |

Validation:
- `409` on duplicate restriction (unique constraint).
- `404` if user or category code unknown.

## Restriction enforcement (the critical constraint)
Central guard in `RestrictionService`:
```java
void assertAllowed(String userId, String categoryCode) {
    if (restrictionRepo.existsByUserIdAndTicketCategoryCode(userId, categoryCode))
        throw new RestrictionViolationException(userId, categoryCode); // → HTTP 403
}
```
Called by `TicketService` before **any** category-specific action:
- Creating/submitting a ticket in that category.
- Approving / rejecting / requesting info / resolving a ticket in that category.

Example: user **1003** attempting to create or act on a **DB** ticket → `403`
`{"message":"User 1003 is restricted from category DB"}`.

> A restriction added to a user does **not** retroactively delete existing tickets; it
> only blocks future actions.

## Validation rules
| Rule | Result |
|------|--------|
| Non-admin calls `/api/admin/**` | `403` |
| Create user with existing userId | `409` |
| Restriction on unknown category | `404` |
| Duplicate restriction | `409` |
| Delete user with tickets | `409` |

## Audit
Every user create/update/delete and restriction add/remove writes an `AuditLog`
(`action` ∈ `USER_CREATED`, `USER_UPDATED`, `USER_DELETED`, `RESTRICTION_ADDED`,
`RESTRICTION_REMOVED`) with old/new values. Passwords are never logged.

## Acceptance criteria
- Admin can CRUD users and manage restrictions from the UI.
- Seeded restriction (1003→DB) is present at startup.
- Any restricted category action is blocked with `403` regardless of entry point.

## Tests
- Unit: `assertAllowed` allows/blocks; duplicate restriction `409`; delete-with-tickets `409`.
- Playwright: admin adds a restriction (1004→DB) then that user is blocked from a DB ticket.
