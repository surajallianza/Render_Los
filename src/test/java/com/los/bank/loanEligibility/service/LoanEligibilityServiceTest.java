package com.los.bank.loanEligibility.service;

import com.los.bank.loanEligibility.dto.EligibilityRequest;
import com.los.bank.loanEligibility.dto.EligibilityRequest.Row;
import com.los.bank.loanEligibility.dto.EligibilityResponse;
import com.los.bank.loanEligibility.entity.EligibilityAssessment;
import com.los.bank.loanEligibility.repository.EligibilityAssessmentRepository;
import com.los.bank.rule.dto.RuleRequestDto;
import com.los.bank.rule.entity.Rule;
import com.los.bank.rule.repository.RuleRepository;
import com.los.bank.rule.service.RuleEvaluator;
import com.los.bank.rule.service.RuleServiceImpl;
import com.los.bank.rule.service.RuleTestLookups;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Eligibility with real rules: rules are created through RuleServiceImpl on in-memory lookups.
 */
class LoanEligibilityServiceTest {

    private final List<Rule> rules = new ArrayList<>();
    private RuleServiceImpl ruleService;
    private EligibilityAssessmentRepository assessmentRepository;
    private LoanEligibilityServiceImpl service;
    private final EligibilityCalculator calculator = new EligibilityCalculator();

    @BeforeEach
    void setUp() {
        RuleTestLookups lookups = new RuleTestLookups();
        RuleRepository ruleRepository = mock(RuleRepository.class);
        when(ruleRepository.save(any(Rule.class))).thenAnswer(inv -> {
            Rule r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId((long) rules.size() + 1);
                rules.add(r);
            }
            return r;
        });
        when(ruleRepository.findByParamLookupIdAndStatus(anyLong(), anyString())).thenAnswer(inv -> rules.stream()
                .filter(r -> r.getParamLookupId().equals(inv.getArgument(0)) && r.getStatus().equals(inv.getArgument(1))).toList());
        when(ruleRepository.findRulesInForce(any())).thenAnswer(inv -> new ArrayList<>(rules));
        ruleService = new RuleServiceImpl(ruleRepository, lookups.service);

        assessmentRepository = mock(EligibilityAssessmentRepository.class);
        when(assessmentRepository.save(any(EligibilityAssessment.class))).thenAnswer(inv -> {
            EligibilityAssessment a = inv.getArgument(0);
            a.setId(500L);
            return a;
        });
        service = new LoanEligibilityServiceImpl(ruleService, new RuleEvaluator(), calculator, assessmentRepository);

