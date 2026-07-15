package com.standardinsurance.itsupport.dto;

import com.standardinsurance.itsupport.entity.AuditLog;
import java.time.Instant;

public record AuditLogDto(
        Long id,
        Long ticketId,
        String actorId,
        String actorName,
        String action,
        String field,
        String oldValue,
        String newValue,
        Instant timestamp) {

    public static AuditLogDto from(AuditLog a, String actorName) {
        return new AuditLogDto(
                a.getId(),
                a.getTicketId(),
                a.getActorId(),
                actorName,
                a.getAction(),
                a.getField(),
                a.getOldValue(),
                a.getNewValue(),
                a.getTimestamp());
    }
}
