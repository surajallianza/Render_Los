package com.los.bank.loan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.los.bank.loan.dto.RetailLoanApplicationEnvelopeDto;
import com.los.bank.loan.dto.RetailLoanFormDto;
import com.los.bank.loan.exception.RetailLoanExceptionHandler;
import com.los.bank.loan.exception.RetailLoanNotFoundException;
import com.los.bank.loan.exception.RetailLoanValidationException;
import com.los.bank.loan.service.RetailLoanApplicationService;
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
class RetailLoanApplicationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RetailLoanApplicationService applicationService;

    @InjectMocks
    private RetailLoanApplicationController controller;

    private RetailLoanFormDto sampleForm;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RetailLoanExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        sampleForm = RetailLoanFormDto.builder()
                .id(1L)
                .loanAccountNumber("RLA-2026-001")
                .branch("BR-001")
                .customerId("CUST-001")
                .productCode("RPL-001")
                .emiApplicable("Yes")
                .bulletPayment("No")
                .build();
    }

    @Test
    @DisplayName("POST /api/retail-loan-applications -> 201 Created")
    void testCreate_Success() throws Exception {
        when(applicationService.createApplication(any(RetailLoanFormDto.class))).thenReturn(sampleForm);

        RetailLoanApplicationEnvelopeDto envelope = RetailLoanApplicationEnvelopeDto.builder()
                .retailLoanForm(sampleForm)
                .build();

        mockMvc.perform(post("/api/retail-loan-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.data.retailLoanForm.loanAccountNumber").value("RLA-2026-001"))
                .andExpect(jsonPath("$.data.retailLoanForm.bulletPayment").value("No"));
    }

    @Test
    @DisplayName("POST /api/retail-loan-applications with missing retailLoanForm -> 400 Bad Request")
    void testCreate_MissingEnvelope_Returns400() throws Exception {
        mockMvc.perform(post("/api/retail-loan-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/retail-loan-applications -> 200 OK")
    void testGetAll_Success() throws Exception {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", Collections.singletonList(sampleForm));
        resp.put("count", 1);

        when(applicationService.getAllApplications(0, 10, null, null)).thenReturn(resp);

        mockMvc.perform(get("/api/retail-loan-applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    @DisplayName("GET /api/retail-loan-applications/999 -> 404 Not Found")
    void testGetById_NotFound() throws Exception {
        when(applicationService.getApplicationById(999L))
                .thenThrow(new RetailLoanNotFoundException("Retail loan application not found with ID: 999"));

        mockMvc.perform(get("/api/retail-loan-applications/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/retail-loan-applications/1 -> 200 OK")
    void testUpdate_Success() throws Exception {
        sampleForm.setEmiApplicable("No");
        sampleForm.setBulletPayment("Yes");
        when(applicationService.updateApplication(eq(1L), any(RetailLoanFormDto.class))).thenReturn(sampleForm);

        RetailLoanApplicationEnvelopeDto envelope = RetailLoanApplicationEnvelopeDto.builder()
                .retailLoanForm(sampleForm)
                .build();

        mockMvc.perform(put("/api/retail-loan-applications/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.retailLoanForm.bulletPayment").value("Yes"));
    }

    @Test
    @DisplayName("POST /api/retail-loan-applications validation error -> 400 Bad Request")
    void testCreate_ValidationFailure_Returns400() throws Exception {
        when(applicationService.createApplication(any(RetailLoanFormDto.class)))
                .thenThrow(new RetailLoanValidationException("emiApplicable must be either 'Yes' or 'No'."));

        RetailLoanApplicationEnvelopeDto envelope = RetailLoanApplicationEnvelopeDto.builder()
                .retailLoanForm(sampleForm)
                .build();

        mockMvc.perform(post("/api/retail-loan-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envelope)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("emiApplicable must be either 'Yes' or 'No'."));
    }
}
