package com.los.bank.branch.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom runtime exception for branch management operations.
 */
public class BranchException extends RuntimeException {

    private final HttpStatus status;

    public BranchException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public BranchException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
