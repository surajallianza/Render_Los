package com.los.bank.user.exception;

public class UserDuplicateException extends RuntimeException {
    public UserDuplicateException(String message) {
        super(message);
    }
}
