package com.standardinsurance.itsupport.controller;

import com.standardinsurance.itsupport.notification.EmailMessage;
import com.standardinsurance.itsupport.notification.LoggingMailSender;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only view of the mock email outbox, so Playwright can assert notifications were
 * "sent". Not registered under the prod profile. See docs/specs/04-email-notification.md.
 */
@RestController
@RequestMapping("/api/dev/outbox")
@Profile("!prod")
public class DevOutboxController {

    private final LoggingMailSender mailSender;

    public DevOutboxController(LoggingMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @GetMapping
    public List<EmailMessage> outbox() {
        return mailSender.outbox();
    }

    @DeleteMapping
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void clear() {
        mailSender.clear();
    }
}
