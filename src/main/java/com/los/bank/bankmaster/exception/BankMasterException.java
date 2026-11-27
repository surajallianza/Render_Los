package com.los.bank.bankmaster.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom runtime exception for Bank Master module operations.
 */
public class BankMasterException extends RuntimeException {

    private final HttpStatus status;

    public BankMasterException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public BankMasterException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
