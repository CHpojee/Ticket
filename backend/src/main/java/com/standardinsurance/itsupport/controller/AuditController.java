package com.standardinsurance.itsupport.controller;

import com.standardinsurance.itsupport.dto.AuditLogDto;
import com.standardinsurance.itsupport.service.AuditQueryService;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Audit trail read endpoints. See docs/specs/03-audit-trail.md. */
@RestController
public class AuditController {

    private final AuditQueryService auditQueryService;

    public AuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping("/api/tickets/{id}/audit")
    public List<AuditLogDto> forTicket(@PathVariable Long id) {
        return auditQueryService.forTicket(id);
    }

    /** Global filtered log; admin-only via /api/admin/** security rule. */
    @GetMapping("/api/admin/audit")
    public List<AuditLogDto> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String action) {
        return auditQueryService.search(from, to, actorId, action);
    }
}
