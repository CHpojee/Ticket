package com.standardinsurance.itsupport.config;

import com.standardinsurance.itsupport.entity.TicketCategory;
import com.standardinsurance.itsupport.entity.User;
import com.standardinsurance.itsupport.entity.UserRestriction;
import com.standardinsurance.itsupport.repository.TicketCategoryRepository;
import com.standardinsurance.itsupport.repository.UserRepository;
import com.standardinsurance.itsupport.repository.UserRestrictionRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the exact dataset from CLAUDE.md / docs/specs on startup. Idempotent: only
 * populates empty tables so restarts and tests are safe.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final TicketCategoryRepository categoryRepository;
    private final UserRestrictionRepository restrictionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      TicketCategoryRepository categoryRepository,
                      UserRestrictionRepository restrictionRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.restrictionRepository = restrictionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedCategories();
        seedUsers();
        seedRestrictions();
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }
        categoryRepository.saveAll(List.of(
                new TicketCategory("SR", "Service Request"),
                new TicketCategory("DB", "Database Fix (DB Fix)"),
                new TicketCategory("MR", "Mass Request / Bulk Action"),
                new TicketCategory("BW", "BCP Whitelisting (Business Continuity Plan)"),
                new TicketCategory("IR", "Incident Report (IR)")));
        log.info("Seeded {} ticket categories", categoryRepository.count());
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        // Plaintext seed credentials; stored BCrypt-hashed. approver 'Y' = system approver.
        userRepository.saveAll(List.of(
                user("1001", "Admin", "Admin", null, null),
                user("1002", "Leiva", "Leiva", "Y", "rreyes@stand-insurance.com"),
                user("1003", "Rudy", "Rudy", null, "richeercoronareyes@gmail.com"),
                user("1004", "Rich", "Rich", null, null),
                user("1005", "Paw", "Paw", null, "clualhati@standard-insurance.com")));
        log.info("Seeded {} users", userRepository.count());
    }

    private void seedRestrictions() {
        if (restrictionRepository.count() > 0) {
            return;
        }
        User rudy = userRepository.findById("1003").orElseThrow();
        TicketCategory db = categoryRepository.findById("DB").orElseThrow();
        restrictionRepository.save(new UserRestriction(rudy, db));
        log.info("Seeded restriction: user 1003 -> category DB");
    }

    private User user(String userId, String password, String name, String approver,
                      String emailAddress) {
        return new User(userId, passwordEncoder.encode(password), name, approver, emailAddress);
    }
}
