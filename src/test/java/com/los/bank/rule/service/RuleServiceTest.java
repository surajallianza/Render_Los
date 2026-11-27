package com.los.bank.rule.service;

import com.los.bank.rule.dto.RuleRequestDto;
import com.los.bank.rule.dto.RuleResponseDto;
import com.los.bank.rule.entity.Rule;
import com.los.bank.rule.exception.RuleNotFoundException;
import com.los.bank.rule.exception.RuleValidationException;
import com.los.bank.rule.repository.RuleRepository;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    @Mock
    private RuleRepository ruleRepository;

    @InjectMocks
    private RuleServiceImpl ruleService;

    private Rule sampleRule;
    private RuleRequestDto validBetweenRequest;
    private RuleRequestDto validSingleValueRequest;

    @BeforeEach
    void setUp() {
        sampleRule = Rule.builder()
                .id(1L)
                .status("active")
                .param("CIBIL Score")
                .comparisonOperator("between")
                .fromValue("110")
                .toValue("250")
                .val(null)
                .actionOnMatch("Allow")
                .executionOffset(0)
                .createdAt(LocalDateTime.of(2026, 11, 21, 10, 0, 0))
                .build();

        validBetweenRequest = RuleRequestDto.builder()
                .status("active")
                .param("CIBIL Score")
                .comparisonOperator("between")
                .from("110")
                .to("250")
                .value(null)
                .actionOnMatch("Allow")
                .offset(0)
                .build();

        validSingleValueRequest = RuleRequestDto.builder()
                .status("active")
                .param("CIBIL Score")
                .comparisonOperator("greater_than")
                .from(null)
                .to(null)
                .value("750")
                .actionOnMatch("Allow")
                .offset(1)
                .build();
    }

    // =========================================================================
    // CREATE RULE TESTS
    // =========================================================================

    @Test
    @DisplayName("Create valid 'between' rule should succeed and persist all fields")
    void testCreateRule_BetweenOperator_Success() {
        when(ruleRepository.existsByParamIgnoreCaseAndComparisonOperatorIgnoreCaseAndFromValueAndToValueAndValAndStatusIgnoreCase(
                anyString(), anyString(), any(), any(), any(), anyString())).thenReturn(false);
        when(ruleRepository.save(any(Rule.class))).thenReturn(sampleRule);

        RuleResponseDto response = ruleService.createRule(validBetweenRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("active", response.getStatus());
        assertEquals("CIBIL Score", response.getParam());
        assertEquals("between", response.getComparisonOperator());
        assertEquals("110", response.getFrom());
        assertEquals("250", response.getTo());
        assertNull(response.getValue());
        assertEquals("Allow", response.getActionOnMatch());
        assertEquals(0, response.getOffset());

        verify(ruleRepository, times(1)).save(any(Rule.class));
    }

    @Test
    @DisplayName("Create valid single-value rule should succeed and persist single value")
    void testCreateRule_SingleValueOperator_Success() {
        Rule singleRule = Rule.builder()
                .id(2L)
                .status("active")
                .param("CIBIL Score")
                .comparisonOperator("greater_than")
                .fromValue(null)
                .toValue(null)
                .val("750")
                .actionOnMatch("Allow")
                .executionOffset(1)
                .build();

        when(ruleRepository.existsByParamIgnoreCaseAndComparisonOperatorIgnoreCaseAndFromValueAndToValueAndValAndStatusIgnoreCase(
                anyString(), anyString(), any(), any(), any(), anyString())).thenReturn(false);
        when(ruleRepository.save(any(Rule.class))).thenReturn(singleRule);

        RuleResponseDto response = ruleService.createRule(validSingleValueRequest);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("greater_than", response.getComparisonOperator());
        assertEquals("750", response.getValue());
        assertNull(response.getFrom());
        assertNull(response.getTo());
    }

    @Test
    @DisplayName("Create rule with invalid range ('from' > 'to') should throw RuleValidationException")
    void testCreateRule_FromGreaterThanTo_ThrowsException() {
        validBetweenRequest.setFrom("300");
        validBetweenRequest.setTo("200");

        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.createRule(validBetweenRequest));

        assertTrue(ex.getMessage().contains("Lower boundary ('from') cannot be greater than upper boundary ('to')"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create rule with missing 'from' boundary for 'between' should throw RuleValidationException")
    void testCreateRule_MissingFromBoundary_ThrowsException() {
        validBetweenRequest.setFrom(null);

        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.createRule(validBetweenRequest));

        assertTrue(ex.getMessage().contains("Lower boundary ('from') is required"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create rule with missing 'to' boundary for 'between' should throw RuleValidationException")
    void testCreateRule_MissingToBoundary_ThrowsException() {
        validBetweenRequest.setTo("");

        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.createRule(validBetweenRequest));

        assertTrue(ex.getMessage().contains("Upper boundary ('to') is required"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create rule with non-numeric value for CIBIL Score should throw RuleValidationException")
    void testCreateRule_NonNumericCibilScore_ThrowsException() {
        validBetweenRequest.setFrom("abc");

        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.createRule(validBetweenRequest));

        assertTrue(ex.getMessage().contains("must be a valid numeric value"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create rule with missing 'value' for single-value operator should throw RuleValidationException")
    void testCreateRule_MissingValueForSingleValueOperator_ThrowsException() {
        validSingleValueRequest.setValue(null);

        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.createRule(validSingleValueRequest));

        assertTrue(ex.getMessage().contains("Comparison value ('value') is required"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create rule with unsupported comparison operator should throw RuleValidationException")
    void testCreateRule_UnsupportedOperator_ThrowsException() {
        validBetweenRequest.setComparisonOperator("LIKE");

        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.createRule(validBetweenRequest));

        assertTrue(ex.getMessage().contains("Unsupported comparison operator"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create rule with unsupported action should throw RuleValidationException")
    void testCreateRule_UnsupportedAction_ThrowsException() {
        validBetweenRequest.setActionOnMatch("INVALID_ACTION");

        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.createRule(validBetweenRequest));

        assertTrue(ex.getMessage().contains("Unsupported action on match"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create rule with invalid status should throw RuleValidationException")
    void testCreateRule_InvalidStatus_ThrowsException() {
        validBetweenRequest.setStatus("PENDING");

        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.createRule(validBetweenRequest));

        assertTrue(ex.getMessage().contains("Invalid status"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create rule with negative offset should throw RuleValidationException")
    void testCreateRule_NegativeOffset_ThrowsException() {
        validBetweenRequest.setOffset(-1);

        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.createRule(validBetweenRequest));

        assertTrue(ex.getMessage().contains("must be greater than or equal to 0"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create duplicate rule should throw RuleValidationException")
    void testCreateRule_DuplicateRule_ThrowsException() {
        when(ruleRepository.existsByParamIgnoreCaseAndComparisonOperatorIgnoreCaseAndFromValueAndToValueAndValAndStatusIgnoreCase(
                anyString(), anyString(), any(), any(), any(), anyString())).thenReturn(true);

        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.createRule(validBetweenRequest));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create rule with null request should throw RuleValidationException")
    void testCreateRule_NullRequest_ThrowsException() {
        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.createRule(null));

        assertTrue(ex.getMessage().contains("Request payload cannot be null"));
    }

    @Test
    @DisplayName("Create rule with blank parameter should throw RuleValidationException")
    void testCreateRule_BlankParam_ThrowsException() {
        validBetweenRequest.setParam("   ");

        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.createRule(validBetweenRequest));

        assertTrue(ex.getMessage().contains("Parameter ('param') is required"));
    }

    // =========================================================================
    // GET ALL RULES TESTS
    // =========================================================================

    @Test
    @DisplayName("Get all rules returns paginated results with all rule attributes")
    void testGetAllRules_Success() {
        Page<Rule> page = new PageImpl<>(Collections.singletonList(sampleRule));
        when(ruleRepository.findWithFilters(isNull(), isNull(), any(Pageable.class))).thenReturn(page);

        Map<String, Object> result = ruleService.getAllRules(0, 10, null, null);

        assertTrue((Boolean) result.get("success"));
        assertEquals(1L, result.get("totalElements"));
        assertEquals(1, result.get("totalPages"));
        assertEquals(0, result.get("currentPage"));

        @SuppressWarnings("unchecked")
        java.util.List<RuleResponseDto> data = (java.util.List<RuleResponseDto>) result.get("data");
        assertEquals(1, data.size());
        assertEquals("CIBIL Score", data.get(0).getParam());
        assertEquals("110", data.get(0).getFrom());
        assertEquals("250", data.get(0).getTo());
        assertNull(data.get(0).getValue());
        assertEquals("Allow", data.get(0).getActionOnMatch());
        assertEquals(0, data.get(0).getOffset());
    }

    @Test
    @DisplayName("Get all rules with status and param filter")
    void testGetAllRules_WithFilters() {
        Page<Rule> page = new PageImpl<>(Collections.singletonList(sampleRule));
        when(ruleRepository.findWithFilters(eq("active"), eq("CIBIL"), any(Pageable.class))).thenReturn(page);

        Map<String, Object> result = ruleService.getAllRules(0, 10, "active", "CIBIL");

        assertTrue((Boolean) result.get("success"));
        assertEquals(1L, result.get("totalElements"));
    }

    // =========================================================================
    // GET RULE BY ID TESTS
    // =========================================================================

    @Test
    @DisplayName("Get rule by ID returns rule details when ID exists")
    void testGetRuleById_Success() {
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));

        RuleResponseDto result = ruleService.getRuleById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("CIBIL Score", result.getParam());
        assertEquals("between", result.getComparisonOperator());
        assertEquals("110", result.getFrom());
        assertEquals("250", result.getTo());
    }

    @Test
    @DisplayName("Get rule by nonexistent ID throws RuleNotFoundException")
    void testGetRuleById_NotFound_ThrowsException() {
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        RuleNotFoundException ex = assertThrows(RuleNotFoundException.class, () ->
                ruleService.getRuleById(999L));

        assertTrue(ex.getMessage().contains("Rule not found with id: 999"));
    }

    @Test
    @DisplayName("Get rule with null ID throws RuleValidationException")
    void testGetRuleById_NullId_ThrowsException() {
        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.getRuleById(null));

        assertTrue(ex.getMessage().contains("Rule ID cannot be null"));
    }

    // =========================================================================
    // UPDATE RULE TESTS
    // =========================================================================

    @Test
    @DisplayName("Update existing rule should succeed and update all fields")
    void testUpdateRule_Success() {
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleRequestDto updateDto = RuleRequestDto.builder()
                .status("inactive")
                .param("CIBIL Score")
                .comparisonOperator("between")
                .from("200")
                .to("400")
                .value(null)
                .actionOnMatch("Reject")
                .offset(2)
                .build();

        RuleResponseDto updated = ruleService.updateRule(1L, updateDto);

        assertNotNull(updated);
        assertEquals("inactive", updated.getStatus());
        assertEquals("200", updated.getFrom());
        assertEquals("400", updated.getTo());
        assertEquals("Reject", updated.getActionOnMatch());
        assertEquals(2, updated.getOffset());
        verify(ruleRepository, times(1)).save(sampleRule);
    }

    @Test
    @DisplayName("Update nonexistent rule throws RuleNotFoundException")
    void testUpdateRule_NotFound_ThrowsException() {
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        RuleNotFoundException ex = assertThrows(RuleNotFoundException.class, () ->
                ruleService.updateRule(999L, validBetweenRequest));

        assertTrue(ex.getMessage().contains("Rule not found with id: 999"));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update rule with invalid range should throw RuleValidationException")
    void testUpdateRule_InvalidRange_ThrowsException() {
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));

        validBetweenRequest.setFrom("500");
        validBetweenRequest.setTo("100");

        RuleValidationException ex = assertThrows(RuleValidationException.class, () ->
                ruleService.updateRule(1L, validBetweenRequest));

        assertTrue(ex.getMessage().contains("Lower boundary ('from') cannot be greater than upper boundary ('to')"));
        verify(ruleRepository, never()).save(any());
    }
}
