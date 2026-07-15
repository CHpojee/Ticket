package com.standardinsurance.itsupport.notification;

import java.util.List;

public record EmailMessage(List<String> to, String subject, String body) {
}
