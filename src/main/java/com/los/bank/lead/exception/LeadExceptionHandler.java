package com.los.bank.lead.exception;

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
 * Controller advice handling exceptions specifically for Lead Module endpoints.
 */
@RestControllerAdvice(basePackages = "com.los.bank.lead")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LeadExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LeadExceptionHandler.class);

    @ExceptionHandler(LeadNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLeadNotFoundException(LeadNotFoundException ex) {
        logger.warn("Lead not found: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(LeadValidationException.class)
    public ResponseEntity<Map<String, Object>> handleLeadValidationException(LeadValidationException ex) {
        logger.warn("Lead validation failure: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Lead argument validation failure: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error in Lead Module: ", ex);
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "An unexpected error occurred while processing the lead request: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
