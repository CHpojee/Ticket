package com.standardinsurance.itsupport.exception;

import com.standardinsurance.itsupport.entity.TicketStatus;
import org.springframework.http.HttpStatus;

public class InvalidTransitionException extends ApiException {
    public InvalidTransitionException(TicketStatus from, String event) {
        super(HttpStatus.CONFLICT,
                "Illegal transition: cannot '" + event + "' a ticket in status " + from.getLabel());
    }
}
