package com.standardinsurance.itsupport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.standardinsurance.itsupport.dto.TicketDtos.CreateTicketRequest;
import com.standardinsurance.itsupport.entity.Ticket;
import com.standardinsurance.itsupport.entity.TicketCategory;
import com.standardinsurance.itsupport.entity.TicketStatus;
import com.standardinsurance.itsupport.entity.User;
import com.standardinsurance.itsupport.exception.ForbiddenActionException;
import com.standardinsurance.itsupport.exception.RestrictionViolationException;
import com.standardinsurance.itsupport.notification.TicketTransitionEvent;
import com.standardinsurance.itsupport.repository.TicketCategoryRepository;
import com.standardinsurance.itsupport.repository.TicketRepository;
import com.standardinsurance.itsupport.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private TicketCategoryRepository categoryRepository;
    @Mock private RestrictionService restrictionService;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher events;

    private TicketService service;

    private final TicketCategory sr = new TicketCategory("SR", "Service Request");
    private final TicketCategory db = new TicketCategory("DB", "Database Fix (DB Fix)");
    private final User leiva = new User("1002", "x", "Leiva", "Y", 1, "leiva@x.com"); // L1 approver
    private final User rudy = new User("1003", "x", "Rudy", "Y", 2, "rudy@x.com"); // L2 approver
    private final User rich = new User("1004", "x", "Rich"); // requestor / non-approver
    private final User paw = new User("1005", "x", "Paw"); // non-approver

    @BeforeEach
    void setUp() {
        service = new TicketService(ticketRepository, userRepository, categoryRepository,
                new TicketStateMachine(), restrictionService, auditService, events);
    }

    @Test
    void createGoesStraightToForApprovalAndNotifiesApprovers() {
        when(userRepository.findById("1004")).thenReturn(Optional.of(rich));
        when(categoryRepository.findById("SR")).thenReturn(Optional.of(sr));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

        Ticket t = service.create("1004", new CreateTicketRequest("Printer", "broken", "SR"));

        assertThat(t.getStatus()).isEqualTo(TicketStatus.FOR_APPROVAL);
        verify(restrictionService).assertAllowed("1004", "SR");
        verify(auditService).record(eq("1004"), any(), eq("TICKET_CREATED"), eq("status"),
                isNull(), eq("For Approval"));
        ArgumentCaptor<TicketTransitionEvent> ev = ArgumentCaptor.forClass(TicketTransitionEvent.class);
        verify(events).publishEvent(ev.capture());
        assertThat(ev.getValue().transition()).isEqualTo(TicketTransition.SUBMITTED);
    }

    @Test
    void createBlockedByRestriction() {
        when(userRepository.findById("1003")).thenReturn(Optional.of(rudy));
        when(categoryRepository.findById("DB")).thenReturn(Optional.of(db));
        doThrow(new RestrictionViolationException("1003", "DB"))
                .when(restrictionService).assertAllowed("1003", "DB");

        assertThatThrownBy(() -> service.create("1003",
                new CreateTicketRequest("Fix", "x", "DB")))
                .isInstanceOf(RestrictionViolationException.class);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void nonOwnerCannotResubmit() {
        Ticket ticket = new Ticket("t", "d", sr, rich);
        ticket.setStatus(TicketStatus.REJECTED);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.submit("1005", 5L, null))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void resubmitReturnsToFirstApproval() {
        Ticket ticket = new Ticket("t", "d", sr, rich);
        ticket.setStatus(TicketStatus.REJECTED);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById("1004")).thenReturn(Optional.of(rich));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

        Ticket result = service.submit("1004", 5L, null);

        assertThat(result.getStatus()).isEqualTo(TicketStatus.FOR_APPROVAL);
        verify(auditService).record(eq("1004"), any(), eq("TICKET_RESUBMITTED"), eq("status"),
                eq("Rejected"), eq("For Approval"));
    }

    @Test
    void requestorCannotApproveOwnTicket() {
        Ticket ticket = new Ticket("t", "d", sr, leiva); // Leiva is both requestor and an approver
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById("1002")).thenReturn(Optional.of(leiva));

        assertThatThrownBy(() -> service.approve("1002", 5L, null))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void firstApprovalByLevel1MovesToSecondApproval() {
        Ticket ticket = new Ticket("t", "d", sr, rich); // requestor Rich, status For Approval
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById("1002")).thenReturn(Optional.of(leiva));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

        Ticket result = service.approve("1002", 5L, "ok");

        assertThat(result.getStatus()).isEqualTo(TicketStatus.FOR_SECOND_APPROVAL);
        assertThat(result.getApprover()).isEqualTo(leiva);
        verify(auditService).record(eq("1002"), any(), eq("TICKET_APPROVED_L1"), eq("status"),
                eq("For Approval"), eq("For Second Approval"));
    }

    @Test
    void secondApprovalByLevel2MovesToInProcess() {
        Ticket ticket = new Ticket("t", "d", sr, rich);
        ticket.setStatus(TicketStatus.FOR_SECOND_APPROVAL);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById("1003")).thenReturn(Optional.of(rudy));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

        Ticket result = service.approve("1003", 5L, null);

        assertThat(result.getStatus()).isEqualTo(TicketStatus.IN_PROCESS);
        verify(auditService).record(eq("1003"), any(), eq("TICKET_APPROVED_L2"), eq("status"),
                eq("For Second Approval"), eq("In Process"));
    }

    @Test
    void wrongLevelApproverIsRejected() {
        Ticket ticket = new Ticket("t", "d", sr, rich); // For Approval requires level 1
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById("1003")).thenReturn(Optional.of(rudy)); // Rudy is level 2

        assertThatThrownBy(() -> service.approve("1003", 5L, null))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("not a level-1 approver");
    }

    @Test
    void nonApproverCannotApprove() {
        Ticket ticket = new Ticket("t", "d", sr, rich);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById("1005")).thenReturn(Optional.of(paw));

        assertThatThrownBy(() -> service.approve("1005", 5L, null))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("not a system approver");
    }
}
