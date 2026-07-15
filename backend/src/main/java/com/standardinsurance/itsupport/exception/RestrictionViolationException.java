package com.standardinsurance.itsupport.exception;

import org.springframework.http.HttpStatus;

public class RestrictionViolationException extends ApiException {
    public RestrictionViolationException(String userId, String categoryCode) {
        super(HttpStatus.FORBIDDEN,
                "User " + userId + " is restricted from category " + categoryCode);
    }
}
