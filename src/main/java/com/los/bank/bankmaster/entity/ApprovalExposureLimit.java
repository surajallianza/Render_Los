package com.los.bank.bankmaster.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity mapping table s002 (Approval & Exposure Limits).
 * Logical name: approval_exposure_limits.
 */
@Entity
@Table(name = "s002")
public class ApprovalExposureLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "approval_authority", nullable = false, length = 150)
    private String approvalAuthority;

    @Column(name = "approval_authority_lookup_id")
    private Long approvalAuthorityLookupId;

    @Column(name = "single_party_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal singlePartyLimit = BigDecimal.ZERO;

    @Column(name = "group_exposure_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal groupExposureLimit = BigDecimal.ZERO;

    @Column(name = "sanction_margin", nullable = false, precision = 8, scale = 4)
    private BigDecimal sanctionMargin = BigDecimal.ZERO;

    @Column(name = "penalty_charge", nullable = false, precision = 15, scale = 2)
    private BigDecimal penaltyCharge = BigDecimal.ZERO;

    @Column(name = "penalty_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal penaltyRate = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "status", length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "modified_by")
    private Long modifiedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ApprovalExposureLimit() {
    }

    public ApprovalExposureLimit(Long id, Long bankId, String bankName, Long branchId,
                                 String approvalAuthority, Long approvalAuthorityLookupId,
                                 BigDecimal singlePartyLimit, BigDecimal groupExposureLimit,
                                 BigDecimal sanctionMargin, BigDecimal penaltyCharge,
                                 BigDecimal penaltyRate, Boolean isActive, String status,
                                 Long createdBy, LocalDateTime createdAt, Long modifiedBy,
                                 LocalDateTime updatedAt) {
        this.id = id;
        this.bankId = bankId;
        this.bankName = bankName;
        this.branchId = branchId;
        this.approvalAuthority = approvalAuthority;
        this.approvalAuthorityLookupId = approvalAuthorityLookupId;
        this.singlePartyLimit = singlePartyLimit != null ? singlePartyLimit : BigDecimal.ZERO;
        this.groupExposureLimit = groupExposureLimit != null ? groupExposureLimit : BigDecimal.ZERO;
        this.sanctionMargin = sanctionMargin != null ? sanctionMargin : BigDecimal.ZERO;
        this.penaltyCharge = penaltyCharge != null ? penaltyCharge : BigDecimal.ZERO;
        this.penaltyRate = penaltyRate != null ? penaltyRate : BigDecimal.ZERO;
        this.isActive = isActive != null ? isActive : true;
        this.status = status != null ? status : "ACTIVE";
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.modifiedBy = modifiedBy;
        this.updatedAt = updatedAt;
    }

    public static ApprovalExposureLimitBuilder builder() {
        return new ApprovalExposureLimitBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public String getApprovalAuthority() { return approvalAuthority; }
    public void setApprovalAuthority(String approvalAuthority) { this.approvalAuthority = approvalAuthority; }

    public Long getApprovalAuthorityLookupId() { return approvalAuthorityLookupId; }
    public void setApprovalAuthorityLookupId(Long approvalAuthorityLookupId) { this.approvalAuthorityLookupId = approvalAuthorityLookupId; }

    public BigDecimal getSinglePartyLimit() { return singlePartyLimit; }
    public void setSinglePartyLimit(BigDecimal singlePartyLimit) { this.singlePartyLimit = singlePartyLimit; }

    public BigDecimal getGroupExposureLimit() { return groupExposureLimit; }
    public void setGroupExposureLimit(BigDecimal groupExposureLimit) { this.groupExposureLimit = groupExposureLimit; }

    public BigDecimal getSanctionMargin() { return sanctionMargin; }
    public void setSanctionMargin(BigDecimal sanctionMargin) { this.sanctionMargin = sanctionMargin; }

    public BigDecimal getPenaltyCharge() { return penaltyCharge; }
    public void setPenaltyCharge(BigDecimal penaltyCharge) { this.penaltyCharge = penaltyCharge; }

    public BigDecimal getPenaltyRate() { return penaltyRate; }
    public void setPenaltyRate(BigDecimal penaltyRate) { this.penaltyRate = penaltyRate; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(Long modifiedBy) { this.modifiedBy = modifiedBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class ApprovalExposureLimitBuilder {
        private Long id;
        private Long bankId;
        private String bankName;
        private Long branchId;
        private String approvalAuthority;
        private Long approvalAuthorityLookupId;
        private BigDecimal singlePartyLimit = BigDecimal.ZERO;
        private BigDecimal groupExposureLimit = BigDecimal.ZERO;
        private BigDecimal sanctionMargin = BigDecimal.ZERO;
        private BigDecimal penaltyCharge = BigDecimal.ZERO;
        private BigDecimal penaltyRate = BigDecimal.ZERO;
        private Boolean isActive = true;
        private String status = "ACTIVE";
        private Long createdBy;
        private LocalDateTime createdAt = LocalDateTime.now();
        private Long modifiedBy;
        private LocalDateTime updatedAt;

        ApprovalExposureLimitBuilder() {}

        public ApprovalExposureLimitBuilder id(Long id) { this.id = id; return this; }
        public ApprovalExposureLimitBuilder bankId(Long bankId) { this.bankId = bankId; return this; }
        public ApprovalExposureLimitBuilder bankName(String bankName) { this.bankName = bankName; return this; }
        public ApprovalExposureLimitBuilder branchId(Long branchId) { this.branchId = branchId; return this; }
        public ApprovalExposureLimitBuilder approvalAuthority(String approvalAuthority) { this.approvalAuthority = approvalAuthority; return this; }
        public ApprovalExposureLimitBuilder approvalAuthorityLookupId(Long approvalAuthorityLookupId) { this.approvalAuthorityLookupId = approvalAuthorityLookupId; return this; }
        public ApprovalExposureLimitBuilder singlePartyLimit(BigDecimal singlePartyLimit) { this.singlePartyLimit = singlePartyLimit; return this; }
        public ApprovalExposureLimitBuilder groupExposureLimit(BigDecimal groupExposureLimit) { this.groupExposureLimit = groupExposureLimit; return this; }
        public ApprovalExposureLimitBuilder sanctionMargin(BigDecimal sanctionMargin) { this.sanctionMargin = sanctionMargin; return this; }
        public ApprovalExposureLimitBuilder penaltyCharge(BigDecimal penaltyCharge) { this.penaltyCharge = penaltyCharge; return this; }
        public ApprovalExposureLimitBuilder penaltyRate(BigDecimal penaltyRate) { this.penaltyRate = penaltyRate; return this; }
        public ApprovalExposureLimitBuilder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public ApprovalExposureLimitBuilder status(String status) { this.status = status; return this; }
        public ApprovalExposureLimitBuilder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public ApprovalExposureLimitBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ApprovalExposureLimitBuilder modifiedBy(Long modifiedBy) { this.modifiedBy = modifiedBy; return this; }
        public ApprovalExposureLimitBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ApprovalExposureLimit build() {
            return new ApprovalExposureLimit(id, bankId, bankName, branchId, approvalAuthority,
                    approvalAuthorityLookupId, singlePartyLimit, groupExposureLimit, sanctionMargin,
                    penaltyCharge, penaltyRate, isActive, status, createdBy, createdAt, modifiedBy, updatedAt);
        }
    }
}
