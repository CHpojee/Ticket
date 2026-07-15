package com.standardinsurance.itsupport.service;

import static com.standardinsurance.itsupport.entity.TicketStatus.CLOSED;
import static com.standardinsurance.itsupport.entity.TicketStatus.DONE_RESOLVED;
import static com.standardinsurance.itsupport.entity.TicketStatus.FOR_ADDITIONAL_INFO;
import static com.standardinsurance.itsupport.entity.TicketStatus.FOR_APPROVAL;
import static com.standardinsurance.itsupport.entity.TicketStatus.IN_PROCESS;
import static com.standardinsurance.itsupport.entity.TicketStatus.NEW;
import static com.standardinsurance.itsupport.entity.TicketStatus.REJECTED;

import com.standardinsurance.itsupport.entity.TicketStatus;
import com.standardinsurance.itsupport.exception.InvalidTransitionException;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Central, side-effect-free validation of ticket state transitions. Given the current
 * status and a requested event, returns the resulting status or throws
 * {@link InvalidTransitionException} (HTTP 409). See docs/specs/08-ticket-lifecycle.md.
 */
@Component
public class TicketStateMachine {

    private record Key(TicketStatus from, TicketEvent event) {
    }

    private static final Map<Key, TicketStatus> TRANSITIONS = Map.of(
            new Key(NEW, TicketEvent.SUBMIT), FOR_APPROVAL,
            new Key(REJECTED, TicketEvent.SUBMIT), FOR_APPROVAL,
            new Key(FOR_ADDITIONAL_INFO, TicketEvent.SUBMIT), FOR_APPROVAL,
            new Key(FOR_APPROVAL, TicketEvent.APPROVE), IN_PROCESS,
            new Key(FOR_APPROVAL, TicketEvent.REJECT), REJECTED,
            new Key(FOR_APPROVAL, TicketEvent.REQUEST_INFO), FOR_ADDITIONAL_INFO,
            new Key(IN_PROCESS, TicketEvent.RESOLVE), DONE_RESOLVED,
            new Key(DONE_RESOLVED, TicketEvent.CLOSE), CLOSED);

    /** @return the resulting status, or throws if the transition is illegal. */
    public TicketStatus next(TicketStatus current, TicketEvent event) {
        TicketStatus target = TRANSITIONS.get(new Key(current, event));
        if (target == null) {
            throw new InvalidTransitionException(current, event.name());
        }
        return target;
    }

    /** True when the ticket's fields may be edited in the given status (owner only). */
    public boolean isEditable(TicketStatus status) {
        return status == NEW || status == REJECTED || status == FOR_ADDITIONAL_INFO;
    }
}
