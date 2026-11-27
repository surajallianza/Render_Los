package com.los.bank.loanEligibility.exception;

import java.util.Map;

/** 400: the eligibility request is incomplete or invalid. Errors are keyed by field. */
public class EligibilityValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public EligibilityValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
