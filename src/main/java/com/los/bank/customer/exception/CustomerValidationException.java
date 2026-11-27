package com.los.bank.customer.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class CustomerValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public CustomerValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public CustomerValidationException(String field, String message) {
        super(message);
        this.errors = Map.of(field, message);
    }
}
