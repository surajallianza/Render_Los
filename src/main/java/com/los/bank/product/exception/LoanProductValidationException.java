package com.los.bank.product.exception;

/**
 * Exception thrown when validation fails for Loan Product fields or business rules.
 */
public class LoanProductValidationException extends RuntimeException {

    public LoanProductValidationException(String message) {
        super(message);
    }
}
