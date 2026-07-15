package com.standardinsurance.itsupport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Append-only audit record. See docs/specs/03-audit-trail.md.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(length = 40)
    private String field;

    @Column(name = "old_value", length = 512)
    private String oldValue;

    @Column(name = "new_value", length = 512)
    private String newValue;

    @Column(nullable = false)
    private Instant timestamp;

    protected AuditLog() {
        // JPA
    }

    public AuditLog(Long ticketId, String actorId, String action, String field,
                    String oldValue, String newValue) {
        this.ticketId = ticketId;
        this.actorId = actorId;
        this.action = action;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.timestamp = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public String getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getField() {
        return field;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
