package com.standardinsurance.itsupport.service;

import static com.standardinsurance.itsupport.entity.TicketStatus.CLOSED;
import static com.standardinsurance.itsupport.entity.TicketStatus.DONE_RESOLVED;
import static com.standardinsurance.itsupport.entity.TicketStatus.FOR_ADDITIONAL_INFO;
import static com.standardinsurance.itsupport.entity.TicketStatus.FOR_APPROVAL;
import static com.standardinsurance.itsupport.entity.TicketStatus.FOR_SECOND_APPROVAL;
import static com.standardinsurance.itsupport.entity.TicketStatus.IN_PROCESS;
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

    private static final Map<Key, TicketStatus> TRANSITIONS = Map.ofEntries(
            // Two-stage approval: first approver then second approver.
            Map.entry(new Key(FOR_APPROVAL, TicketEvent.APPROVE), FOR_SECOND_APPROVAL),
            Map.entry(new Key(FOR_SECOND_APPROVAL, TicketEvent.APPROVE), IN_PROCESS),
            // Reject / request-info at either approval stage returns to the requestor.
            Map.entry(new Key(FOR_APPROVAL, TicketEvent.REJECT), REJECTED),
            Map.entry(new Key(FOR_SECOND_APPROVAL, TicketEvent.REJECT), REJECTED),
            Map.entry(new Key(FOR_APPROVAL, TicketEvent.REQUEST_INFO), FOR_ADDITIONAL_INFO),
            Map.entry(new Key(FOR_SECOND_APPROVAL, TicketEvent.REQUEST_INFO), FOR_ADDITIONAL_INFO),
            // Requestor resubmits a rejected / info-requested ticket back to the first stage.
            Map.entry(new Key(REJECTED, TicketEvent.SUBMIT), FOR_APPROVAL),
            Map.entry(new Key(FOR_ADDITIONAL_INFO, TicketEvent.SUBMIT), FOR_APPROVAL),
            Map.entry(new Key(IN_PROCESS, TicketEvent.RESOLVE), DONE_RESOLVED),
            Map.entry(new Key(DONE_RESOLVED, TicketEvent.CLOSE), CLOSED));

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
        return status == REJECTED || status == FOR_ADDITIONAL_INFO;
    }

    /**
     * Required approver level for an approval-stage status, or {@code null} if the status is
     * not awaiting an approval decision.
     */
    public Integer requiredApproverLevel(TicketStatus status) {
        if (status == FOR_APPROVAL) {
            return 1;
        }
        if (status == FOR_SECOND_APPROVAL) {
            return 2;
        }
        return null;
    }
}
