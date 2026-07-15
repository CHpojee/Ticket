package com.standardinsurance.itsupport.security;

import com.standardinsurance.itsupport.exception.ForbiddenActionException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Convenience accessor for the authenticated principal in the current request. */
@Component
public class CurrentUser {

    public AuthenticatedUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ForbiddenActionException("No authenticated user in context");
        }
        return user;
    }

    public String userId() {
        return get().userId();
    }
}
