package com.standardinsurance.itsupport.repository;

import com.standardinsurance.itsupport.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUserId(String userId);
}
