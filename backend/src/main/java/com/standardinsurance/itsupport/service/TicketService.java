package com.standardinsurance.itsupport.service;

import com.standardinsurance.itsupport.dto.TicketDtos.CreateTicketRequest;
import com.standardinsurance.itsupport.dto.TicketDtos.UpdateTicketRequest;
import com.standardinsurance.itsupport.entity.Ticket;
import com.standardinsurance.itsupport.entity.TicketCategory;
import com.standardinsurance.itsupport.entity.TicketStatus;
import com.standardinsurance.itsupport.entity.User;
import com.standardinsurance.itsupport.exception.ForbiddenActionException;
import com.standardinsurance.itsupport.exception.InvalidTransitionException;
import com.standardinsurance.itsupport.exception.NotFoundException;
import com.standardinsurance.itsupport.notification.TicketTransitionEvent;
import com.standardinsurance.itsupport.repository.TicketCategoryRepository;
import com.standardinsurance.itsupport.repository.TicketRepository;
import com.standardinsurance.itsupport.repository.UserRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the ticket lifecycle: applies guards (ownership, approver identity,
 * restriction), delegates legality to {@link TicketStateMachine}, writes audit rows, and
 * publishes a transition event for post-commit email. See docs/specs/08-ticket-lifecycle.md.
 */
@Service
public class TicketService {

    private static final String ACTION_CREATED = "TICKET_CREATED";
    private static final String ACTION_UPDATED = "TICKET_UPDATED";
    private static final String FIELD_STATUS = "status";

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketCategoryRepository categoryRepository;
    private final TicketStateMachine stateMachine;
    private final RestrictionService restrictionService;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;

