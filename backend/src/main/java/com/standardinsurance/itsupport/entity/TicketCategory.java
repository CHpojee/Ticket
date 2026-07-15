package com.standardinsurance.itsupport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ticket_categories")
public class TicketCategory {

    @Id
    @Column(nullable = false, updatable = false, length = 8)
    private String code;

    @Column(nullable = false)
    private String description;

    protected TicketCategory() {
        // JPA
    }

    public TicketCategory(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
