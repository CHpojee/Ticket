package com.standardinsurance.itsupport.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.standardinsurance.itsupport.service.TicketTransition;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmailNotificationServiceTest {

    private final List<EmailMessage> sent = new ArrayList<>();
    private final MailSender capturing = sent::add;
    private final EmailNotificationService service =
            new EmailNotificationService(capturing, "standard-insurance.com",
                    "it-support@standard-insurance.com");

    private TicketTransitionEvent event(TicketTransition transition, String approverId) {
        return new TicketTransitionEvent(7L, "Printer", "SR", "old", "new",
                transition, "1002", approverId, "Rich");
    }

    @Test
    void approvalNotifiesRequestor() {
        assertThat(service.recipientsFor(event(TicketTransition.APPROVED, "1004")))
                .containsExactly("1002@standard-insurance.com");
    }

    @Test
    void submissionWithoutApproverGoesToSupportQueue() {
        assertThat(service.recipientsFor(event(TicketTransition.SUBMITTED, null)))
                .containsExactly("it-support@standard-insurance.com");
    }

    @Test
    void closeNotifiesRequestorAndApprover() {
        assertThat(service.recipientsFor(event(TicketTransition.CLOSED, "1004")))
                .containsExactlyInAnyOrder(
                        "1002@standard-insurance.com", "1004@standard-insurance.com");
    }

    @Test
    void sendBuildsSubjectAndBody() {
        service.send(event(TicketTransition.APPROVED, "1004"));
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).subject()).contains("Ticket #7");
        assertThat(sent.get(0).body()).contains("Printer");
    }

    @Test
    void mailFailureIsSwallowed() {
        MailSender throwing = m -> {
            throw new RuntimeException("SMTP down");
        };
        EmailNotificationService failing = new EmailNotificationService(
                throwing, "standard-insurance.com", "it-support@standard-insurance.com");
        assertThatCode(() -> failing.send(event(TicketTransition.RESOLVED, "1004")))
                .doesNotThrowAnyException();
    }
}
