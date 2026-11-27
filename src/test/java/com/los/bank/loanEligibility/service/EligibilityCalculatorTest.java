package com.los.bank.loanEligibility.service;

import com.los.bank.loanEligibility.dto.EligibilityRequest;
import com.los.bank.loanEligibility.dto.EligibilityRequest.Row;
import com.los.bank.loanEligibility.exception.EligibilityValidationException;
import com.los.bank.rule.entity.RuleMetric;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EligibilityCalculatorTest {

    private final EligibilityCalculator calculator = new EligibilityCalculator();

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static EligibilityRequest.EligibilityRequestBuilder screen() {
        return EligibilityRequest.builder()
                .requestedAmount(bd("1000000")).annualInterestRate(bd("12")).tenureMonths(60)
                .existingMonthlyDebt(bd("10000")).cibilScore(750)
                .rows(List.of(Row.builder().type("Income").source("Salary").amount(bd("100000")).build(),
                        Row.builder().type("Expense").source("Rent").amount(bd("20000")).build()));
    }

    @Test
    @DisplayName("EMI of 10,00,000 at 12% for 60 months = 22,244.45")
    void testEmi() {
        assertEquals(bd("22244.45"), EligibilityCalculator.emi(bd("1000000"), bd("12"), 60));
        assertEquals(bd("10000.00"), EligibilityCalculator.emi(bd("120000"), BigDecimal.ZERO, 12));
    }

    @Test
    @DisplayName("FOIR = (debt + EMI) / (income - expenses); DTI = (debt + EMI) / income")
    void testFoirDti() {
        EligibilityCalculator.Inputs in = calculator.prepare(screen().build());

        assertEquals(bd("100000.00"), in.totalIncome());
        assertEquals(bd("20000.00"), in.totalExpenses());
        assertEquals(bd("80000.00"), in.netIncome());
        // (10,000 + 22,244.45) / 80,000 = 40.31%
        assertEquals(bd("40.31"), calculator.foir(in, bd("1000000")));
        // (10,000 + 22,244.45) / 1,00,000 = 32.24%
        assertEquals(bd("32.24"), calculator.dti(in, bd("1000000")));
    }

    @Test
    @DisplayName("LTV = loan / security value; not calculated without a security value")
    void testLtv() {
        EligibilityCalculator.Inputs secured = calculator.prepare(screen().requestedAmount(bd("4000000")).securityValue(bd("5000000")).build());
        assertEquals(bd("80.00"), calculator.ltv(secured, bd("4000000")));

        EligibilityCalculator.Inputs unsecured = calculator.prepare(screen().build());
        Map<RuleMetric, BigDecimal> values = calculator.metrics(unsecured, unsecured.requestedAmount());
        assertFalse(values.containsKey(RuleMetric.LTV));
        assertEquals(bd("750"), values.get(RuleMetric.CIBIL_SCORE));
    }

    @Test
    @DisplayName("The screen's snake_case payload: income_expenses, calculated_metrics.existing_monthly_debt")
    void testLegacyPayload() {
        EligibilityRequest request = EligibilityRequest.builder()
                .requestedAmount(bd("800000")).annualInterestRate(bd("10.5")).tenureMonths(60).cibilScore(710)
                .securityValue(bd("1000000"))
                .rows(List.of(Row.builder().type("income").amount(bd("90000")).build(),
                        Row.builder().type("expense").amount(bd("25000")).build()))
                .calculatedMetrics(EligibilityRequest.CalculatedMetrics.builder().existingMonthlyDebt(bd("8000")).build())
                .build();

        EligibilityCalculator.Inputs in = calculator.prepare(request);

        assertEquals(bd("65000.00"), in.netIncome());
        assertEquals(bd("8000.00"), in.existingMonthlyDebt());
    }

    @Test
    @DisplayName("Validation: required fields, no income, expenses >= income, bad row type")
    void testValidation() {
        EligibilityValidationException missing = assertThrows(EligibilityValidationException.class,
                () -> calculator.prepare(EligibilityRequest.builder().requestedAmount(BigDecimal.ZERO).build()));
        assertTrue(missing.getErrors().keySet().containsAll(
                List.of("requestedAmount", "annualInterestRate", "tenureMonths", "cibilScore", "income")));

        EligibilityValidationException expenses = assertThrows(EligibilityValidationException.class,
                () -> calculator.prepare(screen().rows(List.of(
                        Row.builder().type("Income").amount(bd("30000")).build(),
                        Row.builder().type("Expense").amount(bd("30000")).build())).build()));
        assertTrue(expenses.getErrors().containsKey("rows"));

        EligibilityValidationException type = assertThrows(EligibilityValidationException.class,
                () -> calculator.prepare(screen().rows(List.of(
                        Row.builder().type("Income").amount(bd("30000")).build(),
                        Row.builder().type("Loan").amount(bd("1000")).build())).build()));
        assertTrue(type.getErrors().containsKey("rows[1].type"));
    }
}
