package com.standardinsurance.itsupport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.standardinsurance.itsupport.entity.TicketStatus;
import com.standardinsurance.itsupport.exception.InvalidTransitionException;
import org.junit.jupiter.api.Test;

class TicketStateMachineTest {

    private final TicketStateMachine sm = new TicketStateMachine();

    @Test
    void happyPathTransitions() {
        assertThat(sm.next(TicketStatus.NEW, TicketEvent.SUBMIT)).isEqualTo(TicketStatus.FOR_APPROVAL);
        assertThat(sm.next(TicketStatus.FOR_APPROVAL, TicketEvent.APPROVE)).isEqualTo(TicketStatus.IN_PROCESS);
        assertThat(sm.next(TicketStatus.IN_PROCESS, TicketEvent.RESOLVE)).isEqualTo(TicketStatus.DONE_RESOLVED);
        assertThat(sm.next(TicketStatus.DONE_RESOLVED, TicketEvent.CLOSE)).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    void rejectAndInfoLoopBackOnResubmit() {
        assertThat(sm.next(TicketStatus.FOR_APPROVAL, TicketEvent.REJECT)).isEqualTo(TicketStatus.REJECTED);
        assertThat(sm.next(TicketStatus.REJECTED, TicketEvent.SUBMIT)).isEqualTo(TicketStatus.FOR_APPROVAL);
        assertThat(sm.next(TicketStatus.FOR_APPROVAL, TicketEvent.REQUEST_INFO))
                .isEqualTo(TicketStatus.FOR_ADDITIONAL_INFO);
        assertThat(sm.next(TicketStatus.FOR_ADDITIONAL_INFO, TicketEvent.SUBMIT))
                .isEqualTo(TicketStatus.FOR_APPROVAL);
    }

    @Test
    void illegalTransitionsThrow409() {
        assertThatThrownBy(() -> sm.next(TicketStatus.NEW, TicketEvent.APPROVE))
                .isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> sm.next(TicketStatus.CLOSED, TicketEvent.SUBMIT))
                .isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> sm.next(TicketStatus.IN_PROCESS, TicketEvent.CLOSE))
                .isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> sm.next(TicketStatus.FOR_APPROVAL, TicketEvent.CLOSE))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void editableOnlyInDraftLikeStates() {
        assertThat(sm.isEditable(TicketStatus.NEW)).isTrue();
        assertThat(sm.isEditable(TicketStatus.REJECTED)).isTrue();
        assertThat(sm.isEditable(TicketStatus.FOR_ADDITIONAL_INFO)).isTrue();
        assertThat(sm.isEditable(TicketStatus.FOR_APPROVAL)).isFalse();
        assertThat(sm.isEditable(TicketStatus.IN_PROCESS)).isFalse();
        assertThat(sm.isEditable(TicketStatus.CLOSED)).isFalse();
    }
}
