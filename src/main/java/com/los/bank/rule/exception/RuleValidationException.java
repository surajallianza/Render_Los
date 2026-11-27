package com.los.bank.rule.exception;

import java.util.Map;

/**
 * 400: invalid rule. {@link #getErrors()} is keyed by field when available.
 */
public class RuleValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public RuleValidationException(String message) {
        super(message);
        this.errors = Map.of();
    }

    public RuleValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors == null ? Map.of() : errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
