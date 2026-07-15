package com.standardinsurance.itsupport.dto;

import com.standardinsurance.itsupport.entity.Ticket;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/** Ticket payloads. See docs/specs/08-ticket-lifecycle.md. */
public final class TicketDtos {

    private TicketDtos() {
    }

    public record CreateTicketRequest(
            @NotBlank(message = "is required") String title,
            String description,
            @NotBlank(message = "is required") String categoryCode) {
    }

    public record UpdateTicketRequest(
            @NotBlank(message = "is required") String title,
            String description) {
    }

    /** Optional comment on approve/reject/request-info; stored on the audit row. */
    public record DecisionRequest(String comment) {
    }

    public record TicketDto(
            Long id,
            String title,
            String description,
            String categoryCode,
            String categoryDescription,
            String status,
            String requestorId,
            String requestorName,
            String approverId,
            String approverName,
            Instant createdAt,
            Instant updatedAt) {

        public static TicketDto from(Ticket t) {
            return new TicketDto(
                    t.getId(),
                    t.getTitle(),
                    t.getDescription(),
                    t.getCategory().getCode(),
                    t.getCategory().getDescription(),
                    t.getStatus().getLabel(),
                    t.getRequestor().getUserId(),
                    t.getRequestor().getName(),
                    t.getApprover() == null ? null : t.getApprover().getUserId(),
                    t.getApprover() == null ? null : t.getApprover().getName(),
                    t.getCreatedAt(),
                    t.getUpdatedAt());
        }
    }
}
