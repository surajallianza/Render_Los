package com.los.bank.rule.service;

import com.los.bank.rule.dto.RuleOptionsDto;
import com.los.bank.rule.dto.RuleRequestDto;
import com.los.bank.rule.dto.RuleResponseDto;
import com.los.bank.rule.entity.Rule;
import com.los.bank.rule.exception.RuleNotFoundException;
import com.los.bank.rule.exception.RuleValidationException;
import com.los.bank.rule.repository.RuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RuleServiceTest {

    private RuleTestLookups lookups;
    private RuleRepository ruleRepository;
    private RuleServiceImpl ruleService;
    private final List<Rule> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        lookups = new RuleTestLookups();
        ruleRepository = mock(RuleRepository.class);
        when(ruleRepository.save(any(Rule.class))).thenAnswer(inv -> {
            Rule r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId((long) saved.size() + 1);
                saved.add(r);
            }
            return r;
        });
        when(ruleRepository.findByParamLookupIdAndStatus(anyLong(), anyString())).thenAnswer(inv -> saved.stream()
                .filter(r -> r.getParamLookupId().equals(inv.getArgument(0)) && r.getStatus().equals(inv.getArgument(1)))
                .toList());
        when(ruleRepository.findById(anyLong())).thenAnswer(inv ->
                saved.stream().filter(r -> r.getId().equals(inv.getArgument(0))).findFirst());
        when(ruleRepository.findAll()).thenAnswer(inv -> new ArrayList<>(saved));
        ruleService = new RuleServiceImpl(ruleRepository, lookups.service);
    }

    private static RuleRequestDto req(String param, String op, String a, String decision) {
        return RuleRequestDto.builder().param(param).op(op).a(a).decision(decision).build();
    }

    @Test
    @DisplayName("Options come from the three lookups; metrics and choice parameters are recognised")
    void testOptions() {
        RuleOptionsDto options = ruleService.getOptions();

        assertEquals(6, options.getParameters().size());
        assertEquals("FOIR", options.getParameters().get(0).getMetric());
        assertEquals("DTI", options.getParameters().get(1).getMetric());
        assertEquals("LTV", options.getParameters().get(2).getMetric());
        assertEquals("CIBIL_SCORE", options.getParameters().get(3).getMetric());
        assertNull(options.getParameters().get(4).getMetric(), "Age is a custom parameter");
        RuleOptionsDto.Parameter firm = options.getParameters().get(5);
        assertTrue(firm.isChoice());
        assertEquals(List.of("Registered", "Not Registered"), firm.getOptions().stream().map(o -> o.getName()).toList());
        assertEquals(List.of("lt", "between", "gt", "eq"), options.getOperators().stream().map(RuleOptionsDto.Operator::getShortCode).toList());
        assertTrue(options.getDecisions().stream().anyMatch(d -> "Override".equals(d.getName()) && d.isNeedsRateOffset()));
    }

    @Test
    @DisplayName("Create a rule with lookup codes (as the dropdowns send them)")
    void testCreateWithCodes() {
        RuleResponseDto resp = ruleService.createRule(
                req(lookups.code("FOIR"), lookups.code("Above"), "50", lookups.code("Reject")), 5L);

        assertEquals("FOIR", resp.getParam());
        assertEquals("gt", resp.getOp());
        assertEquals("50", resp.getA());
        assertEquals("Reject", resp.getDecision());
        assertEquals("If FOIR is above 50%, reject the application.", resp.getDescription());
        assertTrue(resp.isActive());
        assertTrue(resp.isInForce());
        assertEquals(5L, resp.getCreatedBy());
        Rule stored = saved.get(0);
        assertEquals(lookups.id("FOIR"), stored.getParamLookupId());
        assertEquals(lookups.id("Above"), stored.getOperatorLookupId());
        assertEquals(lookups.id("Reject"), stored.getDecisionLookupId());
        assertEquals(0, BigDecimal.ZERO.compareTo(stored.getRateOffset()));
    }

    @Test
    @DisplayName("Names and the screen's short operator codes work too; Between uses a and b")
    void testCreateWithNamesBetweenOverride() {
        RuleRequestDto request = req("CIBIL Score", "between", "650", "Override");
        request.setB("700");
        request.setOffset(new BigDecimal("2"));

        RuleResponseDto resp = ruleService.createRule(request, 1L);

        assertEquals("between", resp.getOp());
        assertEquals("650", resp.getA());
        assertEquals("700", resp.getB());
        assertEquals(0, new BigDecimal("2").compareTo(resp.getOffset()));
        assertEquals("If CIBIL Score is between 650 and 700, allow it and add +2% to the interest rate.", resp.getDescription());
    }

    @Test
    @DisplayName("Choice parameter: option by name or code, operator defaults to Is")
    void testChoiceRule() {
        RuleRequestDto request = req("Firm Registration", null, "Not Registered", "Override");
        request.setOffset(new BigDecimal("1"));

        RuleResponseDto resp = ruleService.createRule(request, 1L);

        assertEquals("eq", resp.getOp());
        assertEquals("Not Registered", resp.getA());
        assertEquals(lookups.code("Not Registered"), resp.getChoiceValue().getCode());
        assertEquals("If Firm Registration is Not Registered, allow it and add +1% to the interest rate.", resp.getDescription());

        RuleValidationException wrong = assertThrows(RuleValidationException.class,
                () -> ruleService.createRule(req("Firm Registration", null, "Maybe", "Allow"), 1L));
        assertTrue(wrong.getErrors().get("a").contains("Registered, Not Registered"));

        RuleValidationException notIs = assertThrows(RuleValidationException.class,
                () -> ruleService.createRule(req("Firm Registration", "Above", "Registered", "Allow"), 1L));
        assertTrue(notIs.getErrors().containsKey("op"));
    }

    @Test
    @DisplayName("Validation: unknown lookups, Override without offset, From >= To, past date")
    void testValidation() {
        RuleValidationException ex = assertThrows(RuleValidationException.class,
                () -> ruleService.createRule(req("Shoe Size", "around", "9", "Maybe"), 1L));
        assertTrue(ex.getErrors().containsKey("param"));
        assertTrue(ex.getErrors().containsKey("op"));
        assertTrue(ex.getErrors().containsKey("decision"));

        assertTrue(assertThrows(RuleValidationException.class,
                () -> ruleService.createRule(req("Age", "Below", "21", "Override"), 1L)).getErrors().containsKey("offset"));

        RuleRequestDto between = req("FOIR", "Between", "60", "Reject");
        between.setB("40");
        assertTrue(assertThrows(RuleValidationException.class, () -> ruleService.createRule(between, 1L))
                .getErrors().get("a").contains("lower than"));

        RuleRequestDto past = req("Age", "Below", "21", "Reject");
        past.setEffectiveDate(LocalDate.now().minusDays(1));
        assertTrue(assertThrows(RuleValidationException.class, () -> ruleService.createRule(past, 1L))
                .getErrors().containsKey("effectiveDate"));

        assertTrue(assertThrows(RuleValidationException.class,
                () -> ruleService.createRule(req("CIBIL Score", "Below", "950", "Reject"), 1L)).getErrors().containsKey("a"));
        assertTrue(saved.isEmpty());
    }

    @Test
    @DisplayName("Overlapping active rules of one parameter are rejected; touching bands and other parameters are fine")
    void testOverlap() {
        ruleService.createRule(req("FOIR", "Above", "50", "Reject"), 1L);

        RuleRequestDto overlapping = req("FOIR", "Between", "45", "Reject");
        overlapping.setB("60");
        RuleValidationException ex = assertThrows(RuleValidationException.class, () -> ruleService.createRule(overlapping, 1L));
        assertTrue(ex.getMessage().contains("#1"));

        RuleRequestDto band = req("FOIR", "Between", "40", "Allow");
        band.setB("50");
        assertDoesNotThrow(() -> ruleService.createRule(band, 1L), "40-50 touches 'above 50' without overlapping");
        assertDoesNotThrow(() -> ruleService.createRule(req("Debt to Income (DTI)", "Above", "50", "Reject"), 1L));

        RuleRequestDto inactive = req("FOIR", "Above", "45", "Reject");
        inactive.setStatus("INACTIVE");
        assertDoesNotThrow(() -> ruleService.createRule(inactive, 1L));

        ruleService.createRule(req("Firm Registration", "Is", "Registered", "Allow"), 1L);
        assertThrows(RuleValidationException.class, () -> ruleService.createRule(req("Firm Registration", "Is", "Registered", "Reject"), 1L));
    }

    @Test
    @DisplayName("Update, deactivate, list filters, delete and not-found")
    void testUpdateStatusListDelete() {
        ruleService.createRule(req("FOIR", "Above", "50", "Reject"), 1L);
        RuleRequestDto override = req("CIBIL Score", "Below", "700", "Override");
        override.setOffset(BigDecimal.ONE);
        ruleService.createRule(override, 1L);

        RuleResponseDto updated = ruleService.updateRule(1L, req("FOIR", "Above", "55", "Reject"), 7L);
        assertEquals("55", updated.getA());
        assertEquals(7L, updated.getUpdatedBy());

        assertFalse(ruleService.changeStatus(1L, "inactive", 7L).isActive());

        Map<String, Object> overrides = ruleService.getAllRules(0, 50, null, null, "Override");
        assertEquals(1, overrides.get("totalElements"));
        Map<String, Object> active = ruleService.getAllRules(0, 50, "ACTIVE", null, null);
        assertEquals(1, active.get("totalElements"));
        Map<String, Object> foir = ruleService.getAllRules(0, 50, null, lookups.code("FOIR"), null);
        assertEquals(1, foir.get("totalElements"));

        ruleService.deleteRule(2L);
        verify(ruleRepository).delete(saved.get(1));
        assertThrows(RuleNotFoundException.class, () -> ruleService.getRuleById(99L));
    }

    @Test
    @DisplayName("A missing lookup category gives a clear setup message")
    void testMissingLookupCategory() {
        when(lookups.lookupRepository.findByNormalizedName("Rule Parameter")).thenReturn(List.of());

        RuleValidationException ex = assertThrows(RuleValidationException.class, () -> ruleService.getOptions());

        assertTrue(ex.getMessage().contains("Lookup 'Rule Parameter' is not configured"));
    }

    @Test
    @DisplayName("Rules in force are resolved with their lookup meaning")
    void testRulesInForce() {
        ruleService.createRule(req("FOIR", "Above", "50", "Reject"), 1L);
        when(ruleRepository.findRulesInForce(any())).thenReturn(List.of(saved.get(0)));

        List<ResolvedRule> rules = ruleService.getRulesInForce(LocalDate.now());

        assertEquals(1, rules.size());
        assertEquals("FOIR", rules.get(0).param().metric().name());
        assertTrue(rules.get(0).matches(new BigDecimal("50.01")));
        assertFalse(rules.get(0).matches(new BigDecimal("50")));
        assertEquals(Optional.of("REJECT"), Optional.of(rules.get(0).decision().name()));
    }
}
