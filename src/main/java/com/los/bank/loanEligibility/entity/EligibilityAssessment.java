package com.los.bank.loanEligibility.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One loan eligibility check (table e002).
 */
@Entity
@Table(name = "e002")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_id", length = 50)
    private String leadId;

    @Column(name = "applicant_name", length = 150)
    private String applicantName;

    @Column(name = "product_name", length = 100)
    private String productName;

    @Column(name = "requested_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal requestedAmount;

    @Column(name = "annual_interest_rate", precision = 6, scale = 2, nullable = false)
    private BigDecimal annualInterestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "security_value", precision = 15, scale = 2)
    private BigDecimal securityValue;

    @Column(name = "existing_monthly_debt", precision = 15, scale = 2, nullable = false)
    private BigDecimal existingMonthlyDebt;

    @Column(name = "total_income", precision = 15, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "total_expenses", precision = 15, scale = 2)
    private BigDecimal totalExpenses;

    @Column(name = "net_income", precision = 15, scale = 2)
    private BigDecimal netIncome;

    @Column(name = "cibil_score")
    private Integer cibilScore;

    @Column(name = "proposed_emi", precision = 15, scale = 2)
    private BigDecimal proposedEmi;

    @Column(name = "foir", precision = 8, scale = 2)
    private BigDecimal foir;

    @Column(name = "dti", precision = 8, scale = 2)
    private BigDecimal dti;

    @Column(name = "ltv", precision = 8, scale = 2)
    private BigDecimal ltv;

    /** ELIGIBLE, REFER_FOR_REVIEW or NOT_ELIGIBLE. */
    @Column(name = "decision", length = 20, nullable = false)
    private String decision;

    @Column(name = "eligible_amount", precision = 15, scale = 2)
    private BigDecimal eligibleAmount;

    @Column(name = "rate_offset", precision = 5, scale = 2)
    private BigDecimal rateOffset;

    @Column(name = "final_interest_rate", precision = 6, scale = 2)
    private BigDecimal finalInterestRate;

    @Column(name = "eligible_emi", precision = 15, scale = 2)
    private BigDecimal eligibleEmi;

    /** One reason per line. */
    @Column(name = "reasons", columnDefinition = "TEXT")
    private String reasons;

    @Column(name = "request_snapshot", columnDefinition = "TEXT")
    private String requestSnapshot;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<EligibilityRuleResult> ruleResults = new ArrayList<>();

    public void addRuleResult(EligibilityRuleResult result) {
        result.setAssessment(this);
        ruleResults.add(result);
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