        rule("CIBIL Score", "Below", "650", null, "Reject", null);
        rule("CIBIL Score", "Between", "650", "700", "Override", "2");
        rule("FOIR", "Above", "50", null, "Reject", null);
        rule("Debt to Income (DTI)", "Above", "60", null, "Reject", null);
        rule("Loan-to-Value (LTV)", "Above", "80", null, "Reject", null);
    }

    private void rule(String param, String op, String a, String b, String decision, String offset) {
        RuleRequestDto request = RuleRequestDto.builder().param(param).op(op).a(a).b(b).decision(decision)
                .offset(offset == null ? null : new BigDecimal(offset)).build();
        ruleService.createRule(request, 1L);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    /** Home loan: income 1,00,000, expenses 20,000, debt 10,000, security 50 L, 9.5% for 240 months. */
    private static EligibilityRequest.EligibilityRequestBuilder home(String amount, int cibil) {
        return EligibilityRequest.builder()
                .leadId("LEAD1").applicantName("Rajesh Sharma").productName("Home loan")
                .requestedAmount(bd(amount)).annualInterestRate(bd("9.5")).tenureMonths(240)
                .securityValue(bd("5000000")).existingMonthlyDebt(bd("10000")).cibilScore(cibil)
                .rows(List.of(Row.builder().type("Income").source("Salary").amount(bd("100000")).build(),
                        Row.builder().type("Expense").source("Rent").amount(bd("20000")).build()));
    }

    private static String status(EligibilityResponse r, String code) {
        return r.getRatios().stream().filter(x -> x.getCode().equals(code)).findFirst().orElseThrow().getStatus();
    }

    @Test
    @DisplayName("All within limit: Eligible for the requested amount, rule results saved")
    void testEligible() {
        EligibilityResponse r = service.assess(home("3000000", 760).build(), 9L);

        assertEquals("Eligible", r.getDecision());
        assertEquals("ELIGIBLE", r.getDecisionCode());
        assertTrue(r.isEligible());
        assertEquals(0, bd("3000000").compareTo(r.getEligibleAmount()));
        assertEquals(bd("60.00"), r.getLtv());
        assertEquals("Within limit", status(r, "FOIR"));
        assertEquals("Within limit", status(r, "CIBIL_SCORE"));
        assertEquals(0, bd("9.50").compareTo(r.getFinalInterestRate()));

        ArgumentCaptor<EligibilityAssessment> saved = ArgumentCaptor.forClass(EligibilityAssessment.class);
        verify(assessmentRepository).save(saved.capture());
        assertEquals(5, saved.getValue().getRuleResults().size());
        assertEquals(9L, saved.getValue().getCreatedBy());
    }

    @Test
    @DisplayName("Override: CIBIL 680 adds +2% to the rate; EMI uses the final rate")
    void testOverride() {
        EligibilityResponse r = service.assess(home("3000000", 680).build(), 9L);

        assertEquals("Eligible", r.getDecision());
        assertEquals(0, bd("2").compareTo(r.getRateOffset()));
        assertEquals(0, bd("11.5").compareTo(r.getFinalInterestRate()));
        assertEquals("Rate +2%", status(r, "CIBIL_SCORE"));
        assertEquals(EligibilityCalculator.emi(bd("3000000"), bd("11.50"), 240), r.getEligibleEmi());
        assertTrue(r.getReasons().get(0).contains("+2%"));
    }

    @Test
    @DisplayName("CIBIL below 650: Not Eligible, eligible amount 0")
    void testCibilReject() {
        EligibilityResponse r = service.assess(home("3000000", 600).build(), 9L);

        assertEquals("Not Eligible", r.getDecision());
        assertEquals(0, r.getEligibleAmount().signum());
        assertEquals("CIBIL Score 600 is below 650 (not allowed)", r.getReasons().get(0));
        assertEquals(r.getReasons().get(0), r.getMessage());
        assertEquals("Outside limit", status(r, "CIBIL_SCORE"));
    }

    @Test
    @DisplayName("FOIR too high: Refer for Review with the highest amount within 50% FOIR")
    void testFoirReducesAmount() {
        EligibilityResponse r = service.assess(home("4000000", 760).build(), 9L);

        assertEquals("Refer for Review", r.getDecision());
        BigDecimal eligible = r.getEligibleAmount();
        EligibilityCalculator.Inputs in = calculator.prepare(home("4000000", 760).build());
        assertTrue(calculator.foir(in, eligible).compareTo(bd("50")) <= 0);
        assertTrue(calculator.foir(in, eligible.add(bd("1000"))).compareTo(bd("50")) > 0);
        assertEquals(0, eligible.remainder(bd("1000")).signum());
        assertTrue(r.getReasons().get(1).contains("indicative eligible amount of Rs. 32,18,000"), r.getReasons().get(1));
        assertEquals("Outside limit", status(r, "FOIR"));
    }

    @Test
    @DisplayName("LTV 90%: eligible up to 80% of the security value")
    void testLtvCap() {
        EligibilityResponse r = service.assess(home("4500000", 760)
                .rows(List.of(Row.builder().type("Income").amount(bd("400000")).build())).build(), 9L);

        assertEquals("Refer for Review", r.getDecision());
        assertEquals(0, bd("4000000").compareTo(r.getEligibleAmount()));
    }

    @Test
    @DisplayName("No security value: LTV not applicable, with a warning")
    void testUnsecured() {
        EligibilityResponse r = service.assess(home("1000000", 760).securityValue(null).build(), 9L);

        assertEquals("Eligible", r.getDecision());
        assertNull(r.getLtv());
        assertEquals("Not applicable", status(r, "LTV"));
        assertTrue(r.getWarnings().stream().anyMatch(w -> w.contains("LTV rules were not checked")));
    }

    @Test
    @DisplayName("Custom parameter rules use additionalFacts; missing facts give a warning")
    void testAdditionalFacts() {
        rule("Firm Registration", null, "Not Registered", null, "Override", "1");
        rule("Age", "Above", "60", null, "Reject", null);

        EligibilityResponse noFacts = service.assess(home("1000000", 760).build(), 9L);
        assertEquals("Eligible", noFacts.getDecision());
        assertTrue(noFacts.getWarnings().stream().anyMatch(w -> w.contains("Firm Registration") && w.contains("Age")));

        EligibilityResponse withFacts = service.assess(home("1000000", 760)
                .additionalFacts(Map.of("Firm Registration", "Not Registered", "age", "45")).build(), 9L);
        assertEquals(0, bd("1").compareTo(withFacts.getRateOffset()));

        EligibilityResponse tooOld = service.assess(home("1000000", 760).additionalFacts(Map.of("Age", "62")).build(), 9L);
        assertEquals("Not Eligible", tooOld.getDecision(), "age does not depend on the amount");
    }

    @Test
    @DisplayName("No rules: Eligible with a warning")
    void testNoRules() {
        rules.clear();

        EligibilityResponse r = service.assess(home("4000000", 500).build(), 9L);

        assertEquals("Eligible", r.getDecision());
        assertEquals("No rule", status(r, "FOIR"));
        assertTrue(r.getWarnings().stream().anyMatch(w -> w.contains("No active rules")));
    }

    @Test
    @DisplayName("Indian number format for amounts in reasons")
    void testMoneyFormat() {
        assertEquals("40,00,000", LoanEligibilityServiceImpl.money(bd("4000000")));
        assertEquals("12,34,567.50", LoanEligibilityServiceImpl.money(bd("1234567.5")));
        assertEquals("999", LoanEligibilityServiceImpl.money(bd("999")));
    }
}
