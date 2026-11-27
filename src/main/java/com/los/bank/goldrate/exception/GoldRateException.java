package com.los.bank.goldrate.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom runtime exception for Gold Rate Master module operations.
 */
public class GoldRateException extends RuntimeException {

    private final HttpStatus status;

    public GoldRateException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public GoldRateException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
