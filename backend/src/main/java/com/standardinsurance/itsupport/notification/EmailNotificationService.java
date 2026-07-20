package com.standardinsurance.itsupport.notification;

import com.standardinsurance.itsupport.entity.User;
import com.standardinsurance.itsupport.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends one notification per ticket transition, after the transition commits.
 * Recipient addresses come from the user's {@code emailAddress} column; users without an
 * address are skipped. A send failure is logged and swallowed so it can never roll back
 * the transition. See docs/specs/04-email-notification.md.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final MailSender mailSender;
    private final UserRepository userRepository;

    public EmailNotificationService(MailSender mailSender, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    /** Fires only after the transaction that produced the event has committed. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransition(TicketTransitionEvent event) {
        send(event);
    }

    /** Extracted for direct unit testing without a transaction. */
    public void send(TicketTransitionEvent event) {
        try {
            List<String> recipients = recipientsFor(event);
            if (recipients.isEmpty()) {
                log.info("No email recipients with addresses for ticket {} ({}); skipping send",
                        event.ticketId(), event.transition());
                return;
            }
            mailSender.send(new EmailMessage(recipients, subjectFor(event), bodyFor(event)));
        } catch (Exception ex) {
            log.error("Failed to send notification for ticket {} ({}): {}",
                    event.ticketId(), event.transition(), ex.getMessage());
        }
    }

    List<String> recipientsFor(TicketTransitionEvent e) {
        List<String> to = new ArrayList<>();
        switch (e.transition()) {
            // Awaiting the first approval → notify level-1 approvers.
            case SUBMITTED, RESUBMITTED -> to.addAll(approverEmails(1));
            // First approval done → notify level-2 approvers (next in the chain) + requestor.
            case FIRST_APPROVED -> {
                to.addAll(approverEmails(2));
                emailOf(e.requestorId()).ifPresent(to::add);
            }
            // Second approval done (now In Process), plus reject/info/resolve → notify requestor.
            case SECOND_APPROVED, REJECTED, INFO_REQUESTED, RESOLVED ->
                    emailOf(e.requestorId()).ifPresent(to::add);
            case CLOSED -> {
                emailOf(e.requestorId()).ifPresent(to::add);
                emailOf(e.approverId()).ifPresent(to::add);
            }
            default -> { /* no recipients */ }
        }
        return to;
    }

    private List<String> approverEmails(int level) {
        return userRepository.findByApproverAndApproverLevel(User.APPROVER_FLAG, level).stream()
                .map(User::getEmailAddress)
                .filter(EmailNotificationService::hasText)
                .toList();
    }

    private Optional<String> emailOf(String userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId)
                .map(User::getEmailAddress)
                .filter(EmailNotificationService::hasText);
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

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
