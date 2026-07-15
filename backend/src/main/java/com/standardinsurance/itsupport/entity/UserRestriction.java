package com.standardinsurance.itsupport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A category-specific restriction: {@code userId} may not act on tickets of
 * {@code ticketCategoryCode}. See docs/specs/02-user-maintenance.md.
 */
@Entity
@Table(name = "user_restrictions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_category",
                columnNames = {"user_id", "ticket_category_code"}))
public class UserRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_restriction_user"))
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_category_code", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_restriction_category"))
    private TicketCategory category;

    protected UserRestriction() {
        // JPA
    }

    public UserRestriction(User user, TicketCategory category) {
        this.user = user;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public TicketCategory getCategory() {
        return category;
    }
}
