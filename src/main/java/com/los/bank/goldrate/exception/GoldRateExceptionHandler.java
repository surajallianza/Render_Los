package com.los.bank.goldrate.exception;

import com.los.bank.goldrate.dto.GoldRateApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST controller advice for centralized exception handling in Gold Rate Master module.
 */
@RestControllerAdvice(basePackages = "com.los.bank.goldrate")
public class GoldRateExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoldRateExceptionHandler.class);

    @ExceptionHandler(GoldRateException.class)
    public ResponseEntity<GoldRateApiResponse<Void>> handleGoldRateException(GoldRateException ex) {
        logger.warn("GoldRateException: {}", ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(GoldRateApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GoldRateApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GoldRateApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GoldRateApiResponse<Void>> handleGeneralException(Exception ex) {
        logger.error("Unhandled error in Gold Rate Master: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GoldRateApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }
}
