package com.los.bank.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.los.bank.product.dto.DelegationOfPowerDto;
import com.los.bank.product.dto.LoanProductRequestDto;
import com.los.bank.product.dto.LoanProductResponseDto;
import com.los.bank.product.exception.LoanProductExceptionHandler;
import com.los.bank.product.exception.LoanProductNotFoundException;
import com.los.bank.product.exception.LoanProductValidationException;
import com.los.bank.product.service.LoanProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class LoanProductControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private LoanProductService loanProductService;

    @InjectMocks
    private LoanProductController loanProductController;

    private LoanProductRequestDto sampleRequest;
    private LoanProductResponseDto sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(loanProductController)
                .setControllerAdvice(new LoanProductExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        DelegationOfPowerDto delegation = DelegationOfPowerDto.builder()
                .sr("1")
                .limit(new BigDecimal("1000000"))
                .power("Bank Manager")
                .build();

        sampleRequest = LoanProductRequestDto.builder()
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
                .delegationOfPower(Collections.singletonList(delegation))
                .build();

        sampleResponse = LoanProductResponseDto.builder()
                .id(1L)
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
                .delegationOfPower(Collections.singletonList(delegation))
                .build();
    }

    @Test
    @DisplayName("POST /api/loan-products - Success returns 201 Created")
    void testCreateLoanProduct_Success() throws Exception {
        when(loanProductService.createLoanProduct(any(LoanProductRequestDto.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/loan-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Loan product created successfully"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.data.product_code").value("101"))
                .andExpect(jsonPath("$.data.product_name").value("Home loan"))
                .andExpect(jsonPath("$.data.delegation_of_power[0].Sr").value("1"));
    }

    @Test
    @DisplayName("POST /api/loan-products - Validation failure returns 400 Bad Request")
    void testCreateLoanProduct_ValidationFailure() throws Exception {
        when(loanProductService.createLoanProduct(any(LoanProductRequestDto.class)))
                .thenThrow(new LoanProductValidationException("Product code is required."));

        sampleRequest.setProductCode(null);

        mockMvc.perform(post("/api/loan-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Product code is required."));
    }

    @Test
    @DisplayName("GET /api/loan-products - Success returns 200 OK with list")
    void testGetAllLoanProducts_Success() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("data", Collections.singletonList(sampleResponse));
        map.put("count", 1);
        map.put("totalElements", 1);

        when(loanProductService.getAllLoanProducts(0, 10, null)).thenReturn(map);

        mockMvc.perform(get("/api/loan-products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data[0].product_code").value("101"));
    }

    @Test
    @DisplayName("GET /api/loan-products/{id} - Success returns 200 OK")
    void testGetLoanProductById_Success() throws Exception {
        when(loanProductService.getLoanProductById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/loan-products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.product_code").value("101"));
    }

    @Test
    @DisplayName("GET /api/loan-products/{id} - Not found returns 404")
    void testGetLoanProductById_NotFound() throws Exception {
        when(loanProductService.getLoanProductById(999L))
                .thenThrow(new LoanProductNotFoundException("Loan product not found with ID: 999"));

        mockMvc.perform(get("/api/loan-products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Loan product not found with ID: 999"));
    }

    @Test
    @DisplayName("GET /api/loan-products/code/{code} - Success returns 200 OK")
    void testGetLoanProductByCode_Success() throws Exception {
        when(loanProductService.getLoanProductByCode("101")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/loan-products/code/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.product_code").value("101"));
    }

    @Test
    @DisplayName("GET /api/loan-products/code/{code} - Not found returns 404")
    void testGetLoanProductByCode_NotFound() throws Exception {
        when(loanProductService.getLoanProductByCode("NON_EXIST"))
                .thenThrow(new LoanProductNotFoundException("Loan product not found with code: NON_EXIST"));

        mockMvc.perform(get("/api/loan-products/code/NON_EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Loan product not found with code: NON_EXIST"));
    }

    @Test
    @DisplayName("PUT /api/loan-products/{id} - Success returns 200 OK")
    void testUpdateLoanProduct_Success() throws Exception {
        when(loanProductService.updateLoanProduct(eq(1L), any(LoanProductRequestDto.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/loan-products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Loan product updated successfully"))
                .andExpect(jsonPath("$.data.product_code").value("101"));
    }

    @Test
    @DisplayName("PUT /api/loan-products/{id} - Not found returns 404")
    void testUpdateLoanProduct_NotFound() throws Exception {
        when(loanProductService.updateLoanProduct(eq(999L), any(LoanProductRequestDto.class)))
                .thenThrow(new LoanProductNotFoundException("Loan product not found with ID: 999"));

        mockMvc.perform(put("/api/loan-products/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Loan product not found with ID: 999"));
    }
}
