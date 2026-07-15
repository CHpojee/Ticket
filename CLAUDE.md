# CLAUDE.md — Internal IT Support (Ticketing + Approval Cycle)

Technical guidelines, build commands, and architecture for the **Internal IT Support**
internal ticketing system. Read this before making changes.

---

## 1. Product Summary

An internal IT support ticketing system with a formal **approval cycle**. A requestor
drafts a ticket, submits it for approval, an approver acts on it (approve / reject /
request additional info), work is performed, and the original requestor closes it to
confirm satisfaction. Every state transition is audited and triggers an email
notification. Category-based restrictions prevent specific users from acting on
specific ticket categories.

---

## 2. Tech Stack

| Layer      | Technology                                                        |
|------------|-------------------------------------------------------------------|
| Frontend   | Next.js (App Router), React, TypeScript, Tailwind CSS             |
| E2E Tests  | Playwright (headless)                                              |
| Backend    | Spring Boot 3 (Java 17), Spring Web, Spring Data JPA, Spring Security |
| DB         | H2 (dev/test, in-memory) — PostgreSQL-compatible schema for prod  |
| Unit Tests | JUnit 5 + Mockito, JaCoCo coverage (target ≥ 80%)                 |
| Auth       | JWT bearer tokens (stateless)                                     |
| Charts     | Recharts (dashboard)                                              |
| Reports    | Apache POI (Excel) + OpenCSV / manual CSV writer                  |
| Email      | Spring Mail; mock `LoggingMailSender` in dev, SMTP in prod        |

---

## 3. Repository Layout

```text
/
├── frontend/               # Next.js + Tailwind + Playwright
│   ├── src/app/            # App Router routes (login, dashboard, tickets, admin, specs)
│   ├── src/components/     # Reusable UI components
│   ├── src/lib/            # API client, auth helpers, types
│   └── tests/e2e/          # Playwright specs
├── backend/                # Spring Boot
│   └── src/main/java/com/standardinsurance/itsupport/
│       ├── controller/     # Thin REST controllers (@RestController)
│       ├── service/        # Business logic + state machine + validation
│       ├── repository/     # Spring Data JPA repositories
│       ├── entity/         # JPA entities
│       ├── dto/            # Request/response DTOs
│       ├── config/         # Security, CORS, seeder
│       ├── exception/      # @ControllerAdvice global handler + custom exceptions
│       └── notification/   # Email notification service
│   └── src/test/java/...   # JUnit 5 tests
├── docs/
│   └── specs/              # One markdown spec per feature (served by Specs Viewer)
└── CLAUDE.md
```

---

## 4. Build & Run Commands

### Backend
```bash
cd backend
./mvnw spring-boot:run            # start API on http://localhost:8080
./mvnw test                       # run JUnit 5 tests
./mvnw verify                     # tests + JaCoCo coverage report (target/site/jacoco)
./mvnw clean package              # build jar
```

### Frontend
```bash
cd frontend
npm install
npm run dev                       # start Next.js on http://localhost:3000
npm run build && npm run start    # production build
npm run lint                      # ESLint (Airbnb config)
npm run test:e2e                  # Playwright headless
npm run test:e2e -- --ui          # Playwright interactive
```

---

## 5. Coding Standards

### General
- **Thin controllers, fat services.** Controllers only parse/validate input, delegate to
  services, and shape responses. All business logic — especially the ticket state machine
  and restriction checks — lives in the service layer.
- **Global exception handling** via `@ControllerAdvice` (`GlobalExceptionHandler`).
  Never leak stack traces; return structured `ErrorResponse { timestamp, status, error,
  message, path }`.
- **DTOs at the boundary.** Never expose JPA entities directly in controller responses.
- **Immutability first.** Prefer `final`, records for DTOs, and constructor injection
  (no field `@Autowired`).

### Frontend — Airbnb Style Guide
- ESLint uses **`eslint-config-airbnb` + `eslint-config-airbnb-typescript`** with the
  Next.js plugin. `npm run lint` must pass with zero errors before commit.
- 2-space indentation, single quotes, trailing commas (multiline), semicolons required,
  max line length 100.
