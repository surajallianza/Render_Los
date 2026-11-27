package com.los.bank.product.service;

import com.los.bank.product.dto.DelegationOfPowerDto;
import com.los.bank.product.dto.LoanProductRequestDto;
import com.los.bank.product.dto.LoanProductResponseDto;
import com.los.bank.product.entity.LoanProduct;
import com.los.bank.product.entity.ProductDelegation;
import com.los.bank.product.exception.LoanProductNotFoundException;
import com.los.bank.product.exception.LoanProductValidationException;
import com.los.bank.product.repository.LoanProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoanProductServiceTest {

    @Mock
    private LoanProductRepository loanProductRepository;

    @InjectMocks
    private LoanProductServiceImpl loanProductService;

    private LoanProductRequestDto validRequestDto;
    private LoanProduct mockProduct;

    @BeforeEach
    void setUp() {
        DelegationOfPowerDto delegation = DelegationOfPowerDto.builder()
                .sr("1")
                .limit(new BigDecimal("1000000"))
                .power("Bank Manager")
                .build();

        validRequestDto = LoanProductRequestDto.builder()
                .productCode("101")
                .productName("Home loan")
                .emi("Arrears")
                .emiType("Int in Advance")
                .minTenureMonths(120)
                .maxTenureMonths(200)
                .minAmount(new BigDecimal("20000"))
                .maxAmount(new BigDecimal("200000"))
                .intBaseMethod("Actual/365")
                .noOfDaysInYear(360)
                .interestCalculationFrequency("Monthly")
                .interestCalculationMode("Principal Outstanding")
                .penalty(true)
                .penaltyPercent(new BigDecimal("2.0000"))
                .delegationOfPower(new ArrayList<>(Collections.singletonList(delegation)))
                .build();

        mockProduct = LoanProduct.builder()
                .id(1L)
                .code("101")
                .name("Home loan")
                .status("ACTIVE")
                .emi("Arrears")
                .emiType("Int in Advance")
                .minTenureMonths(120)
                .maxTenureMonths(200)
                .minAmount(new BigDecimal("20000"))
                .maxAmount(new BigDecimal("200000"))
                .intBaseMethod("Actual/365")
                .noOfDaysInYear(360)
                .interestCalculationFrequency("Monthly")
                .interestCalculationMode("Principal Outstanding")
                .penalty(true)
                .penaltyPercent(new BigDecimal("2.0000"))
                .createdAt(LocalDateTime.now())
                .delegationOfPower(new ArrayList<>())
                .build();

        ProductDelegation productDelegation = ProductDelegation.builder()
                .id(10L)
                .sr("1")
                .limit(new BigDecimal("1000000"))
                .power("Bank Manager")
                .loanProduct(mockProduct)
                .build();
        mockProduct.getDelegationOfPower().add(productDelegation);
    }

    // =========================================================================
    // 1. CREATE LOAN PRODUCT TESTS
    // =========================================================================

    @Test
    @DisplayName("Create Loan Product - Successful creation with complete sample JSON")
    void testCreateLoanProduct_Success() {
        when(loanProductRepository.existsByCode("101")).thenReturn(false);
        when(loanProductRepository.save(any(LoanProduct.class))).thenReturn(mockProduct);

        LoanProductResponseDto response = loanProductService.createLoanProduct(validRequestDto);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("101", response.getProductCode());
        assertEquals("Home loan", response.getProductName());
        assertEquals("Arrears", response.getEmi());
        assertEquals("Int in Advance", response.getEmiType());
        assertEquals(120, response.getMinTenureMonths());
        assertEquals(200, response.getMaxTenureMonths());
        assertEquals(new BigDecimal("20000"), response.getMinAmount());
        assertEquals(new BigDecimal("200000"), response.getMaxAmount());
        assertEquals("Actual/365", response.getIntBaseMethod());
        assertEquals(360, response.getNoOfDaysInYear());
        assertEquals("Monthly", response.getInterestCalculationFrequency());
        assertEquals("Principal Outstanding", response.getInterestCalculationMode());
        assertTrue(response.getPenalty());
        assertEquals(new BigDecimal("2.0000"), response.getPenaltyPercent());
        assertNotNull(response.getDelegationOfPower());
        assertEquals(1, response.getDelegationOfPower().size());
        assertEquals("Bank Manager", response.getDelegationOfPower().get(0).getPower());

        verify(loanProductRepository, times(1)).save(any(LoanProduct.class));
    }

    @Test
    @DisplayName("Create Loan Product - Missing product code throws validation exception")
    void testCreateLoanProduct_MissingProductCode() {
        validRequestDto.setProductCode(null);
        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Product code is required.", ex.getMessage());
    }

    @Test
    @DisplayName("Create Loan Product - Missing product name throws validation exception")
    void testCreateLoanProduct_MissingProductName() {
        validRequestDto.setProductName(" ");
        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Product name is required.", ex.getMessage());
    }

    @Test
    @DisplayName("Create Loan Product - Duplicate product code throws validation exception")
    void testCreateLoanProduct_DuplicateProductCode() {
        when(loanProductRepository.existsByCode("101")).thenReturn(true);
        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Product code already exists.", ex.getMessage());
    }

    @Test
    @DisplayName("Create Loan Product - Invalid EMI configuration throws validation exception")
    void testCreateLoanProduct_InvalidEmi() {
        validRequestDto.setEmi("InvalidEmi");
        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Please select a valid EMI type.", ex.getMessage());
    }

    @Test
    @DisplayName("Create Loan Product - Invalid EMI type throws validation exception")
    void testCreateLoanProduct_InvalidEmiType() {
        validRequestDto.setEmiType(null);
        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Please select a valid EMI calculation type.", ex.getMessage());
    }

    @Test
    @DisplayName("Create Loan Product - Invalid tenure range throws validation exception")
    void testCreateLoanProduct_InvalidTenureRange() {
        validRequestDto.setMinTenureMonths(0);
        LoanProductValidationException ex1 = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Minimum tenure must be greater than zero.", ex1.getMessage());

        validRequestDto.setMinTenureMonths(100);
        validRequestDto.setMaxTenureMonths(50);
        LoanProductValidationException ex2 = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Maximum tenure must be greater than or equal to minimum tenure.", ex2.getMessage());
    }

    @Test
    @DisplayName("Create Loan Product - Invalid amount range throws validation exception")
    void testCreateLoanProduct_InvalidAmountRange() {
        validRequestDto.setMinAmount(BigDecimal.ZERO);
        LoanProductValidationException ex1 = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Minimum loan amount must be greater than zero.", ex1.getMessage());

        validRequestDto.setMinAmount(new BigDecimal("50000"));
        validRequestDto.setMaxAmount(new BigDecimal("20000"));
        LoanProductValidationException ex2 = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Maximum loan amount must be greater than or equal to minimum amount.", ex2.getMessage());
    }

    @Test
    @DisplayName("Create Loan Product - Invalid interest base method throws validation exception")
    void testCreateLoanProduct_InvalidInterestBaseMethod() {
        validRequestDto.setIntBaseMethod("UnknownMethod");
        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Please select a valid interest base method.", ex.getMessage());
    }

    @Test
    @DisplayName("Create Loan Product - Invalid days in year throws validation exception")
    void testCreateLoanProduct_InvalidDaysInYear() {
        validRequestDto.setNoOfDaysInYear(-1);
        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Number of days in a year must be greater than zero.", ex.getMessage());
    }

    @Test
    @DisplayName("Create Loan Product - Missing frequency throws validation exception")
    void testCreateLoanProduct_MissingFrequency() {
        validRequestDto.setInterestCalculationFrequency(null);
        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Interest calculation frequency is required.", ex.getMessage());
    }

    @Test
    @DisplayName("Create Loan Product - Missing mode throws validation exception")
    void testCreateLoanProduct_MissingMode() {
        validRequestDto.setInterestCalculationMode(null);
        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Interest calculation mode is required.", ex.getMessage());
    }

    @Test
    @DisplayName("Create Loan Product - Invalid penalty throws validation exception")
    void testCreateLoanProduct_InvalidPenalty() {
        validRequestDto.setPenalty(null);
        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Invalid penalty configuration.", ex.getMessage());
    }

    @Test
    @DisplayName("Create Loan Product - Invalid penalty percent throws validation exception")
    void testCreateLoanProduct_InvalidPenaltyPercent() {
        validRequestDto.setPenaltyPercent(new BigDecimal("-1"));
        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Please enter a valid penalty percentage.", ex.getMessage());
    }

    @Test
    @DisplayName("Create Loan Product - Empty delegation collection permitted")
    void testCreateLoanProduct_EmptyDelegations() {
        validRequestDto.setDelegationOfPower(new ArrayList<>());
        mockProduct.setDelegationOfPower(new ArrayList<>());
        when(loanProductRepository.existsByCode("101")).thenReturn(false);
        when(loanProductRepository.save(any(LoanProduct.class))).thenReturn(mockProduct);

        LoanProductResponseDto response = loanProductService.createLoanProduct(validRequestDto);
        assertNotNull(response);
        assertEquals(0, response.getDelegationOfPower().size());
    }

    @Test
    @DisplayName("Create Loan Product - Multiple delegation records saved with relationship")
    void testCreateLoanProduct_MultipleDelegations() {
        DelegationOfPowerDto d1 = DelegationOfPowerDto.builder().sr("1").limit(new BigDecimal("500000")).power("Branch Manager").build();
        DelegationOfPowerDto d2 = DelegationOfPowerDto.builder().sr("2").limit(new BigDecimal("1000000")).power("Regional Head").build();
        validRequestDto.setDelegationOfPower(Arrays.asList(d1, d2));

        ProductDelegation pd1 = ProductDelegation.builder().id(1L).sr("1").limit(new BigDecimal("500000")).power("Branch Manager").loanProduct(mockProduct).build();
        ProductDelegation pd2 = ProductDelegation.builder().id(2L).sr("2").limit(new BigDecimal("1000000")).power("Regional Head").loanProduct(mockProduct).build();
        mockProduct.setDelegationOfPower(Arrays.asList(pd1, pd2));

        when(loanProductRepository.existsByCode("101")).thenReturn(false);
        when(loanProductRepository.save(any(LoanProduct.class))).thenReturn(mockProduct);

        LoanProductResponseDto response = loanProductService.createLoanProduct(validRequestDto);
        assertNotNull(response);
        assertEquals(2, response.getDelegationOfPower().size());
    }

    @Test
    @DisplayName("Create Loan Product - Invalid delegation fields throw appropriate exceptions")
    void testCreateLoanProduct_InvalidDelegationFields() {
        // Missing Sr
        validRequestDto.getDelegationOfPower().get(0).setSr(null);
        LoanProductValidationException ex1 = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Delegation serial number is required.", ex1.getMessage());

        // Negative limit
        validRequestDto.getDelegationOfPower().get(0).setSr("1");
        validRequestDto.getDelegationOfPower().get(0).setLimit(new BigDecimal("-10"));
        LoanProductValidationException ex2 = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Please enter a valid delegation limit.", ex2.getMessage());

        // Missing power
        validRequestDto.getDelegationOfPower().get(0).setLimit(new BigDecimal("1000"));
        validRequestDto.getDelegationOfPower().get(0).setPower(null);
        LoanProductValidationException ex3 = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("Delegation power is required.", ex3.getMessage());
    }

    // =========================================================================
    // 2. GET ALL LOAN PRODUCTS TESTS
    // =========================================================================

    @Test
    @DisplayName("Get All Loan Products - Returns paged results and metadata")
    void testGetAllLoanProducts_Success() {
        Page<LoanProduct> page = new PageImpl<>(Collections.singletonList(mockProduct));
        when(loanProductRepository.searchProducts(any(), any(Pageable.class))).thenReturn(page);

        Map<String, Object> result = loanProductService.getAllLoanProducts(0, 10, null);

        assertTrue((Boolean) result.get("success"));
        assertEquals(1, result.get("count"));
        assertEquals(1L, result.get("totalElements"));
        List<?> data = (List<?>) result.get("data");
        assertEquals(1, data.size());
    }

    @Test
    @DisplayName("Get All Loan Products - Empty result handling")
    void testGetAllLoanProducts_Empty() {
        Page<LoanProduct> emptyPage = new PageImpl<>(Collections.emptyList());
        when(loanProductRepository.searchProducts(any(), any(Pageable.class))).thenReturn(emptyPage);

        Map<String, Object> result = loanProductService.getAllLoanProducts(0, 10, "NonExistent");

        assertTrue((Boolean) result.get("success"));
        assertEquals(0, result.get("count"));
        assertEquals(0L, result.get("totalElements"));
    }

    // =========================================================================
    // 3. GET LOAN PRODUCT BY ID TESTS
    // =========================================================================

    @Test
    @DisplayName("Get Loan Product By ID - Existing product returned with delegations")
    void testGetLoanProductById_Success() {
        when(loanProductRepository.findByIdWithDelegations(1L)).thenReturn(Optional.of(mockProduct));

        LoanProductResponseDto result = loanProductService.getLoanProductById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("101", result.getProductCode());
        assertEquals(1, result.getDelegationOfPower().size());
    }

    @Test
    @DisplayName("Get Loan Product By ID - Non-existent ID throws NotFoundException")
    void testGetLoanProductById_NotFound() {
        when(loanProductRepository.findByIdWithDelegations(999L)).thenReturn(Optional.empty());

        LoanProductNotFoundException ex = assertThrows(LoanProductNotFoundException.class,
                () -> loanProductService.getLoanProductById(999L));
        assertEquals("Loan product not found with ID: 999", ex.getMessage());
    }

    @Test
    @DisplayName("Get Loan Product By ID - Invalid ID throws ValidationException")
    void testGetLoanProductById_InvalidId() {
        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.getLoanProductById(-1L));
        assertEquals("Valid loan product ID is required.", ex.getMessage());
    }

    // =========================================================================
    // 4. GET LOAN PRODUCT BY CODE TESTS
    // =========================================================================

    @Test
    @DisplayName("Get Loan Product By Code - Existing product returned")
    void testGetLoanProductByCode_Success() {
        when(loanProductRepository.findByCodeWithDelegations("101")).thenReturn(Optional.of(mockProduct));

        LoanProductResponseDto result = loanProductService.getLoanProductByCode("101");

        assertNotNull(result);
        assertEquals("101", result.getProductCode());
        assertEquals("Home loan", result.getProductName());
    }

    @Test
    @DisplayName("Get Loan Product By Code - Non-existent code throws NotFoundException")
    void testGetLoanProductByCode_NotFound() {
        when(loanProductRepository.findByCodeWithDelegations("999")).thenReturn(Optional.empty());

        LoanProductNotFoundException ex = assertThrows(LoanProductNotFoundException.class,
                () -> loanProductService.getLoanProductByCode("999"));
        assertEquals("Loan product not found with code: 999", ex.getMessage());
    }

    @Test
    @DisplayName("Get Loan Product By Code - Blank code throws ValidationException")
    void testGetLoanProductByCode_InvalidCode() {
        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.getLoanProductByCode(" "));
        assertEquals("Product code is required.", ex.getMessage());
    }

    // =========================================================================
    // 5. UPDATE LOAN PRODUCT TESTS
    // =========================================================================

    @Test
    @DisplayName("Update Loan Product - Successfully updates fields and delegations")
    void testUpdateLoanProduct_Success() {
        when(loanProductRepository.findByIdWithDelegations(1L)).thenReturn(Optional.of(mockProduct));
        when(loanProductRepository.existsByCodeAndIdNot("101", 1L)).thenReturn(false);
        when(loanProductRepository.save(any(LoanProduct.class))).thenReturn(mockProduct);

        validRequestDto.setProductName("Updated Home Loan");
        validRequestDto.setMinAmount(new BigDecimal("30000"));

        LoanProductResponseDto result = loanProductService.updateLoanProduct(1L, validRequestDto);

        assertNotNull(result);
        verify(loanProductRepository, times(1)).save(mockProduct);
    }

    @Test
    @DisplayName("Update Loan Product - Duplicate code on another product throws ValidationException")
    void testUpdateLoanProduct_DuplicateCodeOnOtherProduct() {
        when(loanProductRepository.findByIdWithDelegations(1L)).thenReturn(Optional.of(mockProduct));
        when(loanProductRepository.existsByCodeAndIdNot("102", 1L)).thenReturn(true);

        validRequestDto.setProductCode("102");

        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.updateLoanProduct(1L, validRequestDto));
        assertEquals("Product code already exists.", ex.getMessage());
    }

    @Test
    @DisplayName("Update Loan Product - Non-existent product throws NotFoundException")
    void testUpdateLoanProduct_NotFound() {
        when(loanProductRepository.findByIdWithDelegations(999L)).thenReturn(Optional.empty());

        LoanProductNotFoundException ex = assertThrows(LoanProductNotFoundException.class,
                () -> loanProductService.updateLoanProduct(999L, validRequestDto));
        assertEquals("Loan product not found with ID: 999", ex.getMessage());
    }

    // =========================================================================
    // 6. EMI FIXED DATE TESTS
    // =========================================================================

    @Test
    @DisplayName("Create Loan Product - Saves and returns emiFixedDate = Yes")
    void testCreateLoanProduct_WithEmiFixedDateYes() {
        validRequestDto.setEmiFixedDate("Yes");
        when(loanProductRepository.existsByCode("101")).thenReturn(false);
        when(loanProductRepository.save(any(LoanProduct.class))).thenAnswer(inv -> {
            LoanProduct saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        LoanProductResponseDto response = loanProductService.createLoanProduct(validRequestDto);

        assertNotNull(response);
        assertEquals("Yes", response.getEmiFixedDate());
    }

    @Test
    @DisplayName("Create Loan Product - Defaults emiFixedDate to No when omitted")
    void testCreateLoanProduct_DefaultsEmiFixedDateToNo() {
        validRequestDto.setEmiFixedDate(null);
        when(loanProductRepository.existsByCode("101")).thenReturn(false);
        when(loanProductRepository.save(any(LoanProduct.class))).thenAnswer(inv -> {
            LoanProduct saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        LoanProductResponseDto response = loanProductService.createLoanProduct(validRequestDto);

        assertNotNull(response);
        assertEquals("No", response.getEmiFixedDate());
    }

    @Test
    @DisplayName("Create Loan Product - Invalid emiFixedDate throws ValidationException")
    void testCreateLoanProduct_InvalidEmiFixedDateThrows() {
        validRequestDto.setEmiFixedDate("Maybe");

        LoanProductValidationException ex = assertThrows(LoanProductValidationException.class,
                () -> loanProductService.createLoanProduct(validRequestDto));
        assertEquals("EMI fixed date must be either 'Yes' or 'No'.", ex.getMessage());
    }

    @Test
    @DisplayName("Update Loan Product - Updates emiFixedDate to Yes")
    void testUpdateLoanProduct_UpdatesEmiFixedDate() {
        mockProduct.setEmiFixedDate("No");
        when(loanProductRepository.findByIdWithDelegations(1L)).thenReturn(Optional.of(mockProduct));
        when(loanProductRepository.existsByCodeAndIdNot("101", 1L)).thenReturn(false);
        when(loanProductRepository.save(any(LoanProduct.class))).thenAnswer(inv -> inv.getArgument(0));

        validRequestDto.setEmiFixedDate("Yes");
        LoanProductResponseDto result = loanProductService.updateLoanProduct(1L, validRequestDto);

        assertNotNull(result);
        assertEquals("Yes", result.getEmiFixedDate());
    }
}
