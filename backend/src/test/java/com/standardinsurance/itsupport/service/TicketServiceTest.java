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
    private final User leiva = new User("1002", "x", "Leiva");
    private final User rudy = new User("1003", "x", "Rudy");
    private final User rich = new User("1004", "x", "Rich");

    @BeforeEach
    void setUp() {
        service = new TicketService(ticketRepository, userRepository, categoryRepository,
                new TicketStateMachine(), restrictionService, auditService, events);
    }

    @Test
    void createChecksRestrictionAndAuditsCreation() {
        when(userRepository.findById("1002")).thenReturn(Optional.of(leiva));
        when(categoryRepository.findById("SR")).thenReturn(Optional.of(sr));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

        Ticket t = service.create("1002", new CreateTicketRequest("Printer", "broken", "SR"));

        assertThat(t.getStatus()).isEqualTo(TicketStatus.NEW);
        verify(restrictionService).assertAllowed("1002", "SR");
        verify(auditService).record(eq("1002"), any(), eq("TICKET_CREATED"), eq("status"),
                isNull(), eq("New"));
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
    void nonOwnerCannotSubmit() {
        Ticket ticket = new Ticket("t", "d", sr, leiva);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.submit("1004", 5L, null))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void requestorCannotApproveOwnTicket() {
        Ticket ticket = new Ticket("t", "d", sr, leiva);
        ticket.setStatus(TicketStatus.FOR_APPROVAL);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById("1002")).thenReturn(Optional.of(leiva));

        assertThatThrownBy(() -> service.approve("1002", 5L, null))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void submitTransitionsAuditsAndPublishesEvent() {
        Ticket ticket = new Ticket("t", "d", sr, leiva);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById("1002")).thenReturn(Optional.of(leiva));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

        Ticket result = service.submit("1002", 5L, null);

        assertThat(result.getStatus()).isEqualTo(TicketStatus.FOR_APPROVAL);
        verify(auditService).record(eq("1002"), any(), eq("TICKET_SUBMITTED"), eq("status"),
                eq("New"), eq("For Approval"));
        ArgumentCaptor<TicketTransitionEvent> ev = ArgumentCaptor.forClass(TicketTransitionEvent.class);
        verify(events).publishEvent(ev.capture());
        assertThat(ev.getValue().transition()).isEqualTo(TicketTransition.SUBMITTED);
    }

    @Test
    void approveAssignsApproverAndMovesToInProcess() {
        Ticket ticket = new Ticket("t", "d", sr, leiva);
        ticket.setStatus(TicketStatus.FOR_APPROVAL);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById("1004")).thenReturn(Optional.of(rich));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));

        Ticket result = service.approve("1004", 5L, "looks good");

        assertThat(result.getStatus()).isEqualTo(TicketStatus.IN_PROCESS);
        assertThat(result.getApprover()).isEqualTo(rich);
        // status row + comment row
        verify(auditService).record(eq("1004"), any(), eq("TICKET_APPROVED"), eq("comment"),
                isNull(), eq("looks good"));
    }
}
