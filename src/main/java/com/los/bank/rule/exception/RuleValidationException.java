package com.los.bank.rule.exception;

public class RuleValidationException extends RuntimeException {
    public RuleValidationException(String message) {
        super(message);
    }
}
