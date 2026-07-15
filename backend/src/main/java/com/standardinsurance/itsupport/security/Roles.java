package com.standardinsurance.itsupport.security;

/** Role derivation. userId 1001 (Admin) is the only admin; everyone else is a user. */
public final class Roles {

    public static final String ADMIN = "ROLE_ADMIN";
    public static final String USER = "ROLE_USER";
    public static final String ADMIN_USER_ID = "1001";

    private Roles() {
    }

    public static String forUserId(String userId) {
        return ADMIN_USER_ID.equals(userId) ? ADMIN : USER;
    }
}
