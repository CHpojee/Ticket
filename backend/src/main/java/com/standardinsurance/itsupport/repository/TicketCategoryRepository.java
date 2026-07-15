package com.standardinsurance.itsupport.repository;

import com.standardinsurance.itsupport.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, String> {
}
