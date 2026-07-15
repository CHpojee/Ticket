package com.standardinsurance.itsupport.notification;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Production mail sender backed by Spring's JavaMailSender. */
@Component
@Profile("prod")
public class SmtpMailSender implements MailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpMailSender(JavaMailSender mailSender,
                          @org.springframework.beans.factory.annotation.Value("${app.mail.from}")
                          String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(message.to().toArray(new String[0]));
        mail.setSubject(message.subject());
        mail.setText(message.body());
        mailSender.send(mail);
    }
}
