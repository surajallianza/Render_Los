package com.los.bank.customer.service;

import com.los.bank.customer.config.CustomerLookupProperties;
import com.los.bank.customer.dto.*;
import com.los.bank.customer.exception.CustomerValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerValidatorTest {

    @Mock
    private CustomerLookupHelper lookupHelper;

    private CustomerCryptoService cryptoService;
    private CustomerValidator validator;
    private CustomerLookupProperties properties;

    @BeforeEach
    void setUp() {
        cryptoService = new CustomerCryptoService("SecretKeyAES256ForTestPurposeOnly!", "SecretKeyHMAC256ForTestPurposeOnly!");
        validator = new CustomerValidator(lookupHelper, cryptoService);
        properties = new CustomerLookupProperties();
        when(lookupHelper.getProperties()).thenReturn(properties);
    }

    private CustomerSaveRequest buildValidSalariedRequest() {
        return CustomerSaveRequest.builder()
                .personal(PersonalRequest.builder()
                        .title("1000801")
                        .firstName("Rajesh")
                        .middleName("Kumar")
                        .lastName("Sharma")
                        .dob(LocalDate.now().minusYears(30))
                        .gender("1000901")
                        .maritalStatus("1001101")
                        .customerType("1001201")
                        .kycStatus("1001301")
                        .occupation("Software Engineer")
                        .mobile("9876543210")
                        .email("rajesh.sharma@example.com")
                        .branchId(1L)
                        .build())
                .identity(IdentityRequest.builder()
                        .panNumber("ABCDE1234F")
                        .aadhaarNumber("123456789012")
                        .ckycNumber("12345678901234")
                        .panValidationStatus("VALID")
                        .aadhaarValidationStatus("VALID")
                        .build())
                .family(FamilyRequest.builder()
                        .fatherTitle("1000801")
                        .fatherFirstName("Ramesh")
                        .fatherLastName("Sharma")
                        .motherTitle("1000802")
                        .motherFirstName("Sunita")
                        .motherLastName("Sharma")
                        .build())
                .address(AddressRequest.builder()
                        .residentialStatus("1001401")
                        .residentialType("1001501")
                        .flatBuildingName("Flat 402, Green Heights")
                        .streetRoad("MG Road")
                        .landmark("Near Metro Station")
                        .city("Mumbai")
                        .state("1001601")
                        .pincode("400001")
                        .build())
                .employment(EmploymentRequest.builder()
                        .employmentType("1001701")
                        .employerName("Infosys Ltd")
                        .designation("Senior Consultant")
                        .dateOfJoining(LocalDate.now().minusYears(3))
                        .employerEmail("rajesh.s@infosys.com")
                        .yearsOfService(new BigDecimal("3.0"))
                        .build())
                .income(IncomeRequest.builder()
                        .annualIncome(new BigDecimal("1200000.00"))
                        .incomeTaxFilerStatus("1002002") // Non-Filer for simplicity
                        .build())
                .bank(BankRequest.builder()
                        .bankName("State Bank of India")
                        .accountNumber("12345678901")
                        .accountType("1002201")
                        .build())
                .references(List.of(
                        ReferenceRequest.builder()
                                .srNo(1)
                                .referenceName("Amit Patel")
                                .relation("Friend")
                                .location("Mumbai")
                                .contactNo("9823456789")
                                .email("amit.patel@example.com")
                                .build(),
                        ReferenceRequest.builder()
                                .srNo(2)
                                .referenceName("Pooja Verma")
                                .relation("Colleague")
                                .location("Pune")
                                .contactNo("9834567890")
                                .email("pooja.verma@example.com")
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("Valid Salaried request passes validation")
    void testValidSalariedRequestPasses() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Married"))).thenReturn(false);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Self Employed"))).thenReturn(false);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Filer"))).thenReturn(false);

        assertDoesNotThrow(() -> validator.validate(request, false));
    }

    @Test
    @DisplayName("Validation fails when required personal fields are missing")
    void testMissingPersonalFields() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        request.getPersonal().setFirstName("");
        request.getPersonal().setMobile("12345"); // invalid mobile
        request.getPersonal().setEmail("invalid-email");
        request.getPersonal().setDob(LocalDate.now().plusDays(1)); // future DOB

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validate(request, false)
        );

        Map<String, String> errors = ex.getErrors();
        assertTrue(errors.containsKey("personal.firstName"));
        assertTrue(errors.containsKey("personal.mobile"));
        assertTrue(errors.containsKey("personal.email"));
        assertTrue(errors.containsKey("personal.dob"));
    }

    @Test
    @DisplayName("Validation fails when customer age exceeds 80")
    void testAgeAbove80() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        request.getPersonal().setDob(LocalDate.now().minusYears(85));

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validate(request, false)
        );

        assertTrue(ex.getErrors().containsKey("personal.dob"));
        assertEquals("Age cannot be above 80 years", ex.getErrors().get("personal.dob"));
    }

    @Test
    @DisplayName("Derives gender from title if not explicitly supplied")
    void testGenderDerivationFromTitle() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        request.getPersonal().setGender(null);

        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);
        when(lookupHelper.resolveId(eq("Title"), any(), eq("1000801"), anyString(), anyMap())).thenReturn(100L);
        when(lookupHelper.deriveGenderCodeFromTitleId(100L)).thenReturn("1000901");
        when(lookupHelper.resolveId(eq("Gender"), any(), eq("1000901"), anyString(), anyMap())).thenReturn(200L);

        CustomerValidator.ResolvedLookups resolved = validator.validate(request, false);
        assertEquals("1000901", resolved.derivedGenderCode);
        assertEquals(200L, resolved.genderLookupId);
    }

    @Test
    @DisplayName("Married customer requires spouse title, first name and last name")
    void testMarriedCustomerRequiresSpouse() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Married"))).thenReturn(true);

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validate(request, false)
        );

        Map<String, String> errors = ex.getErrors();
        assertTrue(errors.containsKey("family.spouseTitle"));
        assertTrue(errors.containsKey("family.spouseFirstName"));
        assertTrue(errors.containsKey("family.spouseLastName"));
    }

    @Test
    @DisplayName("Self Employed customer requires business details and forbids retirement date and previous employment")
    void testSelfEmployedRules() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        request.getEmployment().setDateOfRetirement(LocalDate.now().plusYears(10));
        request.setPreviousEmployment(List.of(
                PreviousEmploymentRequest.builder().employerName("Old Firm").designation("Dev").build()
        ));

        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Self Employed"))).thenReturn(true);

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validate(request, false)
        );

        Map<String, String> errors = ex.getErrors();
        assertTrue(errors.containsKey("employment.dateOfRetirement"));
        assertTrue(errors.containsKey("previousEmployment"));
        assertTrue(errors.containsKey("employment.location"));
    }

    @Test
    @DisplayName("Filer accepts optional tax returns, ignores empty rows, rejects duplicates or invalid formats")
    void testFilerTaxReturnsRule() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Filer"))).thenReturn(true);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Married"))).thenReturn(false);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Self Employed"))).thenReturn(false);

        // Tax returns are optional for Filer: null or empty passes
        request.setTaxReturns(null);
        assertDoesNotThrow(() -> validator.validate(request, false));

        // Silently ignore rows where both grossIncome and tdsPaid are null
        request.setTaxReturns(List.of(
                TaxReturnRequest.builder().financialYear(null).grossIncome(null).tdsPaid(null).build(),
                TaxReturnRequest.builder().financialYear("2024-25").grossIncome(new BigDecimal("500000")).tdsPaid(new BigDecimal("10000")).build()
        ));
        assertDoesNotThrow(() -> validator.validate(request, false));

        // Duplicate financial year
        request.setTaxReturns(List.of(
                TaxReturnRequest.builder().financialYear("2024-25").grossIncome(new BigDecimal("500000")).tdsPaid(new BigDecimal("10000")).build(),
                TaxReturnRequest.builder().financialYear("2024-25").grossIncome(new BigDecimal("600000")).tdsPaid(new BigDecimal("20000")).build()
        ));

        CustomerValidationException ex2 = assertThrows(CustomerValidationException.class, () ->
                validator.validate(request, false)
        );
        assertTrue(ex2.getErrors().containsKey("taxReturns[1].financialYear"));

        // Invalid financial year format
        request.setTaxReturns(List.of(
                TaxReturnRequest.builder().financialYear("2024-2025").grossIncome(new BigDecimal("500000")).tdsPaid(new BigDecimal("10000")).build()
        ));
        CustomerValidationException ex3 = assertThrows(CustomerValidationException.class, () ->
                validator.validate(request, false)
        );
        assertTrue(ex3.getErrors().containsKey("taxReturns[0].financialYear"));
    }

    @Test
    @DisplayName("Non-Filer rejects non-empty tax returns")
    void testNonFilerRejectsTaxReturns() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Filer"))).thenReturn(false);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Married"))).thenReturn(false);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Self Employed"))).thenReturn(false);

        request.setTaxReturns(List.of(
                TaxReturnRequest.builder().financialYear("2024-25").grossIncome(new BigDecimal("500000")).tdsPaid(new BigDecimal("10000")).build()
        ));

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validate(request, false)
        );
        assertTrue(ex.getErrors().containsKey("taxReturns"));
    }

    @Test
    @DisplayName("Self Employed income requires only annualIncome; others optional and non-negative")
    void testSelfEmployedIncomeRules() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Self Employed"))).thenReturn(true);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Married"))).thenReturn(false);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Filer"))).thenReturn(false);

        // Self-employed employment fields
        request.getEmployment().setLocation("Mumbai");
        request.getEmployment().setDateOfRetirement(null);
        request.setPreviousEmployment(null);

        // Only annualIncome set, others null -> valid
        request.getIncome().setAnnualIncome(new BigDecimal("1500000"));
        request.getIncome().setOtherSourceIncome(null);
        request.getIncome().setTotalGrossIncome(null);
        request.getIncome().setTotalNetIncome(null);
        request.getIncome().setIncomeTax(null);
        assertDoesNotThrow(() -> validator.validate(request, false));

        // Negative optional values -> invalid
        request.getIncome().setOtherSourceIncome(new BigDecimal("-100"));
        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validate(request, false)
        );
        assertTrue(ex.getErrors().containsKey("income.otherSourceIncome"));
    }

    @Test
    @DisplayName("Salaried customer employer email is optional but validated when provided")
    void testSalariedEmployerEmailOptional() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Married"))).thenReturn(false);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Self Employed"))).thenReturn(false);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Filer"))).thenReturn(false);

        // null employer email -> valid
        request.getEmployment().setEmployerEmail(null);
        assertDoesNotThrow(() -> validator.validate(request, false));

        // invalid email -> fails
        request.getEmployment().setEmployerEmail("not-an-email");
        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validate(request, false)
        );
        assertTrue(ex.getErrors().containsKey("employment.employerEmail"));
    }

    @Test
    @DisplayName("Loan history allows past or present expiryDate")
    void testLoanHistoryPastExpiryDateAllowed() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Married"))).thenReturn(false);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Self Employed"))).thenReturn(false);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Filer"))).thenReturn(false);

        request.setLoanHistory(List.of(
                LoanHistoryRequest.builder()
                        .bankName("SBI")
                        .accountNumber("987654321")
                        .loanType("Personal Loan")
                        .totalOutstanding(new BigDecimal("50000"))
                        .emi(new BigDecimal("5000"))
                        .expiryDate(LocalDate.now().minusYears(1)) // Past expiry date is allowed
                        .build()
        ));

        assertDoesNotThrow(() -> validator.validate(request, false));
    }

    @Test
    @DisplayName("Rejects if references list does not have exactly two references with srNo 1 and 2")
    void testReferencesValidation() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);

        // Only 1 reference
        request.setReferences(List.of(
                ReferenceRequest.builder().srNo(1).referenceName("Amit Patel").build()
        ));
        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validate(request, false)
        );
        assertTrue(ex.getErrors().containsKey("references"));
    }

    @Test
    @DisplayName("Accepts masked PAN and Aadhaar on update")
    void testMaskedValuesAllowedOnUpdate() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        request.getIdentity().setPanNumber("ABCDE****F");
        request.getIdentity().setAadhaarNumber("********9012");
        request.getIdentity().setCkycNumber("**********1234");
        request.getBank().setAccountNumber("******9012");

        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Married"))).thenReturn(false);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Self Employed"))).thenReturn(false);
        when(lookupHelper.descriptionMatches(anyLong(), eq("Filer"))).thenReturn(false);

        assertDoesNotThrow(() -> validator.validate(request, true));
    }

    // ---------------------------------------------------------------------
    // Step (partial) validation used by the onboarding draft flow
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Step validation: only the address step is sent and validated")
    void testSectionsAddressOnlyPasses() {
        CustomerSaveRequest full = buildValidSalariedRequest();
        CustomerSaveRequest request = CustomerSaveRequest.builder().address(full.getAddress()).build();
        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);

        assertDoesNotThrow(() -> validator.validateSections(request, true, new CustomerValidator.StoredContext()));
    }

    @Test
    @DisplayName("Step validation: a sent section is still validated completely")
    void testSectionsSentSectionFullyValidated() {
        CustomerSaveRequest full = buildValidSalariedRequest();
        full.getAddress().setPincode("12");
        CustomerSaveRequest request = CustomerSaveRequest.builder().address(full.getAddress()).build();
        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validateSections(request, true, new CustomerValidator.StoredContext()));
        assertEquals(Set.of("address.pincode"), ex.getErrors().keySet());
    }

    @Test
    @DisplayName("Step validation: personal details must come with family details")
    void testSectionsPersonalRequiresFamily() {
        CustomerSaveRequest request = buildValidSalariedRequest();
        request.setFamily(null);
        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validateSections(request, false, null));
        assertTrue(ex.getErrors().containsKey("family"));
    }

    @Test
    @DisplayName("Step validation: family alone uses the stored marital status for spouse rules")
    void testSectionsFamilyUsesStoredMaritalStatus() {
        CustomerSaveRequest request = CustomerSaveRequest.builder().family(buildValidSalariedRequest().getFamily()).build();
        CustomerValidator.StoredContext stored = new CustomerValidator.StoredContext();
        stored.maritalStatusLookupId = 55L;
        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);
        when(lookupHelper.descriptionMatches(55L, "Married")).thenReturn(true);

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validateSections(request, true, stored));
        assertTrue(ex.getErrors().containsKey("family.spouseFirstName"));
    }

    @Test
    @DisplayName("Step validation: income alone uses the stored employment type")
    void testSectionsIncomeUsesStoredEmployment() {
        CustomerSaveRequest request = CustomerSaveRequest.builder()
                .income(IncomeRequest.builder()
                        .annualIncome(new BigDecimal("500000"))
                        .incomeTax(new BigDecimal("-1"))
                        .incomeTaxFilerStatus("1002002")
                        .build())
                .build();
        CustomerValidator.StoredContext stored = new CustomerValidator.StoredContext();
        stored.employmentTypeLookupId = 77L;
        when(lookupHelper.resolveId(anyString(), any(), anyString(), anyString(), anyMap())).thenReturn(10L);
        when(lookupHelper.descriptionMatches(77L, "Self Employed")).thenReturn(true);

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validateSections(request, true, stored));
        assertTrue(ex.getErrors().containsKey("income.incomeTax"));
    }

    @Test
    @DisplayName("Step validation: previous employment needs employment saved first")
    void testSectionsPreviousEmploymentNeedsEmployment() {
        CustomerSaveRequest request = CustomerSaveRequest.builder()
                .previousEmployment(List.of(PreviousEmploymentRequest.builder()
                        .employerName("Wipro").designation("Engineer")
                        .dateOfJoining(LocalDate.now().minusYears(6)).dateOfRelieving(LocalDate.now().minusYears(4))
                        .build()))
                .build();

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validateSections(request, true, new CustomerValidator.StoredContext()));
        assertTrue(ex.getErrors().containsKey("previousEmployment"));
    }

    @Test
    @DisplayName("Step validation: tax returns alone use the stored filer status")
    void testSectionsTaxReturnsUseStoredFilerStatus() {
        CustomerSaveRequest request = CustomerSaveRequest.builder()
                .taxReturns(List.of(TaxReturnRequest.builder()
                        .financialYear("2025-26").grossIncome(new BigDecimal("900000")).tdsPaid(new BigDecimal("50000"))
                        .build()))
                .build();
        CustomerValidator.StoredContext stored = new CustomerValidator.StoredContext();
        stored.incomeTaxFilerStatusLookupId = 88L;
        when(lookupHelper.descriptionMatches(88L, "Filer")).thenReturn(false);

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validateSections(request, true, stored));
        assertEquals("Tax returns must be empty for non-filers", ex.getErrors().get("taxReturns"));
    }

    @Test
    @DisplayName("Step validation: references, when sent, must be exactly two")
    void testSectionsReferencesExactlyTwo() {
        CustomerSaveRequest request = CustomerSaveRequest.builder()
                .references(List.of(buildValidSalariedRequest().getReferences().get(0)))
                .build();

        CustomerValidationException ex = assertThrows(CustomerValidationException.class, () ->
                validator.validateSections(request, true, new CustomerValidator.StoredContext()));
        assertTrue(ex.getErrors().containsKey("references"));
    }
}
