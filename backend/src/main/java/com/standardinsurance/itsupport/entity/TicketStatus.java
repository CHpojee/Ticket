package com.standardinsurance.itsupport.entity;

/**
 * Ticket lifecycle states. See docs/specs/08-ticket-lifecycle.md.
 */
public enum TicketStatus {
    FOR_APPROVAL("For Approval"),
    FOR_SECOND_APPROVAL("For Second Approval"),
    REJECTED("Rejected"),
    FOR_ADDITIONAL_INFO("For Additional Info"),
    IN_PROCESS("In Process"),
    DONE_RESOLVED("Done/Resolved"),
    CLOSED("Closed");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Resolve a status from its human label or enum name; null/blank returns null. */
    public static TicketStatus fromLabelOrName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (TicketStatus s : values()) {
            if (s.label.equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        return null;
    }
}
