package com.standardinsurance.itsupport.security;

/** The principal stored in the SecurityContext for an authenticated request. */
public record AuthenticatedUser(String userId, String name, String role, boolean approver,
                                Integer approverLevel) {

    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(role);
    }
}
