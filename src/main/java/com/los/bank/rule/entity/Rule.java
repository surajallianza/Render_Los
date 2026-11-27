package com.los.bank.rule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Loan eligibility rule (table e001). Parameter, operator and decision are Lookup Master
 * values (m002 ids). Numeric rules use {@link #val} or {@link #fromValue}..{@link #toValue};
 * choice rules (e.g. Firm Registration is Registered) use {@link #valueLookupId}.
 */
@Entity
@Table(name = "e001")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rule {

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "param_lookup_id", nullable = false)
    private Long paramLookupId;

    @Column(name = "operator_lookup_id", nullable = false)
    private Long operatorLookupId;

    @Column(name = "decision_lookup_id", nullable = false)
    private Long decisionLookupId;

    @Column(name = "from_value", precision = 12, scale = 2)
    private BigDecimal fromValue;

    @Column(name = "to_value", precision = 12, scale = 2)
    private BigDecimal toValue;

    @Column(name = "val", precision = 12, scale = 2)
    private BigDecimal val;

    @Column(name = "value_lookup_id")
    private Long valueLookupId;

    /** Added to the interest rate when the decision is Override. */
    @Column(name = "rate_offset", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal rateOffset = BigDecimal.ZERO;

    @Column(name = "message", length = 255)
    private String message;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = ACTIVE;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (effectiveDate == null) {
            effectiveDate = LocalDate.now();
        }
        if (rateOffset == null) {
            rateOffset = BigDecimal.ZERO;
        }
    }
}
