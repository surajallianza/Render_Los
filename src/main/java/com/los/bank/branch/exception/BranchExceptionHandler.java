package com.los.bank.branch.exception;

import com.los.bank.branch.dto.BranchApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler specifically handling errors originating from the Branch Management module.
 */
@RestControllerAdvice(basePackages = "com.los.bank.branch")
public class BranchExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(BranchExceptionHandler.class);

    @ExceptionHandler(BranchException.class)
    public ResponseEntity<BranchApiResponse<Object>> handleBranchException(BranchException ex) {
        logger.error("Branch management error: {} (HTTP Status: {})", ex.getMessage(), ex.getStatus());
        return ResponseEntity.status(ex.getStatus())
                .body(BranchApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BranchApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.error("Branch validation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BranchApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BranchApiResponse<Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error in branch module: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BranchApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }
}
