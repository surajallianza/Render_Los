package com.los.bank.interestrate.exception;

import com.los.bank.interestrate.dto.InterestRateApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.los.bank.interestrate")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InterestRateExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(InterestRateExceptionHandler.class);

    @ExceptionHandler(InterestRateValidationException.class)
    public ResponseEntity<InterestRateApiResponse<Void>> handleValidation(InterestRateValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(InterestRateApiResponse.error(ex.getMessage(), ex.getErrors()));
    }

    @ExceptionHandler(InterestRateNotFoundException.class)
    public ResponseEntity<InterestRateApiResponse<Void>> handleNotFound(InterestRateNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(InterestRateApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler(InterestRateConflictException.class)
    public ResponseEntity<InterestRateApiResponse<Void>> handleConflict(InterestRateConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(InterestRateApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<InterestRateApiResponse<Void>> handleBadRequest(Exception ex) {
        String message = ex instanceof HttpMessageNotReadableException
                ? "Request body is not valid JSON or has a wrong value type (dates must be yyyy-MM-dd, amounts and rates must be numbers)"
                : ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(InterestRateApiResponse.error(message, null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<InterestRateApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(InterestRateApiResponse.error("You do not have permission for this action", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<InterestRateApiResponse<Void>> handleUnexpected(Exception ex) {
        logger.error("Unexpected error in Interest Rate Master module", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(InterestRateApiResponse.error("An unexpected error occurred while processing the interest rate request",
                        Map.of()));
    }
}
