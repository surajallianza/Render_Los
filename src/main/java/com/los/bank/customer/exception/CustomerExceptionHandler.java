package com.los.bank.customer.exception;

import com.los.bank.customer.dto.CustomerApiResponse;
import com.los.bank.lookup.exception.LookupBadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.los.bank.customer")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomerExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomerExceptionHandler.class);

    @ExceptionHandler(CustomerValidationException.class)
    public ResponseEntity<CustomerApiResponse<Void>> handleCustomerValidation(CustomerValidationException ex) {
        logger.warn("Customer validation failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomerApiResponse.error(ex.getMessage(), ex.getErrors()));
    }

    @ExceptionHandler(CustomerConflictException.class)
    public ResponseEntity<CustomerApiResponse<Void>> handleCustomerConflict(CustomerConflictException ex) {
        logger.warn("Customer conflict failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(CustomerApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<CustomerApiResponse<Void>> handleCustomerNotFound(CustomerNotFoundException ex) {
        logger.warn("Customer not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CustomerApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<CustomerApiResponse<Void>> handleDocumentNotFound(DocumentNotFoundException ex) {
        logger.warn("Document not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CustomerApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DocumentValidationException.class)
    public ResponseEntity<CustomerApiResponse<Void>> handleDocumentValidation(DocumentValidationException ex) {
        logger.warn("Document validation failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomerApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DocumentStorageException.class)
    public ResponseEntity<CustomerApiResponse<Void>> handleDocumentStorage(DocumentStorageException ex) {
        logger.error("Document storage failure: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomerApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(LookupBadRequestException.class)
    public ResponseEntity<CustomerApiResponse<Void>> handleLookupBadRequest(LookupBadRequestException ex) {
        logger.warn("Lookup bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomerApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomerApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomerApiResponse.error("Validation failed", fieldErrors));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<CustomerApiResponse<Void>> handleMissingServletRequestPart(MissingServletRequestPartException ex) {
        logger.warn("Missing multipart request part: {}", ex.getRequestPartName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomerApiResponse.error("Required multipart file '" + ex.getRequestPartName() + "' is missing. Please select and attach a file."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<CustomerApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        logger.warn("Max upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomerApiResponse.error("File size exceeds maximum allowed limit. Maximum allowed size is 10MB."));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<CustomerApiResponse<Void>> handleMultipartException(MultipartException ex) {
        logger.warn("Multipart request error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomerApiResponse.error("Multipart request error: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomerApiResponse<Void>> handleGenericException(Exception ex) {
        logger.error("Unexpected error in customer module: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomerApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }
}
