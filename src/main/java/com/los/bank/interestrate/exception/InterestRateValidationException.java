package com.los.bank.interestrate.exception;

import lombok.Getter;

import java.util.Map;

/** 400: request data is invalid. {@link #getErrors()} is keyed by field, e.g. "tiers[0].normalRate". */
@Getter
public class InterestRateValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public InterestRateValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public InterestRateValidationException(String field, String message) {
        super(message);
        this.errors = Map.of(field, message);
    }
}
