package com.los.bank.loanEligibility.exception;

import com.los.bank.loanEligibility.dto.EligibilityApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.los.bank.loanEligibility")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EligibilityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(EligibilityExceptionHandler.class);

    @ExceptionHandler(EligibilityValidationException.class)
    public ResponseEntity<EligibilityApiResponse<Void>> handleValidation(EligibilityValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(EligibilityApiResponse.error(ex.getMessage(), ex.getErrors()));
    }

    @ExceptionHandler(EligibilityNotFoundException.class)
    public ResponseEntity<EligibilityApiResponse<Void>> handleNotFound(EligibilityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(EligibilityApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<EligibilityApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(EligibilityApiResponse.error(
                "Request body is not valid JSON or has a wrong value type (amounts, rates and scores must be numbers)", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<EligibilityApiResponse<Void>> handleUnexpected(Exception ex) {
        logger.error("Unexpected error in Loan Eligibility module", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(EligibilityApiResponse.error("An unexpected error occurred while checking eligibility", null));
    }
}
