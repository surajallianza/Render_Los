package com.los.bank.bankmaster.exception;

import com.los.bank.bankmaster.dto.BankMasterApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception handler for Bank Master module (Bank Accounts & Approval Limits).
 */
@RestControllerAdvice(basePackages = "com.los.bank.bankmaster")
public class BankMasterExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(BankMasterExceptionHandler.class);

    @ExceptionHandler(BankMasterException.class)
    public ResponseEntity<BankMasterApiResponse<Object>> handleBankMasterException(BankMasterException ex) {
        logger.error("Bank Master module error: {} (HTTP Status: {})", ex.getMessage(), ex.getStatus());
        return ResponseEntity.status(ex.getStatus())
                .body(BankMasterApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BankMasterApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        String firstError = errors.values().stream().findFirst().orElse("Validation failed.");
        logger.warn("Validation error in Bank Master: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BankMasterApiResponse<>(false, firstError, errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BankMasterApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Illegal argument error in Bank Master: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BankMasterApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BankMasterApiResponse<Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error in Bank Master: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BankMasterApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }
}
