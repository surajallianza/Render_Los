package com.los.bank.rule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing a configurable business rule in the Rule Engine module.
 * Mapped to table 'e001' in the bank tenant schema.
 */
@Entity
@Table(name = "e001")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "param", length = 100, nullable = false)
    private String param;

    @Column(name = "comparison_operator", length = 50, nullable = false)
    private String comparisonOperator;

    @Column(name = "from_value", length = 100)
    private String fromValue;

    @Column(name = "to_value", length = 100)
    private String toValue;

    @Column(name = "val", length = 100)
    private String val;

    @Column(name = "action_on_match", length = 50, nullable = false)
    private String actionOnMatch;

    @Column(name = "execution_offset", nullable = false)
    private Integer executionOffset;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (executionOffset == null) {
            executionOffset = 0;
        }
        if (status == null || status.trim().isEmpty()) {
            status = "active";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
