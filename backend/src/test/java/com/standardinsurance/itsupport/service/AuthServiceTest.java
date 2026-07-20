package com.standardinsurance.itsupport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.standardinsurance.itsupport.dto.AuthDtos.LoginRequest;
import com.standardinsurance.itsupport.dto.AuthDtos.LoginResponse;
import com.standardinsurance.itsupport.entity.User;
import com.standardinsurance.itsupport.exception.InvalidCredentialsException;
import com.standardinsurance.itsupport.repository.UserRepository;
import com.standardinsurance.itsupport.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtService jwtService =
            new JwtService("test-secret-key-that-is-long-enough-32bytes!!", 3600);
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, encoder, jwtService);
    }

    @Test
    void adminLoginGetsAdminRole() {
        when(userRepository.findById("1001"))
                .thenReturn(Optional.of(new User("1001", encoder.encode("Admin"), "Admin")));
        LoginResponse res = service.login(new LoginRequest("1001", "Admin"));
        assertThat(res.user().role()).isEqualTo("ROLE_ADMIN");
        assertThat(res.token()).isNotBlank();
    }

    @Test
    void regularUserGetsUserRole() {
        when(userRepository.findById("1003"))
                .thenReturn(Optional.of(new User("1003", encoder.encode("Rudy"), "Rudy")));
        LoginResponse res = service.login(new LoginRequest("1003", "Rudy"));
        assertThat(res.user().role()).isEqualTo("ROLE_USER");
        assertThat(res.user().approver()).isFalse();
    }

    @Test
    void approverFlagAndLevelAreReflected() {
        when(userRepository.findById("1002"))
                .thenReturn(Optional.of(new User("1002", encoder.encode("Leiva"), "Leiva",
                        "Y", 1, "rreyes@stand-insurance.com")));
        LoginResponse res = service.login(new LoginRequest("1002", "Leiva"));
        assertThat(res.user().approver()).isTrue();
        assertThat(res.user().approverLevel()).isEqualTo(1);
    }

    @Test
    void unknownUserRejected() {
        when(userRepository.findById("9999")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(new LoginRequest("9999", "x")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void wrongPasswordRejected() {
        when(userRepository.findById("1003"))
                .thenReturn(Optional.of(new User("1003", encoder.encode("Rudy"), "Rudy")));
        assertThatThrownBy(() -> service.login(new LoginRequest("1003", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
