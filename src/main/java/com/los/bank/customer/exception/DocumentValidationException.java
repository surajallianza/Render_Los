package com.los.bank.customer.exception;

public class DocumentValidationException extends RuntimeException {
    public DocumentValidationException(String message) {
        super(message);
    }
}
