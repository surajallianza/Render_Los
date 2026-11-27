package com.los.bank.loanEligibility.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of a loan eligibility check, with the fields the Loan Eligibility screen shows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EligibilityResponse {

    private Long assessmentId;
    private String leadId;
    private String applicantName;
    private String productName;

    /** "Eligible", "Refer for Review" or "Not Eligible" (as shown on the screen). */
    private String decision;
    /** ELIGIBLE, REFER_FOR_REVIEW or NOT_ELIGIBLE. */
    private String decisionCode;
    private boolean eligible;
    /** One-line summary, e.g. the first reason. */
    private String message;

    private BigDecimal requestedAmount;
    private Integer tenureMonths;
    /** Rate entered on the screen. */
    private BigDecimal annualInterestRate;
    /** Sum of the Override rules' rate offsets. */
    private BigDecimal rateOffset;
    /** annualInterestRate + rateOffset. */
    private BigDecimal finalInterestRate;

    /** "Indicative eligible amount": the requested amount, a lower amount that passes, or 0. */
    private BigDecimal eligibleAmount;
    /** EMI of the eligible amount at the final interest rate. */
    private BigDecimal eligibleEmi;
    /** EMI of the requested amount at the entered rate (used in FOIR / DTI). */
    private BigDecimal proposedEmi;

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netIncome;
    private BigDecimal existingMonthlyDebt;
    private BigDecimal securityValue;

    private BigDecimal foir;
    private BigDecimal dti;
    /** Null when no security value was given. */
    private BigDecimal ltv;
    private Integer cibilScore;

    /** One row per ratio for the "Financial ratio / Result / Status" table. */
    private List<Ratio> ratios;

    private List<String> reasons;
    private List<String> warnings;
    private List<RuleResultDto> ruleResults;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime assessedAt;
    private Long assessedBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Ratio {
        /** FOIR, DTI, LTV or CIBIL_SCORE. */
        private String code;
        private String name;
        private BigDecimal value;
        /** Within limit, Rate +x%, Refer for review, Outside limit, Not applicable, No rule. */
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RuleResultDto {
        private Long ruleId;
        private String param;
        /** "If FOIR is above 50%, reject the application." */
        private String rule;
        private String decision;
        private BigDecimal rateOffset;
        private String actualValue;
        /** ALLOWED, OVERRIDDEN, REFERRED, REJECTED, NOT_MATCHED or NOT_APPLICABLE. */
        private String result;
        private String message;
    }
}
