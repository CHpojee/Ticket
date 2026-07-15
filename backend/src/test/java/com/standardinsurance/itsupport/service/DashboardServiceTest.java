package com.standardinsurance.itsupport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.standardinsurance.itsupport.dto.DashboardSummary;
import com.standardinsurance.itsupport.entity.TicketCategory;
import com.standardinsurance.itsupport.entity.TicketStatus;
import com.standardinsurance.itsupport.repository.TicketCategoryRepository;
import com.standardinsurance.itsupport.repository.TicketRepository;
import com.standardinsurance.itsupport.repository.TicketRepository.CategoryCountProjection;
import com.standardinsurance.itsupport.repository.TicketRepository.StatusCountProjection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private TicketCategoryRepository categoryRepository;

    @Test
    void summaryComputesCardsAndFillsAllCategoriesAndStatuses() {
        when(ticketRepository.count()).thenReturn(3L);
        when(ticketRepository.countByStatusIn(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(2L, 1L); // open, then completed
        when(ticketRepository.countByStatus(TicketStatus.FOR_APPROVAL)).thenReturn(1L);

        CategoryCountProjection srCount = mock(CategoryCountProjection.class);
        when(srCount.getCode()).thenReturn("SR");
        when(srCount.getCnt()).thenReturn(3L);
        when(ticketRepository.countGroupedByCategory()).thenReturn(List.of(srCount));

        StatusCountProjection st = mock(StatusCountProjection.class);
        when(st.getStatus()).thenReturn(TicketStatus.FOR_APPROVAL);
        when(st.getCnt()).thenReturn(1L);
        when(ticketRepository.countGroupedByStatus()).thenReturn(List.of(st));

        when(categoryRepository.findAll()).thenReturn(List.of(
                new TicketCategory("SR", "Service Request"),
                new TicketCategory("DB", "Database Fix (DB Fix)")));

        DashboardService service = new DashboardService(ticketRepository, categoryRepository);
        DashboardSummary s = service.summary();

        assertThat(s.totalTickets()).isEqualTo(3);
        assertThat(s.totalOpen()).isEqualTo(2);
        assertThat(s.pendingApprovals()).isEqualTo(1);
        assertThat(s.completed()).isEqualTo(1);
        // both categories present, DB filled with 0
        assertThat(s.byCategory()).hasSize(2);
        assertThat(s.byCategory()).anySatisfy(c -> {
            assertThat(c.code()).isEqualTo("DB");
            assertThat(c.count()).isZero();
        });
        // all 7 statuses represented
        assertThat(s.byStatus()).hasSize(7);
    }
}
