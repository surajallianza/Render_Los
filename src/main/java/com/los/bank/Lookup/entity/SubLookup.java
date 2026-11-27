package com.los.bank.lookup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping to m002 table (Sub Lookup).
 */
@Entity
@Table(name = "m002")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubLookup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lookup_type_code", nullable = false, length = 50)
    private String lookupTypeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lookup_type_code", referencedColumnName = "code", insertable = false, updatable = false)
    private Lookup lookup;

    @Column(name = "sub_type_code", nullable = false, length = 50)
    private String subTypeCode;

    @Column(name = "sub_type_description", length = 255)
    private String subTypeDescription;

    @Column(name = "is_fixed", nullable = false)
    @Builder.Default
    private Boolean isFixed = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_by")
    private Long modifiedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isFixed == null) {
            isFixed = false;
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
