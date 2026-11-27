package com.los.bank.product.exception;

/**
 * Exception thrown when a requested Loan Product is not found.
 */
public class LoanProductNotFoundException extends RuntimeException {

    public LoanProductNotFoundException(String message) {
        super(message);
    }
}
