package com.los.bank.rule.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating or updating a business rule in the Rule Engine module.
 * Preserves the exact request field names:
 * status, param, comparisonOperator, from, to, value, actionOnMatch, offset.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class RuleRequestDto {

    @JsonProperty("status")
    private String status;

    @JsonProperty("param")
    private String param;

    @JsonProperty("comparisonOperator")
    private String comparisonOperator;

    @JsonProperty("from")
    private String from;

    @JsonProperty("to")
    private String to;

    @JsonProperty("value")
    private String value;

    @JsonProperty("actionOnMatch")
    private String actionOnMatch;

    @JsonProperty("offset")
    private Integer offset;
}
