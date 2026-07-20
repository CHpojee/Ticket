# 01 — User Login

## Goal
Authenticate users against seeded credentials and issue a JWT used for all subsequent
API calls. Establish role-based authorization that also respects `user_restrictions`.

## Roles
- **ADMIN** — `userId = 1001` (name `Admin`). Full access, including User Maintenance.
- **USER** — everyone else. Can create tickets and act within the lifecycle, subject to
  category restrictions.

Role is derived at login: userId `1001` → `ROLE_ADMIN`, otherwise `ROLE_USER`.

## API

### `POST /api/auth/login`
Request:
```json
{ "userId": "1003", "password": "Rudy" }
```
Response `200`:
```json
{
  "token": "<jwt>",
  "user": { "userId": "1002", "name": "Leiva", "role": "ROLE_USER",
            "approver": true, "approverLevel": 1 }
}
```
`approver` is `true` iff the user's `approver` column is `'Y'`; `approverLevel` (1 or 2, else
`null`) is the approval stage they act at. Both are JWT claims so the frontend can gate the
correct approval stage (approve / reject / request-info at the matching level).
Errors:
- `400` — missing `userId` or `password`.
- `401` — unknown user or bad password (`{"message":"Invalid credentials"}`).

### `GET /api/auth/me`
Returns the current user from the JWT. `401` if token missing/expired.

## Security design
- Passwords stored **BCrypt-hashed**; seeder hashes the plaintext seed values.
- JWT: HS256, subject = `userId`, claim `role`, `name`; TTL 8h. Secret from
  `app.jwt.secret` (env in prod).
- Stateless `SecurityFilterChain`; `JwtAuthenticationFilter` populates the
  `SecurityContext`. CORS allows the frontend origin.
- `/api/auth/login` is public; everything else requires authentication.
  `/api/admin/**` requires `ROLE_ADMIN`.

## Authorization vs. restriction
Authentication proves identity; **restriction** governs category actions and is checked
in services (see [02](02-user-maintenance.md)). A logged-in user restricted from `DB`
still authenticates fine but is `403`-blocked when acting on a `DB` ticket.

## Validation rules
| Rule | Result |
|------|--------|
| Empty userId/password | `400` |
| userId not in `users` | `401` |
| Password mismatch | `401` |
| Valid | `200` + token |

## Acceptance criteria
- All five seeded users log in with their exact credentials.
- Admin (1001) receives `ROLE_ADMIN`; others `ROLE_USER`.
- Protected endpoints reject missing/expired tokens with `401`.
- Wrong password returns `401`, never reveals whether the user exists beyond "Invalid credentials".

## Tests
- Unit: `AuthService` success, unknown user, wrong password, role mapping.
- Playwright: login as 1001 (admin nav visible), login as 1003 (no admin nav), bad login shows error.
