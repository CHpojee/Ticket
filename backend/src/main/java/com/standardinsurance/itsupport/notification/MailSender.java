package com.standardinsurance.itsupport.notification;

/** Abstraction over email delivery. Dev uses a logging/outbox impl; prod uses SMTP. */
public interface MailSender {
    void send(EmailMessage message);
}
