package com.standardinsurance.itsupport.service;

import com.standardinsurance.itsupport.dto.DashboardSummary;
import com.standardinsurance.itsupport.dto.DashboardSummary.CategoryCount;
import com.standardinsurance.itsupport.dto.DashboardSummary.StatusCount;
import com.standardinsurance.itsupport.entity.TicketStatus;
import com.standardinsurance.itsupport.repository.TicketCategoryRepository;
import com.standardinsurance.itsupport.repository.TicketRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Computes dashboard aggregates via GROUP BY queries. See docs/specs/05-dashboard.md. */
@Service
public class DashboardService {

    private static final List<TicketStatus> OPEN_STATES = List.of(
            TicketStatus.FOR_APPROVAL, TicketStatus.FOR_SECOND_APPROVAL,
            TicketStatus.FOR_ADDITIONAL_INFO, TicketStatus.REJECTED, TicketStatus.IN_PROCESS);
    private static final List<TicketStatus> COMPLETED_STATES = List.of(
            TicketStatus.DONE_RESOLVED, TicketStatus.CLOSED);

    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository categoryRepository;

    public DashboardService(TicketRepository ticketRepository,
                            TicketCategoryRepository categoryRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        long total = ticketRepository.count();
        long open = ticketRepository.countByStatusIn(OPEN_STATES);
        long pending = ticketRepository.countByStatus(TicketStatus.FOR_APPROVAL);
        long completed = ticketRepository.countByStatusIn(COMPLETED_STATES);

        return new DashboardSummary(total, open, pending, completed,
                byCategory(), byStatus());
    }

    private List<CategoryCount> byCategory() {
        Map<String, Long> counts = new LinkedHashMap<>();
        ticketRepository.countGroupedByCategory()
                .forEach(p -> counts.put(p.getCode(), p.getCnt()));
        // Ensure every category appears (0 if none).
        return categoryRepository.findAll().stream()
                .map(c -> new CategoryCount(c.getCode(), c.getDescription(),
                        counts.getOrDefault(c.getCode(), 0L)))
                .toList();
    }

    private List<StatusCount> byStatus() {
        Map<TicketStatus, Long> counts = new LinkedHashMap<>();
        ticketRepository.countGroupedByStatus()
                .forEach(p -> counts.put(p.getStatus(), p.getCnt()));
        // Ensure every lifecycle status appears (0 if none).
        return java.util.Arrays.stream(TicketStatus.values())
                .map(s -> new StatusCount(s.getLabel(), counts.getOrDefault(s, 0L)))
                .toList();
    }
}
