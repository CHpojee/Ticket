package com.standardinsurance.itsupport.notification;

import com.standardinsurance.itsupport.service.TicketTransition;

/**
 * Published by TicketService after a transition mutation. Carries a primitive snapshot
 * (not entities) so the AFTER_COMMIT listener never touches a detached/lazy graph.
 */
public record TicketTransitionEvent(
        Long ticketId,
        String title,
        String categoryCode,
        String oldStatusLabel,
        String newStatusLabel,
        TicketTransition transition,
        String requestorId,
        String approverId,
        String actorName) {
}
