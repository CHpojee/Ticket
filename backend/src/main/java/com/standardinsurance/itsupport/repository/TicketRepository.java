package com.standardinsurance.itsupport.repository;

import com.standardinsurance.itsupport.entity.Ticket;
import com.standardinsurance.itsupport.entity.TicketStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository
        extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    boolean existsByRequestor_UserId(String userId);

    long countByStatus(TicketStatus status);

    long countByStatusIn(List<TicketStatus> statuses);

    @Query("select t.category.code as code, t.category.description as description, "
            + "count(t) as cnt from Ticket t group by t.category.code, t.category.description")
    List<CategoryCountProjection> countGroupedByCategory();

    @Query("select t.status as status, count(t) as cnt from Ticket t group by t.status")
    List<StatusCountProjection> countGroupedByStatus();

    interface CategoryCountProjection {
        String getCode();

        String getDescription();

        long getCnt();
    }

    interface StatusCountProjection {
        TicketStatus getStatus();

        long getCnt();
    }
}
