package com.standardinsurance.itsupport.service;

import com.standardinsurance.itsupport.dto.AuditLogDto;
import com.standardinsurance.itsupport.entity.AuditLog;
import com.standardinsurance.itsupport.repository.AuditLogRepository;
import com.standardinsurance.itsupport.repository.UserRepository;
import java.time.Instant;
import com.standardinsurance.itsupport.entity.User;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of the audit trail. See docs/specs/03-audit-trail.md. */
@Service
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditQueryService(AuditLogRepository auditLogRepository,
                             UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> forTicket(Long ticketId) {
        return toDtos(auditLogRepository.findByTicketIdOrderByTimestampAscIdAsc(ticketId));
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> search(Instant from, Instant to, String actorId, String action) {
        Specification<AuditLog> spec = Specification.where(gte("timestamp", from))
                .and(lte("timestamp", to))
                .and(eq("actorId", actorId))
                .and(eq("action", action));
        return toDtos(auditLogRepository.findAll(spec, Sort.by("timestamp").ascending()));
    }

    private List<AuditLogDto> toDtos(List<AuditLog> logs) {
        Map<String, String> names = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getUserId, User::getName));
        return logs.stream()
                .map(a -> AuditLogDto.from(a, names.getOrDefault(a.getActorId(), a.getActorId())))
                .toList();
    }

    private Specification<AuditLog> eq(String field, String value) {
        return (root, query, cb) -> value == null ? null : cb.equal(root.get(field), value);
    }

    private Specification<AuditLog> gte(String field, Instant value) {
        return (root, query, cb) ->
                value == null ? null : cb.greaterThanOrEqualTo(root.get(field), value);
    }

    private Specification<AuditLog> lte(String field, Instant value) {
        return (root, query, cb) ->
                value == null ? null : cb.lessThanOrEqualTo(root.get(field), value);
    }
}
