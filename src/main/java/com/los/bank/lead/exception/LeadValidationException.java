package com.los.bank.lead.exception;

public class LeadValidationException extends RuntimeException {

    public LeadValidationException(String message) {
        super(message);
    }

    public LeadValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
