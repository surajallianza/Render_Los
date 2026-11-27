package com.los.bank.loanEligibility.exception;

/** 404: eligibility check not found. */
public class EligibilityNotFoundException extends RuntimeException {
    public EligibilityNotFoundException(String message) {
        super(message);
    }
}
