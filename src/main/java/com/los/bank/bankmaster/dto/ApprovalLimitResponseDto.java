package com.los.bank.bankmaster.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Approval & Exposure Limits (s002).
 */
public class ApprovalLimitResponseDto {

    private Long id;

    @JsonProperty("bankId")
    private Long bankId;

    @JsonProperty("bankName")
    private String bankName;

    @JsonProperty("branchId")
    private Long branchId;

    @JsonProperty("branchName")
    private String branchName;

    @JsonProperty("approvalAuthority")
    private String approvalAuthority;

    @JsonProperty("approvalAuthorityLookupId")
    private Long approvalAuthorityLookupId;

    @JsonProperty("singlePartyLimit")
    private BigDecimal singlePartyLimit;

    @JsonProperty("groupExposureLimit")
    private BigDecimal groupExposureLimit;

    @JsonProperty("sanctionMargin")
    private BigDecimal sanctionMargin;

    @JsonProperty("penaltyCharge")
    private BigDecimal penaltyCharge;

    @JsonProperty("penaltyRate")
    private BigDecimal penaltyRate;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("status")
    private String status;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    public ApprovalLimitResponseDto() {}

    public static ApprovalLimitResponseDtoBuilder builder() {
        return new ApprovalLimitResponseDtoBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class ApprovalLimitResponseDtoBuilder {
        private Long id;
        private Long bankId;
        private String bankName;
        private Long branchId;
        private String branchName;
        private String approvalAuthority;
        private Long approvalAuthorityLookupId;
        private BigDecimal singlePartyLimit;
        private BigDecimal groupExposureLimit;
        private BigDecimal sanctionMargin;
        private BigDecimal penaltyCharge;
        private BigDecimal penaltyRate;
        private Boolean isActive;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        ApprovalLimitResponseDtoBuilder() {}

        public ApprovalLimitResponseDtoBuilder id(Long id) { this.id = id; return this; }
        public ApprovalLimitResponseDtoBuilder bankId(Long bankId) { this.bankId = bankId; return this; }
        public ApprovalLimitResponseDtoBuilder bankName(String bankName) { this.bankName = bankName; return this; }
        public ApprovalLimitResponseDtoBuilder branchId(Long branchId) { this.branchId = branchId; return this; }
        public ApprovalLimitResponseDtoBuilder branchName(String branchName) { this.branchName = branchName; return this; }
        public ApprovalLimitResponseDtoBuilder approvalAuthority(String approvalAuthority) { this.approvalAuthority = approvalAuthority; return this; }
        public ApprovalLimitResponseDtoBuilder approvalAuthorityLookupId(Long approvalAuthorityLookupId) { this.approvalAuthorityLookupId = approvalAuthorityLookupId; return this; }
        public ApprovalLimitResponseDtoBuilder singlePartyLimit(BigDecimal singlePartyLimit) { this.singlePartyLimit = singlePartyLimit; return this; }
        public ApprovalLimitResponseDtoBuilder groupExposureLimit(BigDecimal groupExposureLimit) { this.groupExposureLimit = groupExposureLimit; return this; }
        public ApprovalLimitResponseDtoBuilder sanctionMargin(BigDecimal sanctionMargin) { this.sanctionMargin = sanctionMargin; return this; }
        public ApprovalLimitResponseDtoBuilder penaltyCharge(BigDecimal penaltyCharge) { this.penaltyCharge = penaltyCharge; return this; }
        public ApprovalLimitResponseDtoBuilder penaltyRate(BigDecimal penaltyRate) { this.penaltyRate = penaltyRate; return this; }
        public ApprovalLimitResponseDtoBuilder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public ApprovalLimitResponseDtoBuilder status(String status) { this.status = status; return this; }
        public ApprovalLimitResponseDtoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ApprovalLimitResponseDtoBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ApprovalLimitResponseDto build() {
            ApprovalLimitResponseDto dto = new ApprovalLimitResponseDto();
            dto.setId(id);
            dto.setBankId(bankId);
            dto.setBankName(bankName);
            dto.setBranchId(branchId);
            dto.setBranchName(branchName);
            dto.setApprovalAuthority(approvalAuthority);
            dto.setApprovalAuthorityLookupId(approvalAuthorityLookupId);
            dto.setSinglePartyLimit(singlePartyLimit);
            dto.setGroupExposureLimit(groupExposureLimit);
            dto.setSanctionMargin(sanctionMargin);
            dto.setPenaltyCharge(penaltyCharge);
            dto.setPenaltyRate(penaltyRate);
            dto.setIsActive(isActive);
            dto.setStatus(status);
            dto.setCreatedAt(createdAt);
            dto.setUpdatedAt(updatedAt);
            return dto;
        }
    }
}
