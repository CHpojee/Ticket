# 04 — Email Notification Service

## Goal
Send an email alert to the relevant stakeholders on **every** ticket state change.

## Trigger points
One notification per lifecycle transition:
`SUBMITTED, APPROVED, REJECTED, INFO_REQUESTED, RESUBMITTED, RESOLVED, CLOSED`.

## Recipients
| Transition        | To                          | Rationale |
|-------------------|-----------------------------|-----------|
| Submitted         | All system approvers (`approver='Y'`) | action needed |
| Approved          | Requestor                   | inform + work begins |
| Rejected          | Requestor                   | may resubmit |
| Info Requested    | Requestor                   | must supply info |
| Resubmitted       | All system approvers        | re-review needed |
| Resolved          | Requestor                   | please confirm/close |
| Closed            | Requestor + Approver        | cycle complete |

Recipient addresses come from the user's **`email_address`** column. A user without an
address is **skipped**; if a transition has no addressable recipient, no mail is sent (a
line is logged). Failures to send are logged and **must not** roll back or block the ticket
transition.

## Design
```java
interface MailSender { void send(EmailMessage msg); }

@Profile("!prod") class LoggingMailSender implements MailSender { /* logs to console + in-memory outbox */ }
@Profile("prod")  class SmtpMailSender    implements MailSender { /* JavaMailSender */ }
```
- `EmailNotificationService.onTransition(ticket, transition, actor)` builds a templated
  `EmailMessage { to, subject, body }` and calls `MailSender`.
- Invoked by `TicketService` **after** the transition commits (post-commit hook /
  `@TransactionalEventListener(AFTER_COMMIT)`), so emails never fire on a rolled-back change.
- Dev `LoggingMailSender` keeps an in-memory **outbox** exposed at
  `GET /api/dev/outbox` (dev profile only) so Playwright can assert emails were "sent".

## Templates
Subject: `[IT Support] Ticket #{id} — {newStatus}`
Body includes ticket id, title, category, old→new status, actor name, timestamp, and a
link to the ticket.

## Config
```
app.mail.from=it-support@standard-insurance.com
spring.mail.host / port / username / password   # prod only
```

## Acceptance criteria
- Each of the 7 transitions produces exactly one queued/sent email to the correct recipient.
- A mail failure logs an error but the ticket transition still succeeds.
- Dev outbox reflects sent messages for testing.

## Tests
- Unit: `EmailNotificationService` builds correct recipient/subject per transition; mail
  failure is swallowed (transition unaffected) via a throwing mock `MailSender`.
- Playwright: after approval, dev outbox contains an email to the requestor.
