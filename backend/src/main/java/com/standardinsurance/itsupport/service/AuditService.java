package com.standardinsurance.itsupport.service;

import com.standardinsurance.itsupport.entity.AuditLog;
import com.standardinsurance.itsupport.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

/**
 * Single entry point for writing audit records. Called within the same transaction as
 * the mutation it records. See docs/specs/03-audit-trail.md.
 */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public AuditLog record(String actorId, Long ticketId, String action, String field,
                           String oldValue, String newValue) {
        return repository.save(new AuditLog(ticketId, actorId, action, field, oldValue, newValue));
    }
}
