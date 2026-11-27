package com.los.bank.customer;

import com.los.bank.customer.dto.*;
import com.los.bank.customer.entity.*;
import com.los.bank.customer.repository.*;
import com.los.bank.customer.service.CustomerService;
import com.los.bank.user.context.BankContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "app.migration.auto-run=false",
        "spring.flyway.enabled=false"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerPostgresIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private com.los.bank.customer.service.LocalStorageServiceImpl localStorageService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerContactRepository customerContactRepository;

    @Autowired
    private CustomerPersonalRepository customerPersonalRepository;

    @Autowired
    private CustomerIdentityRepository customerIdentityRepository;

    @Autowired
    private CustomerFamilyRepository customerFamilyRepository;

    @Autowired
    private CustomerAddressRepository customerAddressRepository;

    @Autowired
    private CustomerEmploymentRepository customerEmploymentRepository;

    @Autowired
    private CustomerPreviousEmploymentRepository customerPreviousEmploymentRepository;

    @Autowired
    private CustomerIncomeRepository customerIncomeRepository;

    @Autowired
    private CustomerBankAccountRepository customerBankAccountRepository;

    @Autowired
    private CustomerTaxReturnRepository customerTaxReturnRepository;

    @Autowired
    private CustomerManualLoanHistoryRepository customerManualLoanHistoryRepository;

    @Autowired
    private CustomerReferenceRepository customerReferenceRepository;

    @Autowired
    private CustomerDocumentRepository customerDocumentRepository;

    private static String createdCustomerCode;
    private static Long createdCustomerId;

    @BeforeEach
    void setUp() {
        BankContext.setCurrentDatabase("bank_abc001");
        BankContext.setCurrentBankCode("bank_abc001");
        if (createdCustomerId == null) {
            customerContactRepository.findByPhone("9876512345").ifPresent(c -> {
                cleanupCustomer(c.getCustomerId());
            });
        }
    }

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("bankDataSource")
    private javax.sql.DataSource bankDataSource;

    private void cleanupCustomer(Long id) {
        if (id == null) return;
        try (java.sql.Connection conn = bankDataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM c015 WHERE customer_id = " + id);
            stmt.executeUpdate("DELETE FROM c014 WHERE customer_id = " + id);
            stmt.executeUpdate("DELETE FROM c013 WHERE customer_id = " + id);
            stmt.executeUpdate("DELETE FROM c012 WHERE customer_id = " + id);
            stmt.executeUpdate("DELETE FROM c011 WHERE customer_id = " + id);
            stmt.executeUpdate("DELETE FROM c010 WHERE customer_id = " + id);
            stmt.executeUpdate("DELETE FROM c009 WHERE customer_id = " + id);
            stmt.executeUpdate("DELETE FROM c008 WHERE customer_id = " + id);
            stmt.executeUpdate("DELETE FROM c007 WHERE customer_id = " + id);
            stmt.executeUpdate("DELETE FROM c006 WHERE customer_id = " + id);
            stmt.executeUpdate("DELETE FROM c005 WHERE customer_id = " + id);
            stmt.executeUpdate("DELETE FROM c004 WHERE customer_id = " + id);
            stmt.executeUpdate("DELETE FROM c002 WHERE customer_id = " + id);
            stmt.executeUpdate("DELETE FROM c001 WHERE id = " + id);
        } catch (Exception e) {
            // ignore
        }
    }

    @AfterEach
    void tearDown() {
        BankContext.clear();
    }

    @Test
    @Order(1)
    @DisplayName("Postgres Integration: Create customer end-to-end against real PostgreSQL")
    void testCreateCustomerIntegration() {
        CustomerSaveRequest request = CustomerSaveRequest.builder()
                .branchId(1L)
                .personal(PersonalRequest.builder()
                        .title("1000801")
                        .firstName("Vikram")
                        .middleName("Kumar")
                        .lastName("Malhotra")
                        .dob(LocalDate.of(1992, 4, 10))
                        .gender("1000901")
                        .maritalStatus("1001101")
                        .customerType("1001201")
                        .kycStatus("1001301")
                        .occupation("Architect")
                        .mobile("9876512345")
                        .email("vikram.malhotra@testit.com")
                        .build())
                .identity(IdentityRequest.builder()
                        .panNumber("ABCDE9999Z")
                        .aadhaarNumber("999988887777")
                        .ckycNumber("99998888777766")
                        .panValidationStatus("CLIENT_ATTEMPTED_VERIFIED")
                        .aadhaarValidationStatus("CLIENT_ATTEMPTED_VERIFIED")
                        .build())
                .family(FamilyRequest.builder()
                        .fatherTitle("1000801")
                        .fatherFirstName("Sunil")
                        .fatherLastName("Malhotra")
                        .motherTitle("1000802")
                        .motherFirstName("Kiran")
                        .motherLastName("Malhotra")
                        .build())
                .address(AddressRequest.builder()
                        .residentialStatus("1001401")
                        .residentialType("1001501")
                        .flatBuildingName("Flat 402, Green Heights")
                        .streetRoad("Linking Road")
                        .landmark("Near Metro")
                        .city("Mumbai")
                        .state("1001601")
                        .pincode("400001")
                        .build())
                .employment(EmploymentRequest.builder()
                        .employmentType("1001701")
                        .employerType("1001801")
                        .designation("Senior Architect")
                        .employerName("Studio Mumbai")
                        .dateOfJoining(LocalDate.of(2019, 1, 15))
                        .qualification("B.Arch")
                        .employerEmail("vikram@studiomumbai.com")
                        .listedStatus("1001901")
                        .yearsOfService(new BigDecimal("5.0"))
                        .build())
                .previousEmployment(List.of(
                        PreviousEmploymentRequest.builder()
                                .employmentType("1001701")
                                .employerName("Design Inc")
                                .designation("Junior Architect")
                                .location("Pune")
                                .dateOfJoining(LocalDate.of(2016, 1, 1))
                                .dateOfRelieving(LocalDate.of(2018, 12, 31))
                                .build()
                ))
                .income(IncomeRequest.builder()
                        .annualIncome(new BigDecimal("1800000.00"))
                        .otherSourceIncome(new BigDecimal("50000.00"))
                        .totalGrossIncome(new BigDecimal("1850000.00"))
                        .totalNetIncome(new BigDecimal("1400000.00"))
                        .incomeTax(new BigDecimal("450000.00"))
                        .incomeTaxFilerStatus("1002001")
                        .riskCategory("1002101")
                        .build())
                .bank(BankRequest.builder()
                        .bankName("HDFC Bank")
                        .accountNumber("1234567890123")
                        .accountType("1002201")
                        .accountStatus("1002301")
                        .build())
                .taxReturns(List.of(
                        TaxReturnRequest.builder()
                                .financialYear("2024-25")
                                .grossIncome(new BigDecimal("1800000.00"))
                                .tdsPaid(new BigDecimal("150000.00"))
                                .build()
                ))
                .loanHistory(List.of(
                        LoanHistoryRequest.builder()
                                .bankName("State Bank of India")
                                .accountNumber("LOAN9876543")
                                .loanType("Personal Loan")
                                .totalOutstanding(new BigDecimal("100000.00"))
                                .emi(new BigDecimal("10000.00"))
                                .expiryDate(LocalDate.of(2023, 6, 30)) // past date allowed as per Rule 4
                                .build()
                ))
                .references(List.of(
                        ReferenceRequest.builder()
                                .srNo(1)
                                .referenceName("Amitabh Sen")
                                .relation("Friend")
                                .location("Mumbai")
                                .contactNo("9876512346")
                                .email("amitabh.sen@testit.com")
                                .build(),
                        ReferenceRequest.builder()
                                .srNo(2)
                                .referenceName("Pooja Nair")
                                .relation("Colleague")
                                .location("Mumbai")
                                .contactNo("9876512347")
                                .email("pooja.nair@testit.com")
                                .build()
                ))
                .build();

        CustomerProfileResponse response = customerService.createCustomer(request, 1L);

        assertNotNull(response);
        assertNotNull(response.getCustomerCode());
        assertTrue(response.getCustomerCode().startsWith("CUS-"));
        assertEquals("Vikram", response.getPersonal().getFirstName());
        assertEquals("Malhotra", response.getPersonal().getLastName());

        // Verify client-sent validation status was ignored and reset to UNVERIFIED
        assertEquals("UNVERIFIED", response.getIdentity().getPanValidationStatus());
        assertEquals("UNVERIFIED", response.getIdentity().getAadhaarValidationStatus());

        // Verify loan history has source MANUAL
        assertNotNull(response.getLoanHistory());
        assertFalse(response.getLoanHistory().isEmpty());
        assertEquals("MANUAL", response.getLoanHistory().get(0).getSource());

        // Store for subsequent test steps
        createdCustomerCode = response.getCustomerCode();
        createdCustomerId = response.getCustomerId();
    }

    @Test
    @Order(2)
    @DisplayName("Postgres Integration: Read back customer profile from PostgreSQL")
    void testReadCustomerIntegration() {
        assertNotNull(createdCustomerCode, "Customer should have been created in Order 1");

        CustomerProfileResponse profile = customerService.getCustomerByCode(createdCustomerCode);

        assertNotNull(profile);
        assertEquals(createdCustomerCode, profile.getCustomerCode());
        assertEquals("Vikram", profile.getPersonal().getFirstName());
        assertEquals("Mumbai", profile.getAddress().getCity());
        assertEquals("HDFC Bank", profile.getBank().getBankName());

        // Masked identity fields
        assertTrue(profile.getIdentity().getPanNumber().contains("*"));
        assertTrue(profile.getIdentity().getAadhaarNumber().contains("*"));
        assertTrue(profile.getBank().getAccountNumber().contains("*"));

        // Loan history source
        assertEquals("MANUAL", profile.getLoanHistory().get(0).getSource());
    }

    @Test
    @Order(3)
    @DisplayName("Postgres Integration: Update customer profile in PostgreSQL")
    void testUpdateCustomerIntegration() {
        assertNotNull(createdCustomerCode, "Customer should have been created in Order 1");

        CustomerProfileResponse existing = customerService.getCustomerByCode(createdCustomerCode);

        CustomerSaveRequest updateRequest = CustomerSaveRequest.builder()
                .personal(PersonalRequest.builder()
                        .title("1000801")
                        .firstName("Vikram")
                        .middleName("Kumar")
                        .lastName("Malhotra")
                        .dob(LocalDate.of(1992, 4, 10))
                        .gender("1000901")
                        .maritalStatus("1001101")
                        .customerType("1001201")
                        .kycStatus("1001301")
                        .occupation("Principal Architect") // updated
                        .mobile(existing.getPersonal().getMobile())
                        .email(existing.getPersonal().getEmail())
                        .build())
                .identity(IdentityRequest.builder()
                        .panNumber(existing.getIdentity().getPanNumber()) // masked round-trip
                        .aadhaarNumber(existing.getIdentity().getAadhaarNumber()) // masked round-trip
                        .ckycNumber(existing.getIdentity().getCkycNumber()) // masked round-trip
                        .panValidationStatus("SHOULD_BE_IGNORED")
                        .aadhaarValidationStatus("SHOULD_BE_IGNORED")
                        .build())
                .family(FamilyRequest.builder()
                        .fatherTitle("1000801")
                        .fatherFirstName("Sunil")
                        .fatherLastName("Malhotra")
                        .motherTitle("1000802")
                        .motherFirstName("Kiran")
                        .motherLastName("Malhotra")
                        .build())
                .address(AddressRequest.builder()
                        .residentialStatus("1001401")
                        .residentialType("1001501")
                        .flatBuildingName("Flat 501, Blue Horizon") // updated
                        .streetRoad("Marine Drive") // updated
                        .city("Mumbai")
                        .state("1001601")
                        .pincode("400020") // updated
                        .build())
                .employment(EmploymentRequest.builder()
                        .employmentType("1001701")
                        .employerType("1001801")
                        .designation("Principal Architect") // updated
                        .employerName("Studio Mumbai")
                        .dateOfJoining(LocalDate.of(2019, 1, 15))
                        .qualification("M.Arch") // updated
                        .listedStatus("1001901")
                        .yearsOfService(new BigDecimal("5.0"))
                        .build())
                .income(IncomeRequest.builder()
                        .annualIncome(new BigDecimal("2200000.00")) // updated
                        .incomeTaxFilerStatus("1002001")
                        .riskCategory("1002101")
                        .build())
                .bank(BankRequest.builder()
                        .bankName("HDFC Bank")
                        .accountNumber(existing.getBank().getAccountNumber()) // masked round-trip
                        .accountType("1002201")
                        .accountStatus("1002301")
                        .build())
                .taxReturns(List.of(
                        TaxReturnRequest.builder()
                                .financialYear("2024-25")
                                .grossIncome(new BigDecimal("2200000.00"))
                                .tdsPaid(new BigDecimal("200000.00"))
                                .build()
                ))
                .loanHistory(List.of(
                        LoanHistoryRequest.builder()
                                .bankName("State Bank of India")
                                .accountNumber("LOAN9876543")
                                .loanType("Personal Loan")
                                .totalOutstanding(new BigDecimal("50000.00"))
                                .emi(new BigDecimal("10000.00"))
                                .expiryDate(LocalDate.of(2023, 6, 30))
                                .build()
                ))
                .references(List.of(
                        ReferenceRequest.builder()
                                .srNo(1)
                                .referenceName("Amitabh Sen")
                                .relation("Friend")
                                .location("Mumbai")
                                .contactNo("9876512346")
                                .email("amitabh.sen@testit.com")
                                .build(),
                        ReferenceRequest.builder()
                                .srNo(2)
                                .referenceName("Pooja Nair")
                                .relation("Colleague")
                                .location("Mumbai")
                                .contactNo("9876512347")
                                .email("pooja.nair@testit.com")
                                .build()
                ))
                .build();

        CustomerProfileResponse updated = customerService.updateCustomer(createdCustomerCode, updateRequest, 1L);

        assertNotNull(updated);
        assertEquals("Principal Architect", updated.getPersonal().getOccupation());
        assertEquals("Flat 501, Blue Horizon", updated.getAddress().getFlatBuildingName());
        assertEquals(new BigDecimal("2200000.00"), updated.getIncome().getAnnualIncome());
        assertEquals("UNVERIFIED", updated.getIdentity().getPanValidationStatus());
    }

    @Test
    @Order(4)
    @DisplayName("Postgres Integration: Upload document, confirm disk file is ENC1 (not %PDF), verify plaintext size, and view back correctly")
    void testUploadAndReadDocumentIntegration() throws Exception {
        assertNotNull(createdCustomerCode, "Customer should have been created in Order 1");

        byte[] fileBytes = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "kyc_aadhaar.pdf",
                "application/pdf",
                fileBytes
        );

        CustomerDocumentResponse docResp = customerService.uploadDocument(
                createdCustomerCode,
                file,
                "1002401", // Aadhaar Card lookup
                LocalDate.now().plusYears(5),
                "Integration test document",
                1L
        );

        assertNotNull(docResp);
        assertNotNull(docResp.getId());
        assertEquals("kyc_aadhaar.pdf", docResp.getOriginalFileName());
        assertEquals("application/pdf", docResp.getContentType());
        assertNotNull(docResp.getDocumentType());
        assertEquals("1002401", docResp.getDocumentType().getCode());

        // Requirement 6: Verify c013.file_size_bytes stores the ORIGINAL (plaintext) size, not encrypted size
        assertEquals(fileBytes.length, docResp.getFileSizeBytes());
        CustomerDocument savedDoc = customerDocumentRepository.findById(docResp.getId()).orElseThrow();
        assertEquals(fileBytes.length, savedDoc.getFileSizeBytes());

        // Requirement 10: Confirm the file on disk does NOT start with %PDF, and starts with ENC1
        java.nio.file.Path diskPath = localStorageService.getRootLocation().resolve(savedDoc.getStoragePath());
        assertTrue(java.nio.file.Files.exists(diskPath), "Physical file must exist at " + diskPath);
        byte[] diskBytes = java.nio.file.Files.readAllBytes(diskPath);
        assertNotEquals('%', (char) diskBytes[0], "Encrypted file on disk must NOT start with %PDF");
        assertEquals("ENC1", new String(diskBytes, 0, 4, StandardCharsets.UTF_8), "Encrypted file must start with ENC1 header");

        // Read document file stream back (Requirement 10: views it back correctly)
        CustomerDocumentFile docFile = customerService.getDocumentFile(createdCustomerCode, docResp.getId());
        assertNotNull(docFile);
        assertEquals("kyc_aadhaar.pdf", docFile.fileName());
        assertEquals("application/pdf", docFile.contentType());
        assertArrayEquals(fileBytes, docFile.data());
        assertTrue(new String(docFile.data(), StandardCharsets.UTF_8).startsWith("%PDF"), "Decrypted data must start with %PDF");

        // Verify document list
        List<CustomerDocumentResponse> docs = customerService.getCustomerDocuments(createdCustomerCode);
        assertFalse(docs.isEmpty());
        assertTrue(docs.stream().anyMatch(d -> d.getId().equals(docResp.getId())));
    }

    @Test
    @Order(5)
    @DisplayName("Postgres Integration: Draft is submitted and becomes COMPLETED")
    void testSubmitDraftIntegration() {
        assertNotNull(createdCustomerCode, "Customer should have been created in Order 1");

        CustomerProfileResponse draft = customerService.getCustomerByCode(createdCustomerCode);
        assertEquals("DRAFT", draft.getOnboardingStatus());

        CustomerProfileResponse submitted = customerService.submitCustomer(createdCustomerCode, 1L);
        assertEquals("COMPLETED", submitted.getOnboardingStatus());
        assertNotNull(submitted.getSubmittedAt());

        assertThrows(com.los.bank.customer.exception.CustomerConflictException.class,
                () -> customerService.submitCustomer(createdCustomerCode, 1L));
    }

    @AfterAll
    static void cleanupDatabase(@Autowired @org.springframework.beans.factory.annotation.Qualifier("bankDataSource") javax.sql.DataSource bankDataSource) {
        if (createdCustomerId != null) {
            BankContext.setCurrentDatabase("bank_abc001");
            BankContext.setCurrentBankCode("bank_abc001");
            try (java.sql.Connection conn = bankDataSource.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM c015 WHERE customer_id = " + createdCustomerId);
                stmt.executeUpdate("DELETE FROM c014 WHERE customer_id = " + createdCustomerId);
                stmt.executeUpdate("DELETE FROM c013 WHERE customer_id = " + createdCustomerId);
                stmt.executeUpdate("DELETE FROM c012 WHERE customer_id = " + createdCustomerId);
                stmt.executeUpdate("DELETE FROM c011 WHERE customer_id = " + createdCustomerId);
                stmt.executeUpdate("DELETE FROM c010 WHERE customer_id = " + createdCustomerId);
                stmt.executeUpdate("DELETE FROM c009 WHERE customer_id = " + createdCustomerId);
                stmt.executeUpdate("DELETE FROM c008 WHERE customer_id = " + createdCustomerId);
                stmt.executeUpdate("DELETE FROM c007 WHERE customer_id = " + createdCustomerId);
                stmt.executeUpdate("DELETE FROM c006 WHERE customer_id = " + createdCustomerId);
                stmt.executeUpdate("DELETE FROM c005 WHERE customer_id = " + createdCustomerId);
                stmt.executeUpdate("DELETE FROM c004 WHERE customer_id = " + createdCustomerId);
                stmt.executeUpdate("DELETE FROM c002 WHERE customer_id = " + createdCustomerId);
                stmt.executeUpdate("DELETE FROM c001 WHERE id = " + createdCustomerId);
            } catch (Exception e) {
                // ignore
            } finally {
                BankContext.clear();
            }
        }
    }
}
