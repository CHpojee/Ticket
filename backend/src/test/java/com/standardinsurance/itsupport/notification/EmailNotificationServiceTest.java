package com.standardinsurance.itsupport.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import com.standardinsurance.itsupport.entity.User;
import com.standardinsurance.itsupport.repository.UserRepository;
import com.standardinsurance.itsupport.service.TicketTransition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock private UserRepository userRepository;

    private final List<EmailMessage> sent = new ArrayList<>();
    private final MailSender capturing = sent::add;
    private EmailNotificationService service;

    private final User requestor = new User("1005", "x", "Paw", null, "paw@x.com");
    private final User noEmailRequestor = new User("1004", "x", "Rich", null, null);
    private final User approver = new User("1002", "x", "Leiva", "Y", "leiva@x.com");

    @BeforeEach
    void setUp() {
        service = new EmailNotificationService(capturing, userRepository);
    }

    private TicketTransitionEvent event(TicketTransition transition, String requestorId,
                                        String approverId) {
        return new TicketTransitionEvent(7L, "Printer", "SR", "old", "new",
                transition, requestorId, approverId, "Leiva");
    }

    @Test
    void submissionNotifiesSystemApprovers() {
        when(userRepository.findByApprover("Y")).thenReturn(List.of(approver));
        assertThat(service.recipientsFor(event(TicketTransition.SUBMITTED, "1005", null)))
                .containsExactly("leiva@x.com");
    }

    @Test
    void approvalNotifiesRequestorEmail() {
        when(userRepository.findById("1005")).thenReturn(Optional.of(requestor));
        assertThat(service.recipientsFor(event(TicketTransition.APPROVED, "1005", "1002")))
                .containsExactly("paw@x.com");
    }

    @Test
    void closeNotifiesRequestorAndApprover() {
        when(userRepository.findById("1005")).thenReturn(Optional.of(requestor));
        when(userRepository.findById("1002")).thenReturn(Optional.of(approver));
        assertThat(service.recipientsFor(event(TicketTransition.CLOSED, "1005", "1002")))
                .containsExactlyInAnyOrder("paw@x.com", "leiva@x.com");
    }

    @Test
    void skipsSendWhenNoRecipientEmail() {
        when(userRepository.findById("1004")).thenReturn(Optional.of(noEmailRequestor));
        service.send(event(TicketTransition.APPROVED, "1004", "1002"));
        assertThat(sent).isEmpty();
    }

    @Test
    void sendBuildsSubjectAndBody() {
        when(userRepository.findById("1005")).thenReturn(Optional.of(requestor));
        service.send(event(TicketTransition.APPROVED, "1005", "1002"));
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).subject()).contains("Ticket #7");
        assertThat(sent.get(0).body()).contains("Printer");
    }

    @Test
    void mailFailureIsSwallowed() {
        when(userRepository.findById("1005")).thenReturn(Optional.of(requestor));
        MailSender throwing = m -> {
            throw new RuntimeException("SMTP down");
        };
        EmailNotificationService failing = new EmailNotificationService(throwing, userRepository);
        assertThatCode(() -> failing.send(event(TicketTransition.RESOLVED, "1005", "1002")))
                .doesNotThrowAnyException();
    }
}
