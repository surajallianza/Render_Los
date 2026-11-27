package com.los.bank.loan.entity;

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
import jakarta.persistence.Transient;
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
 * Root Retail Loan Application entity mapped strictly to the existing PostgreSQL
 * Bank schema tables:
 * - Primary table: a001 (Application Header)
 * - Secondary table: a002 (Financial & Credit Assessment)
 * - Secondary table: a003 (Loan Terms & Sanction)
 * - Secondary table: a004 (Maker-Checker Approval & Remarks)
 * - Child table: a005 (Income / Expense Line Items)
 */
@Entity
@Table(name = "a001")
@SecondaryTables({
        @SecondaryTable(name = "a002", pkJoinColumns = @PrimaryKeyJoinColumn(name = "application_id", referencedColumnName = "id")),
        @SecondaryTable(name = "a003", pkJoinColumns = @PrimaryKeyJoinColumn(name = "application_id", referencedColumnName = "id")),
        @SecondaryTable(name = "a004", pkJoinColumns = @PrimaryKeyJoinColumn(name = "application_id", referencedColumnName = "id"))
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetailLoanApplication {

    // =========================================================================
    // Table: a001 (Loan Application Header)
    // =========================================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "application_number", length = 40, nullable = false, unique = true)
    private String applicationNumber;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "product_code", length = 50, nullable = false)
    private String productCode;

    @Column(name = "status", length = 40)
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
    // Table: a002 (Financial & Credit Assessment)
    // =========================================================================
    @Column(name = "total_monthly_income", table = "a002", precision = 15, scale = 2)
    private BigDecimal totalMonthlyIncome;

    @Column(name = "total_monthly_expenses", table = "a002", precision = 15, scale = 2)
    private BigDecimal totalMonthlyExpenses;

    @Column(name = "net_monthly_income", table = "a002", precision = 15, scale = 2)
    private BigDecimal netMonthlyIncome;

    @Column(name = "existing_monthly_debt", table = "a002", precision = 15, scale = 2)
    private BigDecimal existingMonthlyDebt;

    @Column(name = "cibil_score", table = "a002")
    private Integer cibilScore;

    @Column(name = "collateral_value", table = "a002", precision = 15, scale = 2)
    private BigDecimal collateralValue;

    @Column(name = "emi", table = "a002", precision = 15, scale = 2)
    private BigDecimal emi;

    @Column(name = "dti_ratio", table = "a002", precision = 8, scale = 4)
    private BigDecimal dtiRatio;

    // =========================================================================
    // Table: a003 (Loan Terms & Sanction)
    // =========================================================================
    @Column(name = "applied_amount", table = "a003", precision = 15, scale = 2)
    private BigDecimal appliedAmount;

    @Column(name = "interest_rate", table = "a003", precision = 8, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", table = "a003")
    private Integer tenureMonths;

    @Column(name = "max_eligible_amount", table = "a003", precision = 15, scale = 2)
    private BigDecimal maxEligibleAmount;

    @Column(name = "eligibility_status", table = "a003", length = 40)
    private String eligibilityStatus;

    @Column(name = "sanctioned_amount", table = "a003", precision = 15, scale = 2)
    private BigDecimal sanctionedAmount;

    @Column(name = "kyc_status", table = "a003", length = 30)
    private String kycStatus;

    // =========================================================================
    // Table: a004 (Maker-Checker Approval & Remarks)
    // =========================================================================
    @Column(name = "maker_user_id", table = "a004")
    private Long makerUserId;

    @Column(name = "checker_user_id", table = "a004")
    private Long checkerUserId;

    @Column(name = "checker_remarks", table = "a004", length = 500)
    private String checkerRemarks;

    @Column(name = "approved_at", table = "a004")
    private LocalDateTime approvedAt;

    // =========================================================================
    // Table: a005 (Income / Expense Line Items)
    // =========================================================================
    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RetailLoanApplicationItem> items = new ArrayList<>();

    // =========================================================================
    // Transient Metadata (Resolved Master Names)
    // =========================================================================
    @Transient
    private String customerCode;

    @Transient
    private String branchCode;

    @Transient
    private String productName;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null || status.trim().isEmpty()) {
            status = "Operative";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
