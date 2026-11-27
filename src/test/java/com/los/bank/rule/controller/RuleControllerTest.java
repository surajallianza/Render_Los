package com.los.bank.rule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
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
class RuleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RuleService ruleService;

    @InjectMocks
    private RuleController ruleController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RuleResponseDto sampleResponseDto;
    private RuleRequestDto sampleRequestDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ruleController)
                .setControllerAdvice(new RuleExceptionHandler())
                .build();

        sampleRequestDto = RuleRequestDto.builder()
                .status("active")
                .param("CIBIL Score")
                .comparisonOperator("between")
                .from("110")
                .to("250")
                .value(null)
                .actionOnMatch("Allow")
                .offset(0)
                .build();

        sampleResponseDto = RuleResponseDto.builder()
                .id(1L)
                .status("active")
                .param("CIBIL Score")
                .comparisonOperator("between")
                .from("110")
                .to("250")
                .value(null)
                .actionOnMatch("Allow")
                .offset(0)
                .createdAt(LocalDateTime.of(2026, 11, 21, 10, 0, 0))
                .build();
    }

    @Test
    @DisplayName("POST /api/rules creates rule and returns 201 CREATED with reference payload")
    void testCreateRule_Success() throws Exception {
        when(ruleService.createRule(any(RuleRequestDto.class))).thenReturn(sampleResponseDto);

        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Rule created successfully"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.data.param").value("CIBIL Score"))
                .andExpect(jsonPath("$.data.comparisonOperator").value("between"))
                .andExpect(jsonPath("$.data.from").value("110"))
                .andExpect(jsonPath("$.data.to").value("250"))
                .andExpect(jsonPath("$.data.actionOnMatch").value("Allow"))
                .andExpect(jsonPath("$.data.offset").value(0));
    }

    @Test
    @DisplayName("POST /api/rules returns 400 BAD REQUEST when validation fails")
    void testCreateRule_ValidationFailure() throws Exception {
        when(ruleService.createRule(any(RuleRequestDto.class)))
                .thenThrow(new RuleValidationException("Lower boundary ('from') cannot be greater than upper boundary ('to')"));

        mockMvc.perform(post("/api/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Lower boundary ('from') cannot be greater than upper boundary ('to')"));
    }

    @Test
    @DisplayName("GET /api/rules returns 200 OK with paginated list")
    void testGetAllRules_Success() throws Exception {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("success", true);
        mockResult.put("totalElements", 1L);
        mockResult.put("totalPages", 1);
        mockResult.put("currentPage", 0);
        mockResult.put("pageSize", 10);
        mockResult.put("data", Collections.singletonList(sampleResponseDto));

        when(ruleService.getAllRules(0, 10, null, null)).thenReturn(mockResult);

        mockMvc.perform(get("/api/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].param").value("CIBIL Score"))
                .andExpect(jsonPath("$.data[0].from").value("110"))
                .andExpect(jsonPath("$.data[0].to").value("250"));
    }

    @Test
    @DisplayName("GET /api/rules/{id} returns 200 OK when rule exists")
    void testGetRuleById_Success() throws Exception {
        when(ruleService.getRuleById(1L)).thenReturn(sampleResponseDto);

        mockMvc.perform(get("/api/rules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.param").value("CIBIL Score"))
                .andExpect(jsonPath("$.data.from").value("110"))
                .andExpect(jsonPath("$.data.to").value("250"))
                .andExpect(jsonPath("$.data.actionOnMatch").value("Allow"));
    }

    @Test
    @DisplayName("GET /api/rules/{id} returns 404 NOT FOUND when rule does not exist")
    void testGetRuleById_NotFound() throws Exception {
        when(ruleService.getRuleById(999L)).thenThrow(new RuleNotFoundException("Rule not found with id: 999"));

        mockMvc.perform(get("/api/rules/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Rule not found with id: 999"));
    }

    @Test
    @DisplayName("PUT /api/rules/{id} returns 200 OK when update succeeds")
    void testUpdateRule_Success() throws Exception {
        when(ruleService.updateRule(eq(1L), any(RuleRequestDto.class))).thenReturn(sampleResponseDto);

        mockMvc.perform(put("/api/rules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Rule updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/rules/{id} returns 404 NOT FOUND when updating nonexistent rule")
    void testUpdateRule_NotFound() throws Exception {
        when(ruleService.updateRule(eq(999L), any(RuleRequestDto.class)))
                .thenThrow(new RuleNotFoundException("Rule not found with id: 999"));

        mockMvc.perform(put("/api/rules/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Rule not found with id: 999"));
    }
}
