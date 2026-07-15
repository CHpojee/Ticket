package com.standardinsurance.itsupport.dto;

import java.util.List;

/** Dashboard aggregate response. See docs/specs/05-dashboard.md. */
public record DashboardSummary(
        long totalTickets,
        long totalOpen,
        long pendingApprovals,
        long completed,
        List<CategoryCount> byCategory,
        List<StatusCount> byStatus) {

    public record CategoryCount(String code, String description, long count) {
    }

    public record StatusCount(String status, long count) {
    }
}
