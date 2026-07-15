package com.standardinsurance.itsupport.repository;

import com.standardinsurance.itsupport.entity.UserRestriction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRestrictionRepository extends JpaRepository<UserRestriction, Long> {

    boolean existsByUser_UserIdAndCategory_Code(String userId, String categoryCode);

    List<UserRestriction> findByUser_UserId(String userId);

    Optional<UserRestriction> findByUser_UserIdAndCategory_Code(String userId, String categoryCode);
}
