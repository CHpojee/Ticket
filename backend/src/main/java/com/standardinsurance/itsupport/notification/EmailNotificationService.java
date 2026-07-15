package com.standardinsurance.itsupport.notification;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends one notification per ticket transition, after the transition commits.
 * A send failure is logged and swallowed so it can never roll back the transition.
 * See docs/specs/04-email-notification.md.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final MailSender mailSender;
    private final String domain;
    private final String supportQueue;

    public EmailNotificationService(
            MailSender mailSender,
            @Value("${app.mail.domain}") String domain,
            @Value("${app.mail.from}") String supportQueue) {
        this.mailSender = mailSender;
        this.domain = domain;
        this.supportQueue = supportQueue;
    }

    /** Fires only after the transaction that produced the event has committed. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransition(TicketTransitionEvent event) {
        send(event);
    }

    /** Extracted for direct unit testing without a transaction. */
    public void send(TicketTransitionEvent event) {
        try {
            EmailMessage message = new EmailMessage(
                    recipientsFor(event),
                    subjectFor(event),
                    bodyFor(event));
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send notification for ticket {} ({}): {}",
                    event.ticketId(), event.transition(), ex.getMessage());
        }
    }

    List<String> recipientsFor(TicketTransitionEvent e) {
        List<String> to = new ArrayList<>();
        switch (e.transition()) {
            case SUBMITTED, RESUBMITTED -> {
                if (e.approverId() != null) {
                    to.add(email(e.approverId()));
                } else {
                    to.add(supportQueue);
                }
            }
            case APPROVED, REJECTED, INFO_REQUESTED, RESOLVED -> to.add(email(e.requestorId()));
            case CLOSED -> {
                to.add(email(e.requestorId()));
                if (e.approverId() != null) {
                    to.add(email(e.approverId()));
                }
            }
            default -> to.add(supportQueue);
        }
        return to;
    }

    private String subjectFor(TicketTransitionEvent e) {
        return "[IT Support] Ticket #" + e.ticketId() + " — " + e.newStatusLabel();
    }

    private String bodyFor(TicketTransitionEvent e) {
        return """
                Ticket #%d "%s" (category %s)
                Status: %s -> %s
                Action by: %s
                """.formatted(
                e.ticketId(), e.title(), e.categoryCode(),
                e.oldStatusLabel(), e.newStatusLabel(), e.actorName());
    }

    private String email(String userId) {
        return userId + "@" + domain;
    }
}
