package com.standardinsurance.itsupport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.standardinsurance.itsupport.dto.UserDtos.CreateUserRequest;
import com.standardinsurance.itsupport.dto.UserDtos.UpdateUserRequest;
import com.standardinsurance.itsupport.entity.User;
import com.standardinsurance.itsupport.exception.ConflictException;
import com.standardinsurance.itsupport.repository.TicketRepository;
import com.standardinsurance.itsupport.repository.UserRepository;
import com.standardinsurance.itsupport.repository.UserRestrictionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRestrictionRepository restrictionRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private AuditService auditService;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, restrictionRepository, ticketRepository,
                encoder, auditService);
    }

    @Test
    void createRejectsDuplicateUserId() {
        when(userRepository.existsByUserId("1002")).thenReturn(true);
        assertThatThrownBy(() -> service.create("1001",
                new CreateUserRequest("1002", "Leiva", "pw", false, null)))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createHashesPasswordAndAudits() {
        when(userRepository.existsByUserId("2001")).thenReturn(false);
        when(restrictionRepository.findByUser_UserId("2001")).thenReturn(List.of());
        service.create("1001",
                new CreateUserRequest("2001", "New", "secret", true, "new@example.com"));
        verify(userRepository).save(any(User.class));
        verify(auditService).record("1001", null, "USER_CREATED", "userId", null, "2001");
    }

    @Test
    void updateChangesNameAndAudits() {
        User user = new User("1002", encoder.encode("Leiva"), "Leiva");
        when(userRepository.findById("1002")).thenReturn(Optional.of(user));
        when(restrictionRepository.findByUser_UserId("1002")).thenReturn(List.of());
        service.update("1001", "1002", new UpdateUserRequest("Leiva Renamed", null, null, null));
        assertThat(user.getName()).isEqualTo("Leiva Renamed");
        verify(auditService).record("1001", null, "USER_UPDATED", "name", "Leiva", "Leiva Renamed");
    }

    @Test
    void updateTogglesApproverAndEmail() {
        User user = new User("1004", encoder.encode("Rich"), "Rich"); // not an approver
        when(userRepository.findById("1004")).thenReturn(Optional.of(user));
        when(restrictionRepository.findByUser_UserId("1004")).thenReturn(List.of());

        service.update("1001", "1004", new UpdateUserRequest(null, null, true, "rich@x.com"));

        assertThat(user.isApprover()).isTrue();
        assertThat(user.getEmailAddress()).isEqualTo("rich@x.com");
        verify(auditService).record("1001", null, "USER_UPDATED", "approver", null, "Y");
        verify(auditService).record("1001", null, "USER_UPDATED", "emailAddress", null, "rich@x.com");
    }

    @Test
    void deleteBlockedWhenUserOwnsTickets() {
        when(userRepository.findById("1002"))
                .thenReturn(Optional.of(new User("1002", "x", "Leiva")));
        when(ticketRepository.existsByRequestor_UserId("1002")).thenReturn(true);
        assertThatThrownBy(() -> service.delete("1001", "1002"))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).delete(any());
    }
}
