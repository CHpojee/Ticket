package com.standardinsurance.itsupport.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.standardinsurance.itsupport.entity.TicketCategory;
import com.standardinsurance.itsupport.entity.User;
import com.standardinsurance.itsupport.entity.UserRestriction;
import com.standardinsurance.itsupport.exception.ConflictException;
import com.standardinsurance.itsupport.exception.NotFoundException;
import com.standardinsurance.itsupport.exception.RestrictionViolationException;
import com.standardinsurance.itsupport.repository.TicketCategoryRepository;
import com.standardinsurance.itsupport.repository.UserRepository;
import com.standardinsurance.itsupport.repository.UserRestrictionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestrictionServiceTest {

    @Mock private UserRestrictionRepository restrictionRepo;
    @Mock private UserRepository userRepo;
    @Mock private TicketCategoryRepository categoryRepo;
    @InjectMocks private RestrictionService service;

    private final User rudy = new User("1003", "x", "Rudy");
    private final TicketCategory db = new TicketCategory("DB", "Database Fix (DB Fix)");

    @Test
    void assertAllowedBlocksRestrictedUser() {
        when(restrictionRepo.existsByUser_UserIdAndCategory_Code("1003", "DB")).thenReturn(true);
        assertThatThrownBy(() -> service.assertAllowed("1003", "DB"))
                .isInstanceOf(RestrictionViolationException.class);
    }

    @Test
    void assertAllowedPassesForUnrestrictedUser() {
        when(restrictionRepo.existsByUser_UserIdAndCategory_Code("1003", "SR")).thenReturn(false);
        assertThatCode(() -> service.assertAllowed("1003", "SR")).doesNotThrowAnyException();
    }

    @Test
    void addRejectsDuplicate() {
        when(userRepo.findById("1003")).thenReturn(Optional.of(rudy));
        when(categoryRepo.findById("DB")).thenReturn(Optional.of(db));
        when(restrictionRepo.existsByUser_UserIdAndCategory_Code("1003", "DB")).thenReturn(true);
        assertThatThrownBy(() -> service.add("1003", "DB"))
                .isInstanceOf(ConflictException.class);
        verify(restrictionRepo, never()).save(any());
    }

    @Test
    void addRejectsUnknownCategory() {
        when(userRepo.findById("1003")).thenReturn(Optional.of(rudy));
        when(categoryRepo.findById("ZZ")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.add("1003", "ZZ"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addPersistsNewRestriction() {
        when(userRepo.findById("1003")).thenReturn(Optional.of(rudy));
        when(categoryRepo.findById("DB")).thenReturn(Optional.of(db));
        when(restrictionRepo.existsByUser_UserIdAndCategory_Code("1003", "DB")).thenReturn(false);
        service.add("1003", "DB");
        verify(restrictionRepo).save(any(UserRestriction.class));
    }

    @Test
    void removeRejectsMissingRestriction() {
        when(restrictionRepo.findByUser_UserIdAndCategory_Code("1003", "DB"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.remove("1003", "DB"))
                .isInstanceOf(NotFoundException.class);
    }
}
