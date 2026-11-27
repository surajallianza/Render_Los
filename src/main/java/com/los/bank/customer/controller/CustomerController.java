package com.los.bank.customer.controller;

import com.los.bank.customer.dto.*;
import com.los.bank.customer.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<CustomerApiResponse<CustomerProfileResponse>> createCustomer(
            @RequestBody CustomerSaveRequest request,
            Authentication authentication) {

        Long staffUserId = extractStaffUserId(authentication);
        CustomerProfileResponse response = customerService.createCustomer(request, staffUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerApiResponse.success("Customer draft created successfully", response));
    }

    @PutMapping("/{customerCode}")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<CustomerApiResponse<CustomerProfileResponse>> updateCustomer(
            @PathVariable("customerCode") String customerCode,
            @RequestBody CustomerSaveRequest request,
            Authentication authentication) {

        Long staffUserId = extractStaffUserId(authentication);
        CustomerProfileResponse response = customerService.updateCustomer(customerCode, request, staffUserId);
        return ResponseEntity.ok(CustomerApiResponse.success("Customer profile updated successfully", response));
    }

    @PostMapping("/{customerCode}/submit")
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE') or hasAuthority('CUSTOMER_UPDATE') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<CustomerApiResponse<CustomerProfileResponse>> submitCustomer(
            @PathVariable("customerCode") String customerCode,
            Authentication authentication) {

        Long staffUserId = extractStaffUserId(authentication);
        CustomerProfileResponse response = customerService.submitCustomer(customerCode, staffUserId);
        return ResponseEntity.ok(CustomerApiResponse.success("Customer onboarded successfully", response));
    }

    @DeleteMapping("/{customerCode}")
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE') or hasAuthority('CUSTOMER_DELETE') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<CustomerApiResponse<Void>> discardDraft(
            @PathVariable("customerCode") String customerCode,
            Authentication authentication) {

        Long staffUserId = extractStaffUserId(authentication);
        customerService.discardDraft(customerCode, staffUserId);
        return ResponseEntity.ok(CustomerApiResponse.success("Draft customer discarded successfully", null));
    }

    @GetMapping("/{customerCode}")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<CustomerApiResponse<CustomerProfileResponse>> getCustomerByCode(
            @PathVariable("customerCode") String customerCode) {

        CustomerProfileResponse response = customerService.getCustomerByCode(customerCode);
        return ResponseEntity.ok(CustomerApiResponse.success("Customer profile retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<CustomerApiResponse<CustomerPageResponse<CustomerListItemDto>>> getCustomers(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "customerType", required = false) String customerType,
            @RequestParam(name = "onboardingStatus", required = false) String onboardingStatus,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        CustomerPageResponse<CustomerListItemDto> response = customerService.getCustomers(search, status, customerType, onboardingStatus, page, size);
        return ResponseEntity.ok(CustomerApiResponse.success("Customer list retrieved successfully", response));
    }

    @PostMapping(value = "/{customerCode}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CUSTOMER_DOCUMENT_UPLOAD') or hasAuthority('CUSTOMER_UPDATE') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<CustomerApiResponse<CustomerDocumentResponse>> uploadDocument(
            @PathVariable("customerCode") String customerCode,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(name = "documentTypeCode", required = false) String documentTypeCode,
            @RequestParam(name = "expiryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam(name = "remarks", required = false) String remarks,
            HttpServletRequest request,
            Authentication authentication) {

        file = resolveMultipartFile(file, request);

        Long staffUserId = extractStaffUserId(authentication);
        CustomerDocumentResponse response = customerService.uploadDocument(customerCode, file, documentTypeCode, expiryDate, remarks, staffUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerApiResponse.success("Document uploaded successfully", response));
    }

    @PostMapping(value = "/{customerCode}/documents/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CUSTOMER_DOCUMENT_UPLOAD') or hasAuthority('CUSTOMER_UPDATE') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<CustomerApiResponse<CustomerDocumentResponse>> replaceDocument(
            @PathVariable("customerCode") String customerCode,
            @PathVariable("id") Long documentId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(name = "documentTypeCode", required = false) String documentTypeCode,
            @RequestParam(name = "expiryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam(name = "remarks", required = false) String remarks,
            HttpServletRequest request,
            Authentication authentication) {

        file = resolveMultipartFile(file, request);

        Long staffUserId = extractStaffUserId(authentication);
        CustomerDocumentResponse response = customerService.replaceDocument(customerCode, documentId, file, documentTypeCode, expiryDate, remarks, staffUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerApiResponse.success("Document replaced successfully", response));
    }

    @GetMapping("/{customerCode}/documents")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<CustomerApiResponse<List<CustomerDocumentResponse>>> getDocuments(
            @PathVariable("customerCode") String customerCode) {

        List<CustomerDocumentResponse> response = customerService.getCustomerDocuments(customerCode);
        return ResponseEntity.ok(CustomerApiResponse.success("Customer documents retrieved successfully", response));
    }

    @GetMapping("/{customerCode}/documents/{id}/view")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW') or hasAuthority('CUSTOMER_DOCUMENT_VIEW') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<byte[]> viewDocument(
            @PathVariable("customerCode") String customerCode,
            @PathVariable("id") Long documentId) {

        CustomerDocumentFile docFile = customerService.getDocumentFile(customerCode, documentId);
        MediaType mediaType = parseMediaType(docFile.contentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + docFile.fileName() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(docFile.data());
    }

    @GetMapping("/{customerCode}/documents/{id}/download")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW') or hasAuthority('CUSTOMER_DOCUMENT_VIEW') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable("customerCode") String customerCode,
            @PathVariable("id") Long documentId) {

        CustomerDocumentFile docFile = customerService.getDocumentFile(customerCode, documentId);
        MediaType mediaType = parseMediaType(docFile.contentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + docFile.fileName() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(docFile.data());
    }

    @DeleteMapping("/{customerCode}/documents/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_DOCUMENT_DELETE') or hasAuthority('CUSTOMER_DELETE') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<CustomerApiResponse<Void>> deleteDocument(
            @PathVariable("customerCode") String customerCode,
            @PathVariable("id") Long documentId,
            Authentication authentication) {

        Long staffUserId = extractStaffUserId(authentication);
        customerService.deleteDocument(customerCode, documentId, staffUserId);
        return ResponseEntity.ok(CustomerApiResponse.success("Document deleted successfully", null));
    }

    private MultipartFile resolveMultipartFile(MultipartFile file, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            if (request instanceof MultipartHttpServletRequest multipartRequest) {
                file = multipartRequest.getFile("file");
                if ((file == null || file.isEmpty()) && !multipartRequest.getFileMap().isEmpty()) {
                    file = multipartRequest.getFileMap().values().iterator().next();
                }
            }
        }

        if (file == null || file.isEmpty()) {
            String ct = request.getContentType();
            if (ct != null && ct.toLowerCase().contains("multipart/form-data") && !ct.toLowerCase().contains("boundary")) {
                throw new com.los.bank.customer.exception.DocumentValidationException(
                        "The Content-Type header 'multipart/form-data' is missing the multipart boundary delimiter. " +
                        "In Postman, go to the 'Headers' tab and UNCHECK any manual 'Content-Type' header so Postman can automatically generate the correct boundary.");
            }
            throw new com.los.bank.customer.exception.DocumentValidationException(
                    "Required multipart file 'file' is missing or empty. Please ensure: " +
                    "1) The 'Headers' tab does not have a manual 'Content-Type' header checked, " +
                    "2) In Postman Settings > General, 'Allow reading files outside working directory' is enabled, or " +
                    "3) Re-select the file in Postman.");
        }

        return file;
    }

    private Long extractStaffUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private MediaType parseMediaType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