    public TicketService(TicketRepository ticketRepository,
                         UserRepository userRepository,
                         TicketCategoryRepository categoryRepository,
                         TicketStateMachine stateMachine,
                         RestrictionService restrictionService,
                         AuditService auditService,
                         ApplicationEventPublisher events) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.stateMachine = stateMachine;
        this.restrictionService = restrictionService;
        this.auditService = auditService;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<Ticket> list(String requestorId, TicketStatus status, String categoryCode) {
        Specification<Ticket> spec = Specification
                .where(TicketSpecifications.requestor(requestorId))
                .and(TicketSpecifications.statusIn(status == null ? null : List.of(status)))
                .and(TicketSpecifications.categoryIn(categoryCode == null ? null : List.of(categoryCode)));
        return ticketRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public Ticket get(Long id) {
        return require(id);
    }

    @Transactional
    public Ticket create(String actorId, CreateTicketRequest req) {
        User requestor = requireUser(actorId);
        TicketCategory category = categoryRepository.findById(req.categoryCode())
                .orElseThrow(() -> new NotFoundException("Unknown category " + req.categoryCode()));
        restrictionService.assertAllowed(actorId, category.getCode());

        Ticket ticket = new Ticket(req.title(), req.description(), category, requestor);
        ticketRepository.save(ticket);
        auditService.record(actorId, ticket.getId(), ACTION_CREATED, FIELD_STATUS,
                null, TicketStatus.NEW.getLabel());
        return ticket;
    }

    @Transactional
    public Ticket update(String actorId, Long id, UpdateTicketRequest req) {
        Ticket ticket = require(id);
        requireOwner(ticket, actorId);
        if (!stateMachine.isEditable(ticket.getStatus())) {
            throw new InvalidTransitionException(ticket.getStatus(), "edit");
        }
        restrictionService.assertAllowed(actorId, ticket.getCategory().getCode());

        if (!req.title().equals(ticket.getTitle())) {
            auditService.record(actorId, id, ACTION_UPDATED, "title",
                    ticket.getTitle(), req.title());
            ticket.setTitle(req.title());
        }
        String newDesc = req.description();
        if (!java.util.Objects.equals(newDesc, ticket.getDescription())) {
            auditService.record(actorId, id, ACTION_UPDATED, "description",
                    ticket.getDescription(), newDesc);
            ticket.setDescription(newDesc);
        }
        return ticketRepository.save(ticket);
    }

    // ---- Lifecycle transitions ----

    @Transactional
    public Ticket submit(String actorId, Long id, String comment) {
        Ticket ticket = require(id);
        requireOwner(ticket, actorId);
        restrictionService.assertAllowed(actorId, ticket.getCategory().getCode());
        boolean resubmit = ticket.getStatus() != TicketStatus.NEW;
        TicketStatus next = stateMachine.next(ticket.getStatus(), TicketEvent.SUBMIT);
        return applyTransition(ticket, next,
                resubmit ? TicketTransition.RESUBMITTED : TicketTransition.SUBMITTED,
                actorId, comment);
    }

    @Transactional
    public Ticket approve(String actorId, Long id, String comment) {
        return decide(actorId, id, TicketEvent.APPROVE, TicketTransition.APPROVED, comment, true);
    }

    @Transactional
    public Ticket reject(String actorId, Long id, String comment) {
        return decide(actorId, id, TicketEvent.REJECT, TicketTransition.REJECTED, comment, true);
    }

    @Transactional
    public Ticket requestInfo(String actorId, Long id, String comment) {
        return decide(actorId, id, TicketEvent.REQUEST_INFO, TicketTransition.INFO_REQUESTED,
                comment, true);
    }

    @Transactional
    public Ticket resolve(String actorId, Long id, String comment) {
        return decide(actorId, id, TicketEvent.RESOLVE, TicketTransition.RESOLVED, comment, false);
    }

    @Transactional
    public Ticket close(String actorId, Long id, String comment) {
        Ticket ticket = require(id);
        requireOwner(ticket, actorId); // only the original requestor may close
        restrictionService.assertAllowed(actorId, ticket.getCategory().getCode());
        TicketStatus next = stateMachine.next(ticket.getStatus(), TicketEvent.CLOSE);
        return applyTransition(ticket, next, TicketTransition.CLOSED, actorId, comment);
    }

    /** Approver-side actions: actor must not be the requestor; sets approver on decision. */
    private Ticket decide(String actorId, Long id, TicketEvent event,
                          TicketTransition transition, String comment, boolean assignApprover) {
        Ticket ticket = require(id);
        User actor = requireUser(actorId);
        if (actor.getUserId().equals(ticket.getRequestor().getUserId())) {
            throw new ForbiddenActionException(
                    "A requestor cannot " + event.name().toLowerCase() + " their own ticket");
        }
        restrictionService.assertAllowed(actorId, ticket.getCategory().getCode());
        TicketStatus next = stateMachine.next(ticket.getStatus(), event);
        if (assignApprover) {
            ticket.setApprover(actor);
        }
        return applyTransition(ticket, next, transition, actorId, comment);
    }

    private Ticket applyTransition(Ticket ticket, TicketStatus next, TicketTransition transition,
                                   String actorId, String comment) {
        String oldLabel = ticket.getStatus().getLabel();
        ticket.setStatus(next);
        ticketRepository.save(ticket);

        auditService.record(actorId, ticket.getId(), transition.auditAction(), FIELD_STATUS,
                oldLabel, next.getLabel());
        if (comment != null && !comment.isBlank()) {
            auditService.record(actorId, ticket.getId(), transition.auditAction(), "comment",
                    null, comment);
        }

        events.publishEvent(new TicketTransitionEvent(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getCategory().getCode(),
                oldLabel,
                next.getLabel(),
                transition,
                ticket.getRequestor().getUserId(),
                ticket.getApprover() == null ? null : ticket.getApprover().getUserId(),
                requireUser(actorId).getName()));
        return ticket;
    }

    private Ticket require(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket " + id + " not found"));
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Unknown user " + userId));
    }

    private void requireOwner(Ticket ticket, String actorId) {
        if (!ticket.getRequestor().getUserId().equals(actorId)) {
            throw new ForbiddenActionException(
                    "Only the requestor may perform this action on ticket " + ticket.getId());
        }
    }
}
