package com.los.bank.rule.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * Everything the Rule Engine screen needs for its dropdowns, read from Lookup Master.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleOptionsDto {

    /** "Check" dropdown. */
    private List<Parameter> parameters;
    /** "When value is" dropdown. */
    private List<Operator> operators;
    /** "Then" dropdown. */
    private List<Decision> decisions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Parameter {
        private Long id;
        private String code;
        private String name;
        private String unit;
        /** FOIR, DTI, LTV or CIBIL_SCORE when the eligibility check works it out; null for custom parameters. */
        private String metric;
        /** True for choice parameters (e.g. Firm Registration): pick one of {@link #options}, operator "Is". */
        private boolean choice;
        private List<RuleLookupValue> options;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Operator {
        private Long id;
        private String code;
        private String name;
        /** LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, BETWEEN or EQUALS; null when the name is not understood. */
        private String type;
        /** Screen code: lt, lte, gt, gte, between, eq. */
        private String shortCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Decision {
        private Long id;
        private String code;
        private String name;
        /** ALLOW, OVERRIDE, REFER or REJECT; null when the name is not understood. */
        private String type;
        /** True when the screen must ask for "Add to rate (%)". */
        private boolean needsRateOffset;
    }
}
