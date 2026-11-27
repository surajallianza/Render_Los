package com.los.bank.loanEligibility.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A rule checked during an eligibility check (table e003). The rule is copied as text, so
 * editing or deleting the rule or its lookups later does not change the audit trail.
 */
@Entity
@Table(name = "e003")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityRuleResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private EligibilityAssessment assessment;

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "param_name", length = 100, nullable = false)
    private String paramName;

    @Column(name = "rule_text", length = 255, nullable = false)
    private String ruleText;

    @Column(name = "decision_name", length = 50, nullable = false)
    private String decisionName;

    @Column(name = "rate_offset", precision = 5, scale = 2)
    private BigDecimal rateOffset;

    @Column(name = "actual_value", length = 100)
    private String actualValue;

    /** ALLOWED, OVERRIDDEN, REFERRED, REJECTED, NOT_MATCHED or NOT_APPLICABLE. */
    @Column(name = "result", length = 20, nullable = false)
    private String result;

    @Column(name = "message", length = 255)
    private String message;
}
