package com.los.bank.lookup.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for Lookup module and Bean Validation errors.
 * Preserves the project's standard response wrapper { success: false, message: ... }.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LookupExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(LookupExceptionHandler.class);

    @ExceptionHandler(LookupNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLookupNotFound(LookupNotFoundException ex) {
        logger.warn("Lookup not found: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(LookupConflictException.class)
    public ResponseEntity<Map<String, Object>> handleLookupConflict(LookupConflictException ex) {
        logger.warn("Lookup conflict: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(LookupBadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleLookupBadRequest(LookupBadRequestException ex) {
        logger.warn("Lookup bad request: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(LookupException.class)
    public ResponseEntity<Map<String, Object>> handleGenericLookupException(LookupException ex) {
        logger.error("Lookup error: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuilder errorMessage = new StringBuilder("Validation failed: ");
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errorMessage.append(error.getField())
                    .append(" ")
                    .append(error.getDefaultMessage())
                    .append("; ");
        }
        String msg = errorMessage.toString().trim();
        logger.warn("Validation error: {}", msg);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
