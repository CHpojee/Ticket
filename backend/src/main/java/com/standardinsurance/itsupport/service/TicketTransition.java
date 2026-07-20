package com.standardinsurance.itsupport.service;

/**
 * A lifecycle transition event, used for audit action naming and email routing.
 * See docs/specs/08-ticket-lifecycle.md and docs/specs/04-email-notification.md.
 */
public enum TicketTransition {
    SUBMITTED("TICKET_SUBMITTED"),
    FIRST_APPROVED("TICKET_APPROVED_L1"),
    SECOND_APPROVED("TICKET_APPROVED_L2"),
    REJECTED("TICKET_REJECTED"),
    INFO_REQUESTED("TICKET_INFO_REQUESTED"),
    RESUBMITTED("TICKET_RESUBMITTED"),
    RESOLVED("TICKET_RESOLVED"),
    CLOSED("TICKET_CLOSED");

    private final String auditAction;

    TicketTransition(String auditAction) {
        this.auditAction = auditAction;
    }

    public String auditAction() {
        return auditAction;
    }
}
