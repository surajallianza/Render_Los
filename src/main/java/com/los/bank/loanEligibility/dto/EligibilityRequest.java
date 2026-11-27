package com.los.bank.loanEligibility.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loan eligibility check, with the fields of the Loan Eligibility screen.
 *
 * <pre>
 * {
 *   "leadId": "LEAD1001", "applicantName": "Rajesh Sharma", "productName": "Home loan",
 *   "requestedAmount": 3000000, "annualInterestRate": 9.5, "tenureMonths": 240,
 *   "securityValue": 5000000, "existingMonthlyDebt": 10000, "cibilScore": 760,
 *   "rows": [ { "type": "Income", "source": "Salary", "amount": 100000 },
 *             { "type": "Expense", "source": "Rent", "amount": 20000 } ]
 * }
 * </pre>
 * The snake_case payload the screen sends today (lead_id, customer_name, product, requested_amount,
 * income_expenses, security_value, cibil_score, calculated_metrics.existing_monthly_debt) works too.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityRequest {

    @JsonAlias({"lead_id"})
    private String leadId;

    @JsonAlias({"applicant_name", "customerName", "customer_name"})
    private String applicantName;

    @JsonAlias({"product_name", "product"})
    private String productName;

    @JsonAlias({"requested_amount", "loanAmount"})
    private BigDecimal requestedAmount;

    @JsonAlias({"annual_interest_rate", "interestRate"})
    private BigDecimal annualInterestRate;

    @JsonAlias({"tenure_months"})
    private Integer tenureMonths;

    /** Optional; LTV is calculated only when given. */
    @JsonAlias({"security_value"})
    private BigDecimal securityValue;

    /** Existing monthly debt (EMIs etc.); 0 when none. */
    @JsonAlias({"existing_monthly_debt"})
    private BigDecimal existingMonthlyDebt;

    @JsonAlias({"cibil_score"})
    private Integer cibilScore;

    /** Income / expense rows: type "Income" or "Expense", source, amount (monthly). */
    @JsonAlias({"incomeExpenses", "income_expenses"})
    @Builder.Default
    private List<Row> rows = new ArrayList<>();

    /** The screen's calculated_metrics block; only existing_monthly_debt is read from it. */
    @JsonAlias({"calculated_metrics"})
    private CalculatedMetrics calculatedMetrics;

    /**
     * Values for rule parameters that are not calculated here (Age, EMI Bounce, Firm Registration ...),
     * keyed by the parameter's lookup code or name. Rules on parameters without a value are not checked.
     */
    @JsonAlias({"additional_facts", "facts"})
    @Builder.Default
    private Map<String, String> additionalFacts = new LinkedHashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        /** "Income" or "Expense". */
        private String type;
        private String source;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculatedMetrics {
        @JsonAlias({"existing_monthly_debt"})
        private BigDecimal existingMonthlyDebt;
    }
}
