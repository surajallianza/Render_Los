package com.los.bank.interestrate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Interest rate sheet of a loan product (table i001), effective from {@link #effectiveDate}.
 */
@Entity
@Table(name = "i001")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestRateMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = InterestRateStatus.PENDING;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private Long modifiedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Checker who approved or rejected the sheet. */
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @OneToMany(mappedBy = "rateMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("srNo ASC")
    @Builder.Default
    private List<InterestRateTier> tiers = new ArrayList<>();

    /**
     * Replaces all tiers (orphans are deleted).
     */
    public void replaceTiers(List<InterestRateTier> newTiers) {
        tiers.clear();
        if (newTiers != null) {
            for (InterestRateTier tier : newTiers) {
                tier.setRateMaster(this);
                tiers.add(tier);
            }
        }
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
