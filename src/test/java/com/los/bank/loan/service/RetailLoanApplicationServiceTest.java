package com.los.bank.loan.service;

import com.los.bank.branch.entity.Branch;
import com.los.bank.branch.repository.BranchRepository;
import com.los.bank.customer.entity.Customer;
import com.los.bank.customer.repository.CustomerRepository;
import com.los.bank.loan.dto.RelatedPartyDto;
import com.los.bank.loan.dto.RetailLoanFormDto;
import com.los.bank.loan.entity.RetailLoanApplication;
import com.los.bank.loan.exception.RetailLoanNotFoundException;
import com.los.bank.loan.exception.RetailLoanValidationException;
import com.los.bank.loan.repository.RetailLoanApplicationRepository;
import com.los.bank.product.entity.LoanProduct;
import com.los.bank.product.repository.LoanProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetailLoanApplicationServiceTest {

    @Mock
    private RetailLoanApplicationRepository applicationRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private LoanProductRepository loanProductRepository;

    private RetailLoanApplicationValidator validator;
    private RetailLoanApplicationServiceImpl service;

    private RetailLoanFormDto sampleForm;
    private Customer mockCustomer;
    private Branch mockBranch;
    private LoanProduct mockProduct;

    @BeforeEach
    void setUp() {
        validator = new RetailLoanApplicationValidator();
        service = new RetailLoanApplicationServiceImpl(
                applicationRepository, customerRepository, branchRepository, loanProductRepository, validator);

        mockCustomer = new Customer();
        mockCustomer.setId(10L);
        mockCustomer.setCustomerCode("CUST-001");

        mockBranch = new Branch();
        mockBranch.setId(1L);
        mockBranch.setCode("BR-001");

        mockProduct = new LoanProduct();
        mockProduct.setId(5L);
        mockProduct.setCode("RPL-001");
        mockProduct.setName("Retail Personal Loan");

        RelatedPartyDto party = RelatedPartyDto.builder()
                .nameType("Applicant")
                .relation("Other")
                .customerNo("CUST-001")
                .salutation("Mr")
                .firstName("Aarav")
                .middleName("NA")
                .lastName("Sharma")
                .build();

        sampleForm = RetailLoanFormDto.builder()
                .loanAccountNumber("RLA-2026-001")
                .branch("BR-001")
                .effectiveDate("2026-11-26")
                .customerId("CUST-001")
                .title("Mr")
                .firstName("Aarav")
                .middleName("NA")
                .lastName("Sharma")
                .productCode("RPL-001")
                .productName("Retail Personal Loan")
                .accountStatus("Operative")
                .modeOfOperation("Single")
                .lastTransactionDate("2026-10-01")
                .dpd(56)
                .relatedParties(Collections.singletonList(party))
                .sanctionedDate("2026-11-26")
                .sanctioningAuthority("Branch Manager")
                .loanAmount("500000")
                .tenureMonths("12")
                .interestRate("12")
                .offsetRate("0")
                .totalInterestRate("12")
                .emiApplicable("Yes")
                .emiFixDate("Current Date")
                .emiType("Monthly")
                .moratoriumApplicable("No")
                .moratoriumMonths("0")
                .bulletPayment("No")
                .instalmentAmount("44424.40")
                .instalmentStartDate("2026-12-26")
                .instalmentEndDate("2027-11-26")
                .expiryDate("2027-11-26")
                .purposeCode("MEDICAL")
                .prioritySector("No")
                .prioritySectorCode("NA")
                .industryType("Individual")
                .industrySubType("Salaried")
                .physicallyChallenged("No")
                .termCode("12M")
                .securityId("NA")
                .securityType("Unsecured")
                .securityAmount("0")
                .marginPercent("0")
                .assetAddress("Not Applicable")
                .pincode("400001")
                .policyType("Credit Life")
                .premiumFrequency("Annual")
                .policyStatus("Active")
                .autoRenew("No")
                .premiumAmount("6000")
                .policyNumber("POL-2026-001")
                .sumAssured("500000")
                .insuranceCompany("Sample Life Insurance Co.")
                .insuranceIssueDate("2026-11-26")
                .insuranceExpiryDate("2027-11-26")
                .disbursementMode("NEFT")
                .disbursementAccountNumber("123456789012")
                .ifscCode("HDFC0001234")
                .bankName("HDFC Bank")
                .beneficiaryAccountName("Aarav Sharma")
                .beneficiaryMobileNumber("9876543210")
                .disbursementAmount("500000")
                .remarks("Retail personal loan for medical expenses; unsecured.")
                .build();
    }

    private void stubMasters() {
        when(customerRepository.findByCustomerCode("CUST-001")).thenReturn(Optional.of(mockCustomer));
        when(branchRepository.findByCodeIgnoreCase("BR-001")).thenReturn(Optional.of(mockBranch));
        when(loanProductRepository.findByCodeIgnoreCase("RPL-001")).thenReturn(Optional.of(mockProduct));
    }

    private void stubSaveWithId(Long id) {
        when(applicationRepository.save(any(RetailLoanApplication.class))).thenAnswer(inv -> {
            RetailLoanApplication entity = inv.getArgument(0);
            entity.setId(id);
            if (entity.getCreatedAt() == null) entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return entity;
        });
    }

    // Test 1: EMI = Yes, Bullet = Yes -> Backend overrides Bullet = No & generates chart
    @Test
    @DisplayName("Test 1: EMI = Yes, Bullet = Yes -> Save Bullet = No; generate 12-month repayment chart")
    void testCreate_EmiYes_ConflictingBulletYes_OverridesToNo() {
        sampleForm.setEmiApplicable("Yes");
        sampleForm.setBulletPayment("Yes");
        when(applicationRepository.existsByApplicationNumber("RLA-2026-001")).thenReturn(false);
        stubMasters();
        stubSaveWithId(1L);

        RetailLoanFormDto res = service.createApplication(sampleForm);

        assertEquals("Yes", res.getEmiApplicable());
        assertEquals("No", res.getBulletPayment());
        assertNotNull(res.getRepaymentSchedule());
        assertEquals(12, res.getRepaymentSchedule().size());
        assertEquals("Dec 2026", res.getRepaymentSchedule().get(0).getMonth());
        assertEquals("0.00", res.getRepaymentSchedule().get(11).getBalanceAmount());
    }

    // Test 2: EMI = Yes, Bullet = No -> Save Bullet = No; generate chart
    @Test
    @DisplayName("Test 2: EMI = Yes, Bullet = No -> Save Bullet = No; generate repayment chart")
    void testCreate_EmiYes_BulletNo() {
        sampleForm.setEmiApplicable("Yes");
        sampleForm.setBulletPayment("No");
        when(applicationRepository.existsByApplicationNumber("RLA-2026-001")).thenReturn(false);
        stubMasters();
        stubSaveWithId(2L);

        RetailLoanFormDto res = service.createApplication(sampleForm);

        assertEquals("Yes", res.getEmiApplicable());
        assertEquals("No", res.getBulletPayment());
        assertNotNull(res.getRepaymentSchedule());
        assertEquals(12, res.getRepaymentSchedule().size());
    }

    // Test 3: EMI = No, Bullet = No -> Backend overrides Bullet = Yes; no chart
    @Test
    @DisplayName("Test 3: EMI = No, Bullet = No -> Save Bullet = Yes; no repayment chart")
    void testCreate_EmiNo_ConflictingBulletNo_OverridesToYes() {
        sampleForm.setEmiApplicable("No");
        sampleForm.setBulletPayment("No");
        when(applicationRepository.existsByApplicationNumber("RLA-2026-001")).thenReturn(false);
        stubMasters();
        stubSaveWithId(3L);

        RetailLoanFormDto res = service.createApplication(sampleForm);

        assertEquals("No", res.getEmiApplicable());
        assertEquals("Yes", res.getBulletPayment());
        assertNull(res.getRepaymentSchedule());
    }

    // Test 4: EMI = No, Bullet = Yes -> Save Bullet = Yes; no chart
    @Test
    @DisplayName("Test 4: EMI = No, Bullet = Yes -> Save Bullet = Yes; no repayment chart")
    void testCreate_EmiNo_BulletYes() {
        sampleForm.setEmiApplicable("No");
        sampleForm.setBulletPayment("Yes");
        when(applicationRepository.existsByApplicationNumber("RLA-2026-001")).thenReturn(false);
        stubMasters();
        stubSaveWithId(4L);

        RetailLoanFormDto res = service.createApplication(sampleForm);

        assertEquals("No", res.getEmiApplicable());
        assertEquals("Yes", res.getBulletPayment());
        assertNull(res.getRepaymentSchedule());
    }

    // Test 5: EMI missing -> Validation error
    @Test
    @DisplayName("Test 5: EMI missing -> Throws RetailLoanValidationException")
    void testCreate_EmiMissing_ThrowsValidation() {
        sampleForm.setEmiApplicable(null);

        RetailLoanValidationException ex = assertThrows(RetailLoanValidationException.class,
                () -> service.createApplication(sampleForm));
        assertTrue(ex.getMessage().contains("emiApplicable"));
        verify(applicationRepository, never()).save(any());
    }

    // Test 6: Invalid EMI value -> Validation error
    @Test
    @DisplayName("Test 6: Invalid EMI value -> Throws RetailLoanValidationException")
    void testCreate_InvalidEmiValue_ThrowsValidation() {
        sampleForm.setEmiApplicable("Maybe");

        RetailLoanValidationException ex = assertThrows(RetailLoanValidationException.class,
                () -> service.createApplication(sampleForm));
        assertTrue(ex.getMessage().contains("'Yes' or 'No'"));
        verify(applicationRepository, never()).save(any());
    }

    // Test 7: Update Yes -> No -> Bullet = Yes; no active EMI chart
    @Test
    @DisplayName("Test 7: Update EMI Yes -> No sets Bullet = Yes and clears EMI chart")
    void testUpdate_EmiYesToNo() {
        RetailLoanApplication existing = RetailLoanApplication.builder()
                .id(1L)
                .applicationNumber("RLA-2026-001")
                .customerId(10L)
                .branchId(1L)
                .productCode("RPL-001")
                .status("Operative")
                .eligibilityStatus("EMI")
                .emi(new BigDecimal("44424.40"))
                .build();

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(applicationRepository.existsByApplicationNumberAndIdNot("RLA-2026-001", 1L)).thenReturn(false);
        stubMasters();
        stubSaveWithId(1L);

        sampleForm.setEmiApplicable("No");
        sampleForm.setBulletPayment("No"); // conflicting on purpose
        RetailLoanFormDto updated = service.updateApplication(1L, sampleForm);

        assertEquals("No", updated.getEmiApplicable());
        assertEquals("Yes", updated.getBulletPayment());
        assertNull(updated.getRepaymentSchedule());
        assertNull(existing.getEmi());
        assertEquals("BULLET", existing.getEligibilityStatus());
    }

    // Test 8: Update No -> Yes -> Bullet = No; generate EMI chart
    @Test
    @DisplayName("Test 8: Update EMI No -> Yes sets Bullet = No and generates EMI chart")
    void testUpdate_EmiNoToYes() {
        RetailLoanApplication existing = RetailLoanApplication.builder()
                .id(1L)
                .applicationNumber("RLA-2026-001")
                .customerId(10L)
                .branchId(1L)
                .productCode("RPL-001")
                .status("Operative")
                .eligibilityStatus("BULLET")
                .build();

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(applicationRepository.existsByApplicationNumberAndIdNot("RLA-2026-001", 1L)).thenReturn(false);
        stubMasters();
        stubSaveWithId(1L);

        sampleForm.setEmiApplicable("Yes");
        sampleForm.setBulletPayment("Yes"); // conflicting on purpose
        RetailLoanFormDto updated = service.updateApplication(1L, sampleForm);

        assertEquals("Yes", updated.getEmiApplicable());
        assertEquals("No", updated.getBulletPayment());
        assertNotNull(updated.getRepaymentSchedule());
        assertEquals(12, updated.getRepaymentSchedule().size());
        assertEquals("EMI", existing.getEligibilityStatus());
    }

    @Test
    @DisplayName("Get Application By ID - Nonexistent throws RetailLoanNotFoundException")
    void testGetById_NotFound() {
        when(applicationRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RetailLoanNotFoundException.class, () -> service.getApplicationById(999L));
    }

    @Test
    @DisplayName("Get All Applications - Returns paginated list with no chart for Bullet loans")
    void testGetAll_MixedEmiAndBullet() {
        RetailLoanApplication emiApp = RetailLoanApplication.builder()
                .id(1L).applicationNumber("RLA-1").customerId(10L).branchId(1L).productCode("RPL-001")
                .status("Operative").eligibilityStatus("EMI").appliedAmount(new BigDecimal("500000"))
                .interestRate(new BigDecimal("12")).tenureMonths(12).emi(new BigDecimal("44424.40"))
                .build();
        RetailLoanApplication bulletApp = RetailLoanApplication.builder()
                .id(2L).applicationNumber("RLA-2").customerId(10L).branchId(1L).productCode("RPL-001")
                .status("Operative").eligibilityStatus("BULLET").appliedAmount(new BigDecimal("500000"))
                .interestRate(new BigDecimal("12")).tenureMonths(12)
                .build();

        Page<RetailLoanApplication> page = new PageImpl<>(java.util.Arrays.asList(emiApp, bulletApp));
        when(applicationRepository.searchApplications(isNull(), isNull(), any(Pageable.class))).thenReturn(page);
        when(customerRepository.findById(10L)).thenReturn(Optional.of(mockCustomer));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(mockBranch));
        when(loanProductRepository.findByCodeIgnoreCase("RPL-001")).thenReturn(Optional.of(mockProduct));

        Map<String, Object> result = service.getAllApplications(0, 10, null, null);
        assertEquals(true, result.get("success"));
        assertEquals(2, result.get("count"));

        @SuppressWarnings("unchecked")
        java.util.List<RetailLoanFormDto> list = (java.util.List<RetailLoanFormDto>) result.get("data");
        assertEquals("Yes", list.get(0).getEmiApplicable());
        assertNotNull(list.get(0).getRepaymentSchedule());
        assertEquals("No", list.get(1).getEmiApplicable());
        assertEquals("Yes", list.get(1).getBulletPayment());
        assertNull(list.get(1).getRepaymentSchedule());
    }
}