- Prefer function components + hooks; no default exports for shared components
  (Airbnb `import/prefer-default-export` handled per-file); named exports for utilities.
- No `any` unless justified with an inline comment.

### Backend
- Google Java Format / standard Spring conventions, 4-space indentation.
- Package-by-feature within the layered structure above.

---

## 6. Architecture Notes

### Layered flow
```
HTTP → Controller → Service (business rules, state machine, restriction guard)
     → Repository → DB
Service → AuditService (records every mutation) → EmailNotificationService
```

### Authentication & Authorization
- `POST /api/auth/login` validates seeded credentials, returns a JWT.
- Roles: `ADMIN` (userId 1001) manages users; all users can create/act on tickets subject
  to restrictions. Role is derived from user data (Admin flag) — see spec 01.
- **Approver rule:** only users with `approver = 'Y'` may approve / reject / request-info /
  resolve a ticket. The flag is carried in the JWT so the UI can gate approver actions.
- Authorization is enforced in services, not just filters, so tests exercise the rules.

### Restriction enforcement (critical constraint)
Before any category-specific action, the service calls
`RestrictionService.assertAllowed(userId, ticketCategoryCode)`. If a matching row exists
in `user_restrictions`, a `RestrictionViolationException` (HTTP 403) is thrown. Seed:
user **1003 (Rudy)** is restricted from category **DB**.

### State machine
Transitions are validated centrally in `TicketStateMachine`. Illegal transitions throw
`InvalidTransitionException` (HTTP 409). See `docs/specs/08-ticket-lifecycle.md`.

---

## 7. Database — Schema & Seed Data (authoritative)

Foreign keys are enforced. Tables:

- `users(user_id PK, password, name, approver CHAR(1) 'Y'|NULL, email_address NULL)`
  — a user is a **system approver iff `approver = 'Y'`**.
- `user_restrictions(id PK, user_id FK→users, ticket_category_code FK→ticket_categories,
  UNIQUE(user_id, ticket_category_code))`
- `ticket_categories(code PK, description)`
- `tickets(id PK, title, description, category_code FK→ticket_categories, status,
  requestor_id FK→users, approver_id FK→users NULL, created_at, updated_at)`
- `audit_logs(id PK, ticket_id FK→tickets NULL, actor_id FK→users, action, field,
  old_value, new_value, timestamp)`

### Seed data
**users**
| userId | password | name  | approver | emailAddress                      |
|--------|----------|-------|----------|-----------------------------------|
| 1001   | Admin    | Admin | NULL     | NULL                              |
| 1002   | Leiva    | Leiva | Y        | rreyes@stand-insurance.com        |
| 1003   | Rudy     | Rudy  | NULL     | richeercoronareyes@gmail.com      |
| 1004   | Rich     | Rich  | NULL     | NULL                              |
| 1005   | Paw      | Paw   | NULL     | clualhati@standard-insurance.com  |

> Seed passwords are stored **BCrypt-hashed** by the seeder; the plaintext above is the
> login credential. `approver = 'Y'` marks a system approver (only **1002 Leiva**).
> Email notifications go to a user's `emailAddress`; users without one are skipped.

**ticket_categories**
| Code | Description                              |
|------|------------------------------------------|
| SR   | Service Request                          |
| DB   | Database Fix (DB Fix)                    |
| MR   | Mass Request / Bulk Action               |
| BW   | BCP Whitelisting (Business Continuity Plan) |
| IR   | Incident Report (IR)                     |

**user_restrictions**
| userId | ticketCategoryCode |
|--------|--------------------|
| 1003   | DB                 |

---

## 8. Git Workflow

- Conventional Commits: `feat(scope): …`, `fix(scope): …`, `test(scope): …`,
  `docs(scope): …`, `chore(scope): …`.
- One commit per milestone; keep commits focused and buildable.
- Co-author trailer is added automatically by the tooling.

---

## 9. Definition of Done (per feature)

1. Spec exists in `docs/specs/` and matches implementation.
2. Backend: service logic + JUnit 5 tests; global exception handling wired.
3. Frontend: UI implemented, `npm run lint` clean.
4. Audit + email notification fire on every state change.
5. Playwright covers the critical path.
6. JaCoCo backend coverage ≥ 80%.
