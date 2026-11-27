package com.los.bank.loan.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler scoped strictly to the Retail Loan Application Module.
 */
@RestControllerAdvice(basePackages = "com.los.bank.loan")
public class RetailLoanExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RetailLoanExceptionHandler.class);

    @ExceptionHandler(RetailLoanNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RetailLoanNotFoundException ex) {
        logger.warn("Retail loan application not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RetailLoanValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(RetailLoanValidationException ex) {
        logger.warn("Retail loan application validation failure: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(HttpMessageNotReadableException ex) {
        logger.warn("Malformed retail loan application JSON payload: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid JSON payload format: " + ex.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralError(Exception ex) {
        logger.error("Unexpected error in Retail Loan Application Module: ", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return new ResponseEntity<>(body, status);
    }
}
