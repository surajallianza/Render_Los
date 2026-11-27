package com.los.bank.rule.controller;

import com.los.bank.rule.dto.RuleOptionsDto;
import com.los.bank.rule.dto.RuleRequestDto;
import com.los.bank.rule.dto.RuleResponseDto;
import com.los.bank.rule.exception.RuleExceptionHandler;
import com.los.bank.rule.exception.RuleNotFoundException;
import com.los.bank.rule.exception.RuleValidationException;
import com.los.bank.rule.service.RuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RuleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RuleService ruleService;

    @InjectMocks
    private RuleController ruleController;

    private RuleResponseDto sample;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ruleController)
                .setControllerAdvice(new RuleExceptionHandler())
                .build();
        sample = RuleResponseDto.builder().id(1L).param("FOIR").op("gt").a("50").decision("Reject").active(true)
                .description("If FOIR is above 50%, reject the application.").build();
    }

    @Test
    @DisplayName("GET /api/rules/options returns the dropdown values")
    void testOptions() throws Exception {
        when(ruleService.getOptions()).thenReturn(RuleOptionsDto.builder()
                .parameters(List.of(RuleOptionsDto.Parameter.builder().code("1002001").name("FOIR").metric("FOIR").build()))
                .operators(List.of()).decisions(List.of()).build());

        mockMvc.perform(get("/api/rules/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parameters[0].name").value("FOIR"));
    }

    @Test
    @DisplayName("POST accepts the Rule Engine screen payload (param, op, a, b, decision, offset, active)")
    void testCreateWithScreenPayload() throws Exception {
        when(ruleService.createRule(any(RuleRequestDto.class), any())).thenReturn(sample);

        mockMvc.perform(post("/api/rules").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"param\":\"CIBIL Score\",\"op\":\"between\",\"a\":401,\"b\":500,\"decision\":\"Override\","
                                + "\"offset\":4,\"active\":true,\"effectiveDate\":\"2030-01-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.data.description").value("If FOIR is above 50%, reject the application."));

        ArgumentCaptor<RuleRequestDto> captor = ArgumentCaptor.forClass(RuleRequestDto.class);
        verify(ruleService).createRule(captor.capture(), any());
        RuleRequestDto sent = captor.getValue();
        assertEquals("CIBIL Score", sent.getParam());
        assertEquals("between", sent.getOp());
        assertEquals("401", sent.getA());
        assertEquals("500", sent.getB());
        assertEquals("Override", sent.getDecision());
        assertEquals(0, new BigDecimal("4").compareTo(sent.getOffset()));
        assertEquals(Boolean.TRUE, sent.getActive());
    }

    @Test
    @DisplayName("Validation errors come back as 400 with field errors")
    void testValidationError() throws Exception {
        when(ruleService.createRule(any(RuleRequestDto.class), any()))
                .thenThrow(new RuleValidationException("Rule validation failed", Map.of("param", "'X' is not an active value")));

        mockMvc.perform(post("/api/rules").contentType(MediaType.APPLICATION_JSON).content("{\"param\":\"X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.param").value("'X' is not an active value"));
    }

    @Test
    @DisplayName("GET /api/rules passes the filters; GET /{id} 404 for unknown")
    void testListAndNotFound() throws Exception {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("success", true);
        page.put("totalElements", 1);
        page.put("data", List.of(sample));
        when(ruleService.getAllRules(0, 100, "ACTIVE", "FOIR", "Reject")).thenReturn(page);
        when(ruleService.getRuleById(99L)).thenThrow(new RuleNotFoundException("Rule not found with id: 99"));

        mockMvc.perform(get("/api/rules").param("status", "ACTIVE").param("param", "FOIR").param("decision", "Reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].op").value("gt"));
        mockMvc.perform(get("/api/rules/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT, PATCH status and DELETE")
    void testUpdateStatusDelete() throws Exception {
        when(ruleService.updateRule(eq(1L), any(RuleRequestDto.class), any())).thenReturn(sample);
        when(ruleService.changeStatus(eq(1L), eq("INACTIVE"), any())).thenReturn(sample);

        mockMvc.perform(put("/api/rule-engine/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"param\":\"FOIR\",\"op\":\"gt\",\"a\":55,\"decision\":\"Reject\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/rules/1/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/rules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rule removed"));
        verify(ruleService).deleteRule(1L);
    }
}
