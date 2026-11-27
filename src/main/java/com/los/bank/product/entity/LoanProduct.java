package com.los.bank.product.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Root Loan Product entity strictly mapped to PostgreSQL Bank database tables:
 * - Primary table: p001 (Core header)
 * - Secondary table: p002 (Terms, tenure, and amounts)
 * - Secondary table: p003 (Interest calculation and penalties)
 * - Child table: p004 (Delegation of Power entries)
 */
@Entity
@Table(name = "p001")
@SecondaryTables({
        @SecondaryTable(name = "p002", pkJoinColumns = @PrimaryKeyJoinColumn(name = "product_id", referencedColumnName = "id")),
        @SecondaryTable(name = "p003", pkJoinColumns = @PrimaryKeyJoinColumn(name = "product_id", referencedColumnName = "id"))
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProduct {

    // =========================================================================
    // Table: p001 (Core Product Definition)
    // =========================================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    // =========================================================================
    // Table: p002 (Product Financial Terms, Limits, and Basis)
    // =========================================================================
    @Column(name = "emi", table = "p002", length = 50)
    private String emi;

    @Column(name = "emi_type", table = "p002", length = 50)
    private String emiType;

    @Column(name = "emi_fixed_date", table = "p002", length = 20)
    private String emiFixedDate;

    @Column(name = "min_tenure_months", table = "p002")
    private Integer minTenureMonths;

    @Column(name = "max_tenure_months", table = "p002")
    private Integer maxTenureMonths;

    @Column(name = "min_amount", table = "p002", precision = 15, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", table = "p002", precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "int_base_method", table = "p002", length = 50)
    private String intBaseMethod;

    @Column(name = "no_of_days_in_year", table = "p002")
    private Integer noOfDaysInYear;

    // =========================================================================
    // Table: p003 (Interest Calculation and Penalties)
    // =========================================================================
    @Column(name = "penalty", table = "p003")
    private Boolean penalty;

    @Column(name = "penalty_percent", table = "p003", precision = 8, scale = 4)
    private BigDecimal penaltyPercent;

    @Column(name = "interest_calculation_frequency", table = "p003", length = 50)
    private String interestCalculationFrequency;

    @Column(name = "interest_calculation_mode", table = "p003", length = 50)
    private String interestCalculationMode;

    // =========================================================================
    // Table: p004 (Child Collection: Delegation of Power)
    // =========================================================================
    @OneToMany(mappedBy = "loanProduct", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductDelegation> delegationOfPower = new ArrayList<>();

    public void addDelegation(ProductDelegation delegation) {
        if (delegation != null) {
            delegation.setLoanProduct(this);
            this.delegationOfPower.add(delegation);
        }
    }

    public void removeDelegation(ProductDelegation delegation) {
        if (delegation != null) {
            delegation.setLoanProduct(null);
            this.delegationOfPower.remove(delegation);
        }
    }

    public void setDelegationOfPower(List<ProductDelegation> delegations) {
        this.delegationOfPower.clear();
        if (delegations != null) {
            for (ProductDelegation delegation : delegations) {
                delegation.setLoanProduct(this);
                this.delegationOfPower.add(delegation);
            }
        }
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null || status.trim().isEmpty()) {
            status = "ACTIVE";
        }
        if (emiFixedDate == null || emiFixedDate.trim().isEmpty()) {
            emiFixedDate = "No";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
