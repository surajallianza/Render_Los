package com.los.bank.setup.exception;

public class BankSetupException extends RuntimeException {
    public BankSetupException(String message) {
        super(message);
    }

    public BankSetupException(String message, Throwable cause) {
        super(message, cause);
    }
}

