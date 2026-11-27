package com.los.bank.lead.exception;

public class LeadNotFoundException extends RuntimeException {

    public LeadNotFoundException(String message) {
        super(message);
    }

    public LeadNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
