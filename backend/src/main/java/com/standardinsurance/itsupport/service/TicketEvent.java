package com.standardinsurance.itsupport.service;

/** Lifecycle events a caller can request on a ticket. See docs/specs/08-ticket-lifecycle.md. */
public enum TicketEvent {
    SUBMIT,
    APPROVE,
    REJECT,
    REQUEST_INFO,
    RESOLVE,
    CLOSE
}
