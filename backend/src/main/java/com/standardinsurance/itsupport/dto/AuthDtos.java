package com.standardinsurance.itsupport.dto;

import jakarta.validation.constraints.NotBlank;

/** Authentication request/response payloads. See docs/specs/01-user-login.md. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "is required") String userId,
            @NotBlank(message = "is required") String password) {
    }

    public record LoginResponse(String token, UserDto user) {
    }

    public record UserDto(String userId, String name, String role, boolean approver,
                          Integer approverLevel) {
    }
}
