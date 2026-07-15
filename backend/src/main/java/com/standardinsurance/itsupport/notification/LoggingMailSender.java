package com.standardinsurance.itsupport.notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Dev/test mail sender: logs the message and keeps it in an in-memory outbox that the
 * dev outbox endpoint and Playwright tests can inspect. See docs/specs/04-email-notification.md.
 */
@Component
@Profile("!prod")
public class LoggingMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

    private final List<EmailMessage> outbox = new CopyOnWriteArrayList<>();

    @Override
    public void send(EmailMessage message) {
        outbox.add(message);
        log.info("[MOCK EMAIL] to={} subject={}", message.to(), message.subject());
    }

    public List<EmailMessage> outbox() {
        return List.copyOf(outbox);
    }

    public void clear() {
        outbox.clear();
    }
}
