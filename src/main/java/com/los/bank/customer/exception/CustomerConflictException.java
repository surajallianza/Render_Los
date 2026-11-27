package com.los.bank.customer.exception;

public class CustomerConflictException extends RuntimeException {
    public CustomerConflictException(String message) {
        super(message);
    }
}
