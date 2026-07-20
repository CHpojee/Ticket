package com.standardinsurance.itsupport.repository;

import com.standardinsurance.itsupport.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUserId(String userId);

    /** System approvers (users whose approver flag equals the given value, e.g. "Y"). */
    List<User> findByApprover(String approver);

    /** System approvers at a given approval stage (level 1 = first, 2 = second). */
    List<User> findByApproverAndApproverLevel(String approver, Integer approverLevel);
}
