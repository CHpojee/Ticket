package com.standardinsurance.itsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService service =
            new JwtService("test-secret-key-that-is-long-enough-32bytes!!", 3600);

    @Test
    void generatesAndParsesRoundTrip() {
        String token = service.generate("1001", "Admin", "ROLE_ADMIN", false);
        AuthenticatedUser user = service.parse(token);
        assertThat(user.userId()).isEqualTo("1001");
        assertThat(user.name()).isEqualTo("Admin");
        assertThat(user.role()).isEqualTo("ROLE_ADMIN");
        assertThat(user.isAdmin()).isTrue();
        assertThat(user.approver()).isFalse();
    }

    @Test
    void carriesApproverClaim() {
        AuthenticatedUser user = service.parse(service.generate("1002", "Leiva", "ROLE_USER", true));
        assertThat(user.approver()).isTrue();
    }

    @Test
    void rejectsTamperedToken() {
        String token = service.generate("1002", "Leiva", "ROLE_USER", false);
        assertThatThrownBy(() -> service.parse(token + "tampered"))
                .isInstanceOf(Exception.class);
    }
}
