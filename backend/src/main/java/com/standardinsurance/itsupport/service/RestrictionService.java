package com.standardinsurance.itsupport.service;

import com.standardinsurance.itsupport.entity.TicketCategory;
import com.standardinsurance.itsupport.entity.User;
import com.standardinsurance.itsupport.entity.UserRestriction;
import com.standardinsurance.itsupport.exception.ConflictException;
import com.standardinsurance.itsupport.exception.NotFoundException;
import com.standardinsurance.itsupport.exception.RestrictionViolationException;
import com.standardinsurance.itsupport.repository.TicketCategoryRepository;
import com.standardinsurance.itsupport.repository.UserRepository;
import com.standardinsurance.itsupport.repository.UserRestrictionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns category-restriction rules. The {@link #assertAllowed} guard is the single point
 * every category-specific action funnels through. See docs/specs/02-user-maintenance.md.
 */
@Service
public class RestrictionService {

    private final UserRestrictionRepository restrictionRepo;
    private final UserRepository userRepo;
    private final TicketCategoryRepository categoryRepo;

    public RestrictionService(UserRestrictionRepository restrictionRepo,
                              UserRepository userRepo,
                              TicketCategoryRepository categoryRepo) {
        this.restrictionRepo = restrictionRepo;
        this.userRepo = userRepo;
        this.categoryRepo = categoryRepo;
    }

    /** Throws 403 if the user is restricted from the given category. */
    public void assertAllowed(String userId, String categoryCode) {
        if (restrictionRepo.existsByUser_UserIdAndCategory_Code(userId, categoryCode)) {
            throw new RestrictionViolationException(userId, categoryCode);
        }
    }

    @Transactional(readOnly = true)
    public List<String> listCategoryCodes(String userId) {
        requireUser(userId);
        return restrictionRepo.findByUser_UserId(userId).stream()
                .map(r -> r.getCategory().getCode())
                .toList();
    }

    @Transactional
    public void add(String userId, String categoryCode) {
        User user = requireUser(userId);
        TicketCategory category = categoryRepo.findById(categoryCode)
                .orElseThrow(() -> new NotFoundException("Unknown category " + categoryCode));
        if (restrictionRepo.existsByUser_UserIdAndCategory_Code(userId, categoryCode)) {
            throw new ConflictException(
                    "User " + userId + " is already restricted from category " + categoryCode);
        }
        restrictionRepo.save(new UserRestriction(user, category));
    }

    @Transactional
    public void remove(String userId, String categoryCode) {
        UserRestriction restriction = restrictionRepo
                .findByUser_UserIdAndCategory_Code(userId, categoryCode)
                .orElseThrow(() -> new NotFoundException(
                        "No restriction for user " + userId + " on category " + categoryCode));
        restrictionRepo.delete(restriction);
    }

    private User requireUser(String userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Unknown user " + userId));
    }
}
