package com.los.bank.interestrate.exception;

/** 409: the action is not allowed in the sheet's current state (duplicate date, already in effect, maker approving own sheet...). */
public class InterestRateConflictException extends RuntimeException {
    public InterestRateConflictException(String message) {
        super(message);
    }
}
