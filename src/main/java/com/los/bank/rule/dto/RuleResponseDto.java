package com.los.bank.rule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A rule, with lookup values as { id, code, name }. The flat fields param / op / a / b / decision /
 * offset / active / effectiveDate match the Rule Engine screen's rule list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleResponseDto {
    private Long id;

    // ---- screen fields -------------------------------------------------
    /** Parameter name, e.g. "CIBIL Score". */
    private String param;
    /** lt, lte, gt, gte, between or eq. */
    private String op;
    /** Value, From, or the choice option name. */
    private String a;
    /** To (Between only). */
    private String b;
    /** Decision name, e.g. "Override". */
    private String decision;
    /** Rate offset % (Override). */
    private BigDecimal offset;
    private boolean active;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    // ---- full details --------------------------------------------------
    private RuleLookupValue parameter;
    private RuleLookupValue operator;
    private RuleLookupValue decisionLookup;
    /** Choice option for choice parameters. */
    private RuleLookupValue choiceValue;
    /** FOIR, DTI, LTV, CIBIL_SCORE or null (custom parameter). */
    private String metric;
    private String unit;
    private String operatorType;
    private String decisionType;
    private BigDecimal value;
    private BigDecimal from;
    private BigDecimal to;
    private String message;
    /** "If FOIR is above 50%, reject the application." */
    private String description;
    private String status;
    /** ACTIVE and effective date reached. */
    private boolean inForce;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
