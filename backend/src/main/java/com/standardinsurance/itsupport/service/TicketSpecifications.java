package com.standardinsurance.itsupport.service;

import com.standardinsurance.itsupport.entity.Ticket;
import com.standardinsurance.itsupport.entity.TicketStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Reusable JPA specifications for ticket queries (list + reports). */
public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> requestor(String requestorId) {
        return (root, query, cb) ->
                requestorId == null ? null : cb.equal(root.get("requestor").get("userId"), requestorId);
    }

    public static Specification<Ticket> statusIn(List<TicketStatus> statuses) {
        return (root, query, cb) ->
                (statuses == null || statuses.isEmpty()) ? null : root.get("status").in(statuses);
    }

    public static Specification<Ticket> categoryIn(List<String> codes) {
        return (root, query, cb) ->
                (codes == null || codes.isEmpty()) ? null
                        : root.get("category").get("code").in(codes);
    }

    public static Specification<Ticket> createdFrom(Instant from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Ticket> createdTo(Instant to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
