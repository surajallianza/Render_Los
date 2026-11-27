package com.los.bank.bankmaster.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for Approval & Exposure Limits (s002).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApprovalLimitRequestDto {

    private Long bankId;

    @JsonProperty("bankName")
    @JsonAlias({"bank_name"})
    private String bankName;

    @JsonProperty("branchId")
    @JsonAlias({"branch_id"})
    private Long branchId;

    @NotBlank(message = "Approval authority is required.")
    @JsonProperty("approvalAuthority")
    @JsonAlias({"approval_authority", "LoanApprovalAuthority", "loanApprovalAuthority"})
    private String approvalAuthority;

    @JsonProperty("approvalAuthorityLookupId")
    @JsonAlias({"approval_authority_lookup_id", "authorityLookupId"})
    private Long approvalAuthorityLookupId;

    @NotNull(message = "Single party limit is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Single party limit must be non-negative.")
    @JsonProperty("singlePartyLimit")
    @JsonAlias({"single_party_limit"})
    private BigDecimal singlePartyLimit;

    @NotNull(message = "Group exposure limit is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Group exposure limit must be non-negative.")
    @JsonProperty("groupExposureLimit")
    @JsonAlias({"group_exposure_limit"})
    private BigDecimal groupExposureLimit;

    @NotNull(message = "Sanction margin is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Sanction margin must be non-negative.")
    @JsonProperty("sanctionMargin")
    @JsonAlias({"sanction_margin"})
    private BigDecimal sanctionMargin;

    @NotNull(message = "Penalty charge is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Penalty charge must be non-negative.")
    @JsonProperty("penaltyCharge")
    @JsonAlias({"penalty_charge"})
    private BigDecimal penaltyCharge;

    @NotNull(message = "Penalty rate is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Penalty rate must be non-negative.")
    @JsonProperty("penaltyRate")
    @JsonAlias({"penalty_rate"})
    private BigDecimal penaltyRate;

    @JsonProperty("status")
    private String status;

    public ApprovalLimitRequestDto() {}

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
