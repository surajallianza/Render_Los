package com.los.bank.customer.service;

import com.los.bank.customer.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface CustomerService {

    CustomerProfileResponse createCustomer(CustomerSaveRequest request, Long staffUserId);

    CustomerProfileResponse updateCustomer(String customerCode, CustomerSaveRequest request, Long staffUserId);

    CustomerProfileResponse getCustomerByCode(String customerCode);

    CustomerProfileResponse submitCustomer(String customerCode, Long staffUserId);

    void discardDraft(String customerCode, Long staffUserId);

    CustomerPageResponse<CustomerListItemDto> getCustomers(String search, String status, String customerType, String onboardingStatus, int page, int size);

    CustomerDocumentResponse uploadDocument(String customerCode, MultipartFile file, String documentTypeCode, LocalDate expiryDate, String remarks, Long staffUserId);

    CustomerDocumentResponse replaceDocument(String customerCode, Long documentId, MultipartFile file, String documentTypeCode, LocalDate expiryDate, String remarks, Long staffUserId);

    List<CustomerDocumentResponse> getCustomerDocuments(String customerCode);

    CustomerDocumentFile getDocumentFile(String customerCode, Long documentId);

    void deleteDocument(String customerCode, Long documentId, Long staffUserId);
}
