package com.los.bank.customer.service;

import com.los.bank.branch.entity.Branch;
import com.los.bank.branch.repository.BranchRepository;
import com.los.bank.customer.dto.*;
import com.los.bank.customer.entity.*;
import com.los.bank.customer.exception.*;
import com.los.bank.customer.repository.*;
import com.los.bank.lead.entity.Lead;
import com.los.bank.lead.repository.LeadRepository;
import com.los.bank.lookup.dto.LookupValue;
import com.los.bank.user.entity.UserSecuritySetting;
import com.los.bank.user.repository.UserSecuritySettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerContactRepository customerContactRepository;
    @Mock private CustomerPersonalRepository customerPersonalRepository;
    @Mock private CustomerIdentityRepository customerIdentityRepository;
    @Mock private CustomerFamilyRepository customerFamilyRepository;
    @Mock private CustomerAddressRepository customerAddressRepository;
    @Mock private CustomerEmploymentRepository customerEmploymentRepository;
    @Mock private CustomerPreviousEmploymentRepository customerPreviousEmploymentRepository;
    @Mock private CustomerIncomeRepository customerIncomeRepository;
    @Mock private CustomerBankAccountRepository customerBankAccountRepository;
    @Mock private CustomerTaxReturnRepository customerTaxReturnRepository;
    @Mock private CustomerDocumentRepository customerDocumentRepository;
    @Mock private CustomerManualLoanHistoryRepository customerManualLoanHistoryRepository;
    @Mock private CustomerReferenceRepository customerReferenceRepository;
    @Mock private CustomerProfileViewRepository customerProfileViewRepository;
    @Mock private LeadRepository leadRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private UserSecuritySettingRepository userSecuritySettingRepository;
    @Mock private CustomerLookupHelper lookupHelper;
    @Mock private CustomerValidator customerValidator;
    @Mock private StorageService storageService;

    @Spy
    private CustomerCryptoService cryptoService = new CustomerCryptoService(
            "TestSecretAES256KeyForUnitTesting2026!",
            "TestSecretHMAC256KeyForUnitTesting2026!"
    );

    @Spy
    private com.los.bank.customer.config.CustomerDocumentProperties documentProperties =
            new com.los.bank.customer.config.CustomerDocumentProperties();

    @InjectMocks
    private CustomerServiceImpl customerService;

    private CustomerSaveRequest validRequest;
    private Customer existingCustomer;
    private CustomerIdentity existingIdentity;

    @BeforeEach
    void setUp() {
        validRequest = CustomerSaveRequest.builder()
                .branchId(1L)
                .personal(PersonalRequest.builder()
                        .firstName("Amit")
                        .lastName("Sharma")
                        .mobile("9876543210")
                        .email("amit.sharma@example.com")
                        .dob(LocalDate.of(1990, 5, 15))
                        .build())
                .identity(IdentityRequest.builder()
                        .panNumber("ABCDE1234F")
                        .aadhaarNumber("123456789012")
                        .ckycNumber("12345678901234")
                        .build())
                .family(FamilyRequest.builder()
                        .fatherFirstName("Ramesh")
                        .fatherLastName("Sharma")
                        .motherFirstName("Sunita")
                        .motherLastName("Sharma")
                        .build())
                .address(AddressRequest.builder()
                        .flatBuildingName("Flat 101")
                        .streetRoad("Station Road")
                        .city("Pune")
                        .pincode("411001")
                        .build())
                .employment(EmploymentRequest.builder()
                        .employerName("TCS")
                        .designation("Lead")
                        .dateOfJoining(LocalDate.of(2020, 1, 1))
                        .build())
                .income(IncomeRequest.builder()
                        .annualIncome(new BigDecimal("1000000"))
                        .build())
                .bank(BankRequest.builder()
                        .bankName("HDFC")
                        .accountNumber("987654321012")
                        .build())
                .references(List.of(
                        ReferenceRequest.builder().srNo(1).referenceName("Ref One").relation("Friend").location("Pune").contactNo("9876543211").email("ref1@test.com").build(),
                        ReferenceRequest.builder().srNo(2).referenceName("Ref Two").relation("Brother").location("Pune").contactNo("9876543212").email("ref2@test.com").build()
                ))
                .build();

        existingCustomer = Customer.builder()
                .id(100L)
                .customerCode("CUS-2026-00001")
                .branchId(1L)
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        existingIdentity = CustomerIdentity.builder()
                .customerId(100L)
                .panNumber(cryptoService.encrypt("ABCDE1234F"))
                .panHash(cryptoService.computeHmacHex("ABCDE1234F"))
                .aadhaarNumber(cryptoService.encrypt("123456789012"))
                .aadhaarHash(cryptoService.computeHmacHex("123456789012"))
                .ckycNumber(cryptoService.encrypt("12345678901234"))
                .build();
    }

    @Test
    @DisplayName("Create customer successfully generates customer code and sets active with BCrypt password")
    void testCreateCustomerSuccess() {
        CustomerValidator.ResolvedLookups resolved = new CustomerValidator.ResolvedLookups();
        when(customerValidator.validateSections(any(), eq(false), any())).thenReturn(resolved);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(Branch.builder().id(1L).code("BR001").build()));
        when(customerRepository.getNextCustomerCodeSequence()).thenReturn(42L);

        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        // Mock profile lookup for response
        when(customerRepository.findByCustomerCode(anyString())).thenReturn(Optional.of(existingCustomer));
        when(customerContactRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerContact.builder().firstName("Amit").lastName("Sharma").phone("9876543210").email("amit@test.com").build()));
        when(customerPersonalRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerPersonal.builder().dob(LocalDate.of(1990, 1, 1)).build()));
        when(customerIdentityRepository.findByCustomerId(100L)).thenReturn(Optional.of(existingIdentity));

        CustomerProfileResponse response = customerService.createCustomer(validRequest, 5L);

        assertNotNull(response);
        verify(customerRepository).getNextCustomerCodeSequence();
        verify(customerRepository).save(argThat(c ->
                c.getCustomerCode().equals(String.format("CUS-%d-%05d", Year.now().getValue(), 42)) &&
                        Boolean.TRUE.equals(c.getIsActive()) &&
                        CustomerOnboardingStatus.DRAFT.equals(c.getOnboardingStatus()) &&
                        c.getPasswordHash() != null &&
                        c.getPasswordHash().startsWith("$2a$")
        ));
    }

    @Test
    @DisplayName("Create customer links lead when leadId is present")
    void testCreateCustomerLinksLead() {
        validRequest.setLeadId("LEAD100");
        CustomerValidator.ResolvedLookups resolved = new CustomerValidator.ResolvedLookups();
        when(customerValidator.validateSections(any(), eq(false), any())).thenReturn(resolved);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(Branch.builder().id(1L).build()));
        when(customerRepository.getNextCustomerCodeSequence()).thenReturn(1L);

        Lead lead = Lead.builder().leadId("LEAD100").build();
        when(leadRepository.findById("LEAD100")).thenReturn(Optional.of(lead));
        when(customerPersonalRepository.existsByLeadId("LEAD100")).thenReturn(false);

        when(customerRepository.save(any())).thenReturn(existingCustomer);
        when(customerRepository.findByCustomerCode(anyString())).thenReturn(Optional.of(existingCustomer));

        customerService.createCustomer(validRequest, 5L);

        assertEquals(100L, lead.getCustomerId());
        verify(leadRepository).save(lead);
    }

    @Test
    @DisplayName("Create customer throws 409 Conflict when PAN is duplicate")
    void testCreateCustomerDuplicatePan() {
        CustomerValidator.ResolvedLookups resolved = new CustomerValidator.ResolvedLookups();
        when(customerValidator.validateSections(any(), eq(false), any())).thenReturn(resolved);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(Branch.builder().id(1L).build()));
        when(customerIdentityRepository.existsByPanHash(anyString())).thenReturn(true);

        CustomerConflictException ex = assertThrows(CustomerConflictException.class, () ->
                customerService.createCustomer(validRequest, 5L)
        );
        assertTrue(ex.getMessage().contains("Duplicate customer PAN"));
    }

    @Test
    @DisplayName("Create customer throws 409 Conflict when Aadhaar is duplicate")
    void testCreateCustomerDuplicateAadhaar() {
        CustomerValidator.ResolvedLookups resolved = new CustomerValidator.ResolvedLookups();
        when(customerValidator.validateSections(any(), eq(false), any())).thenReturn(resolved);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(Branch.builder().id(1L).build()));
        when(customerIdentityRepository.existsByPanHash(anyString())).thenReturn(false);
        when(customerIdentityRepository.existsByAadhaarHash(anyString())).thenReturn(true);

        CustomerConflictException ex = assertThrows(CustomerConflictException.class, () ->
                customerService.createCustomer(validRequest, 5L)
        );
        assertTrue(ex.getMessage().contains("Duplicate customer Aadhaar"));
    }

    @Test
    @DisplayName("Create customer throws 409 Conflict when Mobile or Email is duplicate")
    void testCreateCustomerDuplicateContact() {
        CustomerValidator.ResolvedLookups resolved = new CustomerValidator.ResolvedLookups();
        when(customerValidator.validateSections(any(), eq(false), any())).thenReturn(resolved);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(Branch.builder().id(1L).build()));
        when(customerContactRepository.existsByPhone(anyString())).thenReturn(true);

        CustomerConflictException ex = assertThrows(CustomerConflictException.class, () ->
                customerService.createCustomer(validRequest, 5L)
        );
        assertTrue(ex.getMessage().contains("Duplicate customer mobile number"));
    }

    @Test
    @DisplayName("Update customer retains stored encrypted values when masked strings are submitted")
    void testUpdateCustomerMaskedRoundTrip() {
        validRequest.getIdentity().setPanNumber("ABCDE****F");
        validRequest.getIdentity().setAadhaarNumber("********9012");
        validRequest.getIdentity().setCkycNumber("**********1234");
        validRequest.getBank().setAccountNumber("******9012");

        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
        when(customerValidator.validateSections(any(), eq(true), any())).thenReturn(new CustomerValidator.ResolvedLookups());
        when(customerIdentityRepository.findByCustomerId(100L)).thenReturn(Optional.of(existingIdentity));
        when(customerContactRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerContact.builder().customerId(100L).build()));
        when(customerPersonalRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerPersonal.builder().customerId(100L).build()));
        when(customerFamilyRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerFamily.builder().customerId(100L).build()));
        when(customerAddressRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerAddress.builder().customerId(100L).build()));
        when(customerEmploymentRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerEmployment.builder().customerId(100L).build()));
        when(customerIncomeRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerIncome.builder().customerId(100L).build()));
        when(customerBankAccountRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerBankAccount.builder().customerId(100L).build()));

        byte[] originalPanEncrypted = existingIdentity.getPanNumber();

        customerService.updateCustomer("CUS-2026-00001", validRequest, 10L);

        // Verify that existing identity retained original encrypted bytes
        assertArrayEquals(originalPanEncrypted, existingIdentity.getPanNumber());
        // Verify c001.updated_at was touched
        verify(customerRepository).save(argThat(c -> c.getUpdatedAt() != null));
    }

    @Test
    @DisplayName("Create customer ignores client-sent validation status and saves UNVERIFIED")
    void testCreateCustomerIgnoresClientValidationStatus() {
        validRequest.getIdentity().setPanValidationStatus("VERIFIED_BY_CLIENT");
        validRequest.getIdentity().setAadhaarValidationStatus("VERIFIED_BY_CLIENT");

        CustomerValidator.ResolvedLookups resolved = new CustomerValidator.ResolvedLookups();
        when(customerValidator.validateSections(any(), eq(false), any())).thenReturn(resolved);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(Branch.builder().id(1L).build()));
        when(customerRepository.getNextCustomerCodeSequence()).thenReturn(1L);
        when(customerRepository.save(any())).thenReturn(existingCustomer);
        when(customerRepository.findByCustomerCode(anyString())).thenReturn(Optional.of(existingCustomer));

        customerService.createCustomer(validRequest, 5L);

        verify(customerIdentityRepository).save(argThat(identity ->
                "UNVERIFIED".equals(identity.getPanValidationStatus()) &&
                        "UNVERIFIED".equals(identity.getAadhaarValidationStatus())
        ));
    }

    @Test
    @DisplayName("Get customer includes source MANUAL in loan history")
    void testGetCustomerLoanHistoryHasManualSource() {
        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
        when(customerManualLoanHistoryRepository.findByCustomerId(100L)).thenReturn(List.of(
                CustomerManualLoanHistory.builder()
                        .id(1L)
                        .bankName("SBI")
                        .accountNumber("12345")
                        .loanType("Home Loan")
                        .totalOutstanding(new BigDecimal("100000"))
                        .emi(new BigDecimal("10000"))
                        .expiryDate(LocalDate.now().plusYears(5))
                        .build()
        ));

        CustomerProfileResponse response = customerService.getCustomerByCode("CUS-2026-00001");

        assertNotNull(response);
        assertNotNull(response.getLoanHistory());
        assertEquals(1, response.getLoanHistory().size());
        assertEquals("MANUAL", response.getLoanHistory().get(0).getSource());
    }

    @Test
    @DisplayName("Upload document prefixes tenant to storage folder and response excludes storagePath")
    void testUploadDocumentTenantFolder() {
        com.los.bank.user.context.BankContext.setCurrentBankCode("BANK01");
        try {
            when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
            when(customerPersonalRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerPersonal.builder().customerId(100L).build()));
            when(storageService.storeFile(eq("BANK01/CUS-2026-00001"), eq("test.pdf"), any(), any()))
                    .thenReturn("BANK01/CUS-2026-00001/uuid_test.pdf");

            CustomerDocument savedDoc = CustomerDocument.builder()
                    .id(1L)
                    .customerId(100L)
                    .originalFileName("test.pdf")
                    .contentType("application/pdf")
                    .fileSizeBytes(100L)
                    .storagePath("BANK01/CUS-2026-00001/uuid_test.pdf")
                    .uploadedAt(LocalDateTime.now())
                    .build();
            when(customerDocumentRepository.save(any(CustomerDocument.class))).thenReturn(savedDoc);

            MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());
            CustomerDocumentResponse resp = customerService.uploadDocument("CUS-2026-00001", file, null, null, null, 1L);

            assertNotNull(resp);
            assertEquals("test.pdf", resp.getOriginalFileName());
            // CustomerDocumentResponse has no getStoragePath() method, proving it's omitted
            verify(storageService).storeFile(eq("BANK01/CUS-2026-00001"), eq("test.pdf"), any(), any());
        } finally {
            com.los.bank.user.context.BankContext.clear();
        }
    }

    @Test
    @DisplayName("Soft delete document sets is_deleted to true and records deleted_by and deleted_at")
    void testSoftDeleteDocument() {
        CustomerDocument doc = CustomerDocument.builder()
                .id(50L)
                .customerId(100L)
                .isDeleted(false)
                .build();

        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
        when(customerDocumentRepository.findByIdAndCustomerIdAndIsDeletedFalse(50L, 100L)).thenReturn(Optional.of(doc));

        customerService.deleteDocument("CUS-2026-00001", 50L, 77L);

        assertTrue(doc.getIsDeleted());
        assertEquals(77L, doc.getDeletedBy());
        assertNotNull(doc.getDeletedAt());
        verify(customerDocumentRepository).save(doc);
    }

    // ---------------------------------------------------------------------
    // Draft onboarding flow
    // ---------------------------------------------------------------------

    private CustomerSaveRequest addressOnlyRequest() {
        return CustomerSaveRequest.builder()
                .address(AddressRequest.builder()
                        .flatBuildingName("Flat 202")
                        .streetRoad("MG Road")
                        .city("Mumbai")
                        .pincode("400001")
                        .build())
                .build();
    }

    private void stubCompleteDraft() {
        when(customerContactRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerContact.builder().customerId(100L).build()));
        when(customerPersonalRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerPersonal.builder().customerId(100L).build()));
        when(customerIdentityRepository.findByCustomerId(100L)).thenReturn(Optional.of(existingIdentity));
        when(customerFamilyRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerFamily.builder().customerId(100L).build()));
        when(customerAddressRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerAddress.builder().customerId(100L).build()));
        when(customerEmploymentRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerEmployment.builder().customerId(100L).build()));
        when(customerIncomeRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerIncome.builder().customerId(100L).build()));
        when(customerBankAccountRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerBankAccount.builder().customerId(100L).build()));
        when(customerReferenceRepository.findByCustomerIdOrderBySrNoAsc(100L)).thenReturn(List.of(
                CustomerReference.builder().srNo(1).build(), CustomerReference.builder().srNo(2).build()));
        when(customerDocumentRepository.findByCustomerIdAndIsDeletedFalseOrderByUploadedAtDesc(100L)).thenReturn(List.of(
                CustomerDocument.builder().id(1L).customerId(100L).isDeleted(false).build()));
    }

    @Test
    @DisplayName("Create (step 1) requires personal, identity and family details")
    void testCreateRequiresStepOneSections() {
        validRequest.setFamily(null);

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                customerService.createCustomer(validRequest, 5L));

        assertTrue(ex.getErrors().containsKey("family"));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update saves only the sections sent (address step) and leaves the rest untouched")
    void testUpdateSavesOnlySentSections() {
        existingCustomer.setOnboardingStatus(CustomerOnboardingStatus.DRAFT);
        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
        when(customerValidator.validateSections(any(), eq(true), any())).thenReturn(new CustomerValidator.ResolvedLookups());

        customerService.updateCustomer("CUS-2026-00001", addressOnlyRequest(), 10L);

        verify(customerAddressRepository).save(argThat(a -> "Mumbai".equals(a.getCity())));
        verify(customerContactRepository, never()).save(any());
        verify(customerIdentityRepository, never()).save(any());
        verify(customerEmploymentRepository, never()).save(any());
        verify(customerReferenceRepository, never()).deleteByCustomerId(anyLong());
        verify(customerPreviousEmploymentRepository, never()).deleteByCustomerId(anyLong());
    }

    @Test
    @DisplayName("Re-saving step 1 updates the same customer and ignores its own PAN in the duplicate check")
    void testResaveStepOneUpdatesSameCustomer() {
        existingCustomer.setOnboardingStatus(CustomerOnboardingStatus.DRAFT);
        validRequest.setAddress(null);
        validRequest.setEmployment(null);
        validRequest.setIncome(null);
        validRequest.setBank(null);
        validRequest.setReferences(null);
        validRequest.getPersonal().setFirstName("Amitabh");

        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
        when(customerValidator.validateSections(any(), eq(true), any())).thenReturn(new CustomerValidator.ResolvedLookups());
        when(customerIdentityRepository.findByCustomerId(100L)).thenReturn(Optional.of(existingIdentity));

        customerService.updateCustomer("CUS-2026-00001", validRequest, 10L);

        verify(customerIdentityRepository).existsByPanHashAndCustomerIdNot(anyString(), eq(100L));
        verify(customerContactRepository).save(argThat(c -> "Amitabh".equals(c.getFirstName())));
        verify(customerRepository, never()).getNextCustomerCodeSequence();
    }

    @Test
    @DisplayName("Changing employment to self employed removes saved previous employment")
    void testSelfEmployedRemovesPreviousEmployment() {
        CustomerValidator.ResolvedLookups resolved = new CustomerValidator.ResolvedLookups();
        resolved.employmentTypeLookupId = 7L;
        CustomerSaveRequest request = CustomerSaveRequest.builder()
                .employment(EmploymentRequest.builder()
                        .employerName("Own Shop").designation("Retail").dateOfJoining(LocalDate.of(2018, 1, 1)).build())
                .build();

        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
        when(customerValidator.validateSections(any(), eq(true), any())).thenReturn(resolved);
        when(lookupHelper.descriptionMatches(7L, "Self Employed")).thenReturn(true);

        customerService.updateCustomer("CUS-2026-00001", request, 10L);

        verify(customerPreviousEmploymentRepository).deleteByCustomerId(100L);
    }

    @Test
    @DisplayName("Submit lists every missing step and keeps the customer as DRAFT")
    void testSubmitIncompleteDraft() {
        existingCustomer.setOnboardingStatus(CustomerOnboardingStatus.DRAFT);
        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
        when(customerPersonalRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerPersonal.builder().customerId(100L).build()));
        when(customerContactRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerContact.builder().customerId(100L).build()));
        when(customerIdentityRepository.findByCustomerId(100L)).thenReturn(Optional.of(existingIdentity));
        when(customerFamilyRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerFamily.builder().customerId(100L).build()));

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                customerService.submitCustomer("CUS-2026-00001", 10L));

        assertTrue(ex.getErrors().containsKey("address"));
        assertTrue(ex.getErrors().containsKey("employment"));
        assertTrue(ex.getErrors().containsKey("income"));
        assertTrue(ex.getErrors().containsKey("bank"));
        assertTrue(ex.getErrors().containsKey("references"));
        assertTrue(ex.getErrors().containsKey("documents"));
        assertFalse(ex.getErrors().containsKey("personal"));
        assertEquals(CustomerOnboardingStatus.DRAFT, existingCustomer.getOnboardingStatus());
    }

    @Test
    @DisplayName("Submit of a complete draft marks it COMPLETED with submitter and time")
    void testSubmitCompleteDraft() {
        existingCustomer.setOnboardingStatus(CustomerOnboardingStatus.DRAFT);
        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
        stubCompleteDraft();

        CustomerProfileResponse response = customerService.submitCustomer("CUS-2026-00001", 10L);

        assertEquals(CustomerOnboardingStatus.COMPLETED, existingCustomer.getOnboardingStatus());
        assertEquals(10L, existingCustomer.getSubmittedBy());
        assertNotNull(existingCustomer.getSubmittedAt());
        assertEquals(CustomerOnboardingStatus.COMPLETED, response.getOnboardingStatus());
    }

    @Test
    @DisplayName("Submit checks configured required document types")
    void testSubmitRequiresConfiguredDocumentType() {
        existingCustomer.setOnboardingStatus(CustomerOnboardingStatus.DRAFT);
        documentProperties.setRequiredTypes(List.of("PAN"));
        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
        stubCompleteDraft();
        when(lookupHelper.getProperties()).thenReturn(new com.los.bank.customer.config.CustomerLookupProperties());
        when(lookupHelper.resolveId(eq("Document Type"), any(), eq("PAN"), anyString(), anyMap())).thenReturn(30L);
        when(lookupHelper.resolveLookupValue(30L)).thenReturn(LookupValue.of("PAN", "PAN Card"));

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                customerService.submitCustomer("CUS-2026-00001", 10L));

        assertEquals("PAN Card document is required", ex.getErrors().get("documents.PAN"));
    }

    @Test
    @DisplayName("Submit twice is rejected with 409 Conflict")
    void testSubmitAlreadyCompleted() {
        existingCustomer.setOnboardingStatus(CustomerOnboardingStatus.COMPLETED);
        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));

        assertThrows(CustomerConflictException.class, () -> customerService.submitCustomer("CUS-2026-00001", 10L));
    }

    @Test
    @DisplayName("Uploading a single-file document type again replaces the previous file")
    void testUploadSameTypeReplacesPrevious() {
        CustomerDocument oldDoc = CustomerDocument.builder().id(11L).customerId(100L).documentTypeLookupId(30L).isDeleted(false).build();
        CustomerDocument newDoc = CustomerDocument.builder().id(12L).customerId(100L).documentTypeLookupId(30L).isDeleted(false)
                .originalFileName("aadhaar.pdf").uploadedAt(LocalDateTime.now()).build();

        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
        when(lookupHelper.getProperties()).thenReturn(new com.los.bank.customer.config.CustomerLookupProperties());
        when(lookupHelper.resolveId(eq("Document Type"), any(), eq("AADHAAR"), anyString(), anyMap())).thenReturn(30L);
        when(storageService.storeFile(anyString(), anyString(), any(), any())).thenReturn("default/CUS-2026-00001/x.pdf");
        when(customerDocumentRepository.save(any(CustomerDocument.class))).thenAnswer(inv -> {
            CustomerDocument d = inv.getArgument(0);
            return d.getId() == null ? newDoc : d;
        });
        when(customerDocumentRepository.findByCustomerIdAndDocumentTypeLookupIdAndIsDeletedFalse(100L, 30L))
                .thenReturn(List.of(oldDoc, newDoc));

        MockMultipartFile file = new MockMultipartFile("file", "aadhaar.pdf", "application/pdf", "pdf".getBytes());
        CustomerDocumentResponse resp = customerService.uploadDocument("CUS-2026-00001", file, "AADHAAR", null, null, 9L);

        assertEquals(12L, resp.getId());
        assertEquals(List.of(11L), resp.getReplacedDocumentIds());
        assertTrue(oldDoc.getIsDeleted());
        assertEquals(9L, oldDoc.getDeletedBy());
        assertFalse(newDoc.getIsDeleted());
    }

    @Test
    @DisplayName("Uploading a multiple-allowed document type keeps the previous files")
    void testUploadMultipleAllowedTypeKeepsPrevious() {
        documentProperties.setMultipleAllowedTypes(List.of("SALARY_SLIP"));
        CustomerDocument newDoc = CustomerDocument.builder().id(12L).customerId(100L).documentTypeLookupId(31L).isDeleted(false).build();

        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
        when(lookupHelper.getProperties()).thenReturn(new com.los.bank.customer.config.CustomerLookupProperties());
        when(lookupHelper.resolveId(eq("Document Type"), any(), eq("SALARY_SLIP"), anyString(), anyMap())).thenReturn(31L);
        when(lookupHelper.resolveLookupValue(31L)).thenReturn(LookupValue.of("SALARY_SLIP", "Salary Slip"));
        when(storageService.storeFile(anyString(), anyString(), any(), any())).thenReturn("default/CUS-2026-00001/x.pdf");
        when(customerDocumentRepository.save(any(CustomerDocument.class))).thenReturn(newDoc);

        MockMultipartFile file = new MockMultipartFile("file", "slip.pdf", "application/pdf", "pdf".getBytes());
        CustomerDocumentResponse resp = customerService.uploadDocument("CUS-2026-00001", file, "SALARY_SLIP", null, null, 9L);

        assertTrue(resp.getReplacedDocumentIds().isEmpty());
        verify(customerDocumentRepository, never()).findByCustomerIdAndDocumentTypeLookupIdAndIsDeletedFalse(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Replace document stores the new file, soft deletes the old one and keeps its type and remarks")
    void testReplaceDocument() {
        CustomerDocument oldDoc = CustomerDocument.builder().id(11L).customerId(100L).documentTypeLookupId(30L)
                .remarks("front side").isDeleted(false).build();

        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
        when(customerDocumentRepository.findByIdAndCustomerIdAndIsDeletedFalse(11L, 100L)).thenReturn(Optional.of(oldDoc));
        when(storageService.storeFile(anyString(), anyString(), any(), any())).thenReturn("default/CUS-2026-00001/y.pdf");
        when(customerDocumentRepository.save(any(CustomerDocument.class))).thenAnswer(inv -> {
            CustomerDocument d = inv.getArgument(0);
            if (d.getId() == null) {
                d.setId(12L);
            }
            return d;
        });

        MockMultipartFile file = new MockMultipartFile("file", "correct.pdf", "application/pdf", "pdf".getBytes());
        CustomerDocumentResponse resp = customerService.replaceDocument("CUS-2026-00001", 11L, file, null, null, null, 9L);

        assertEquals(12L, resp.getId());
        assertEquals("front side", resp.getRemarks());
        assertEquals(List.of(11L), resp.getReplacedDocumentIds());
        assertTrue(oldDoc.getIsDeleted());
        verify(customerDocumentRepository).save(argThat(d -> Long.valueOf(30L).equals(d.getDocumentTypeLookupId())
                && "correct.pdf".equals(d.getOriginalFileName())));
    }

    @Test
    @DisplayName("Discard deletes a DRAFT customer, unlinks its lead and removes files after commit")
    void testDiscardDraft() {
        existingCustomer.setOnboardingStatus(CustomerOnboardingStatus.DRAFT);
        Lead lead = Lead.builder().leadId("LEAD100").customerId(100L).build();

        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));
        when(customerPersonalRepository.findByCustomerId(100L)).thenReturn(Optional.of(CustomerPersonal.builder().customerId(100L).leadId("LEAD100").build()));
        when(leadRepository.findById("LEAD100")).thenReturn(Optional.of(lead));
        when(customerDocumentRepository.findByCustomerId(100L)).thenReturn(List.of(
                CustomerDocument.builder().id(1L).storagePath("default/LEAD100/a.pdf").build()));

        customerService.discardDraft("CUS-2026-00001", 10L);

        assertNull(lead.getCustomerId());
        verify(leadRepository).save(lead);
        verify(customerRepository).delete(existingCustomer);
        // No transaction in a unit test, so the file is removed immediately
        verify(storageService).deleteFile("default/LEAD100/a.pdf");
    }

    @Test
    @DisplayName("Discard of a COMPLETED customer is rejected")
    void testDiscardCompletedRejected() {
        existingCustomer.setOnboardingStatus(CustomerOnboardingStatus.COMPLETED);
        when(customerRepository.findByCustomerCode("CUS-2026-00001")).thenReturn(Optional.of(existingCustomer));

        assertThrows(CustomerConflictException.class, () -> customerService.discardDraft("CUS-2026-00001", 10L));
        verify(customerRepository, never()).delete(any());
    }
}
