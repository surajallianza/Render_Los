package com.los.bank.interestrate.exception;

/** 404: rate sheet, product or applicable rate not found. */
public class InterestRateNotFoundException extends RuntimeException {
    public InterestRateNotFoundException(String message) {
        super(message);
    }
}
