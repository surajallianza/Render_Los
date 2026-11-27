package com.los.bank.product.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller advice handling exceptions specifically for Loan Product endpoints.
 */
@RestControllerAdvice(basePackages = "com.los.bank.product")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoanProductExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoanProductExceptionHandler.class);

    @ExceptionHandler(LoanProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(LoanProductNotFoundException ex) {
        logger.warn("Loan product not found: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(LoanProductValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(LoanProductValidationException ex) {
        logger.warn("Loan product validation failure: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Loan product invalid argument: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error in Loan Product module: ", ex);
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "An unexpected error occurred while processing the loan product request: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
