package com.standardinsurance.itsupport.controller;

import com.standardinsurance.itsupport.dto.TicketDtos.CreateTicketRequest;
import com.standardinsurance.itsupport.dto.TicketDtos.DecisionRequest;
import com.standardinsurance.itsupport.dto.TicketDtos.TicketDto;
import com.standardinsurance.itsupport.dto.TicketDtos.UpdateTicketRequest;
import com.standardinsurance.itsupport.entity.TicketStatus;
import com.standardinsurance.itsupport.security.CurrentUser;
import com.standardinsurance.itsupport.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Ticket lifecycle endpoints. See docs/specs/08-ticket-lifecycle.md. */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final CurrentUser currentUser;

    public TicketController(TicketService ticketService, CurrentUser currentUser) {
        this.ticketService = ticketService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<TicketDto> list(
            @RequestParam(required = false, defaultValue = "false") boolean mine,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        String requestorId = mine ? currentUser.userId() : null;
        TicketStatus statusFilter = TicketStatus.fromLabelOrName(status);
        return ticketService.list(requestorId, statusFilter, category).stream()
                .map(TicketDto::from).toList();
    }

    @GetMapping("/{id}")
    public TicketDto get(@PathVariable Long id) {
        return TicketDto.from(ticketService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDto create(@Valid @RequestBody CreateTicketRequest request) {
        return TicketDto.from(ticketService.create(currentUser.userId(), request));
    }

    @PutMapping("/{id}")
    public TicketDto update(@PathVariable Long id,
                            @Valid @RequestBody UpdateTicketRequest request) {
        return TicketDto.from(ticketService.update(currentUser.userId(), id, request));
    }

    @PostMapping("/{id}/submit")
    public TicketDto submit(@PathVariable Long id,
                            @RequestBody(required = false) DecisionRequest body) {
        return TicketDto.from(ticketService.submit(currentUser.userId(), id, comment(body)));
    }

    @PostMapping("/{id}/approve")
    public TicketDto approve(@PathVariable Long id,
                             @RequestBody(required = false) DecisionRequest body) {
        return TicketDto.from(ticketService.approve(currentUser.userId(), id, comment(body)));
    }

    @PostMapping("/{id}/reject")
    public TicketDto reject(@PathVariable Long id,
                            @RequestBody(required = false) DecisionRequest body) {
        return TicketDto.from(ticketService.reject(currentUser.userId(), id, comment(body)));
    }

    @PostMapping("/{id}/request-info")
    public TicketDto requestInfo(@PathVariable Long id,
                                 @RequestBody(required = false) DecisionRequest body) {
        return TicketDto.from(ticketService.requestInfo(currentUser.userId(), id, comment(body)));
    }

    @PostMapping("/{id}/resolve")
    public TicketDto resolve(@PathVariable Long id,
                             @RequestBody(required = false) DecisionRequest body) {
        return TicketDto.from(ticketService.resolve(currentUser.userId(), id, comment(body)));
    }

    @PostMapping("/{id}/close")
    public TicketDto close(@PathVariable Long id,
                           @RequestBody(required = false) DecisionRequest body) {
        return TicketDto.from(ticketService.close(currentUser.userId(), id, comment(body)));
    }

    private String comment(DecisionRequest body) {
        return body == null ? null : body.comment();
    }
}
