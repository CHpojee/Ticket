package com.standardinsurance.itsupport.repository;

import com.standardinsurance.itsupport.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByTicketIdOrderByTimestampAscIdAsc(Long ticketId);
}
