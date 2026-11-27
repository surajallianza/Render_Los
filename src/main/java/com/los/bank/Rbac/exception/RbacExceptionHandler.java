package com.los.bank.Rbac.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.los.bank.Rbac")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RbacExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RbacExceptionHandler.class);

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRoleNotFound(RoleNotFoundException ex) {
        logger.warn("[RBAC] Role not found: {}", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", ex.getMessage());
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(RbacValidationException.class)
    public ResponseEntity<Map<String, Object>> handleRbacValidation(RbacValidationException ex) {
        logger.warn("[RBAC] Validation error: {}", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", ex.getMessage());
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("[RBAC] Illegal argument: {}", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", ex.getMessage());
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        FieldError error = ex.getBindingResult().getFieldError();
        String message = error != null ? error.getDefaultMessage() : "Invalid request payload";
        logger.warn("[RBAC] Method argument not valid: {}", message);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        logger.error("[RBAC] Data integrity violation: {}", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", "Database constraint violation occurred while updating role permissions.");
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        logger.error("[RBAC] Unexpected error occurred: ", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", "Failed to process role permissions: " + ex.getMessage());
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
