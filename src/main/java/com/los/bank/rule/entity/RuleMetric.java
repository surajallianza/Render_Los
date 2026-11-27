package com.los.bank.rule.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Values the loan eligibility check calculates (or receives) itself. A "Rule Parameter"
 * lookup is linked to one of these by its name, e.g. "FOIR", "Debt to Income (DTI)",
 * "Loan-to-Value (LTV)", "CIBIL Score". Other parameters (Age, EMI Bounce, Firm Registration ...)
 * are custom: their value must be sent with the eligibility request.
 */
public enum RuleMetric {

    FOIR("%", true, List.of("FOIR", "FIXED OBLIGATION")),
    DTI("%", true, List.of("DTI", "DEBT TO INCOME")),
    LTV("%", true, List.of("LTV", "LOAN TO VALUE")),
    CIBIL_SCORE("", false, List.of("CIBIL", "CREDIT SCORE", "BUREAU SCORE"));

    private final String unit;
    /** True when the value grows with the loan amount (used to find the eligible amount). */
    private final boolean dependsOnLoanAmount;
    private final List<String> keywords;

    RuleMetric(String unit, boolean dependsOnLoanAmount, List<String> keywords) {
        this.unit = unit;
        this.dependsOnLoanAmount = dependsOnLoanAmount;
        this.keywords = keywords;
    }

    public String getUnit() { return unit; }
    public boolean isDependsOnLoanAmount() { return dependsOnLoanAmount; }

    /**
     * Matches a lookup name such as "Debt to Income (DTI)" or "Loan-to-Value (LTV)".
     */
    public static Optional<RuleMetric> fromLookupName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String text = " " + name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", " ").trim() + " ";
        return Arrays.stream(values())
                .filter(m -> m.keywords.stream().anyMatch(k -> text.contains(" " + k + " ")))
                .findFirst();
    }
}
