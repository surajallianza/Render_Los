package com.los.bank.rule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response payload representing a configured business rule in the Rule Engine module.
 * Preserves all rule fields, including range boundaries ('from', 'to'), single 'value',
 * operator, status, actionOnMatch, and offset.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class RuleResponseDto {

    @JsonProperty("id")
    private Long id;

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

    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
