package com.standardinsurance.itsupport.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** User maintenance payloads. See docs/specs/02-user-maintenance.md. */
public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank(message = "is required") String userId,
            @NotBlank(message = "is required") String name,
            @NotBlank(message = "is required") String password) {
    }

    public record UpdateUserRequest(String name, String password) {
    }

    public record RestrictionRequest(
            @NotBlank(message = "is required") String ticketCategoryCode) {
    }

    /** Full user view for admin screens; never includes the password. */
    public record UserDetail(String userId, String name, String role, List<String> restrictions) {
    }
}
