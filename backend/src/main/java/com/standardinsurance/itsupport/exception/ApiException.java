package com.standardinsurance.itsupport.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for domain exceptions that carry the HTTP status to return.
 * Mapped to {@link com.standardinsurance.itsupport.dto.ErrorResponse} by the
 * {@link GlobalExceptionHandler}.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
