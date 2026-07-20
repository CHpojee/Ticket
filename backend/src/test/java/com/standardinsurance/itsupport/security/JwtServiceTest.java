package com.standardinsurance.itsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService service =
            new JwtService("test-secret-key-that-is-long-enough-32bytes!!", 3600);

    @Test
    void generatesAndParsesRoundTrip() {
        String token = service.generate("1001", "Admin", "ROLE_ADMIN", false, null);
        AuthenticatedUser user = service.parse(token);
        assertThat(user.userId()).isEqualTo("1001");
        assertThat(user.name()).isEqualTo("Admin");
        assertThat(user.role()).isEqualTo("ROLE_ADMIN");
        assertThat(user.isAdmin()).isTrue();
        assertThat(user.approver()).isFalse();
        assertThat(user.approverLevel()).isNull();
    }

    @Test
    void carriesApproverClaimAndLevel() {
        AuthenticatedUser user = service.parse(
                service.generate("1002", "Leiva", "ROLE_USER", true, 1));
        assertThat(user.approver()).isTrue();
        assertThat(user.approverLevel()).isEqualTo(1);
    }

    @Test
    void rejectsTamperedToken() {
        String token = service.generate("1002", "Leiva", "ROLE_USER", false, null);
        assertThatThrownBy(() -> service.parse(token + "tampered"))
                .isInstanceOf(Exception.class);
    }
}
