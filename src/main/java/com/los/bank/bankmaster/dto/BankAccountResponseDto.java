package com.los.bank.bankmaster.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Response DTO for Bank Account (s001).
 */
public class BankAccountResponseDto {

    private Long id;

    @JsonProperty("bankId")
    private Long bankId;

    @JsonProperty("bankName")
    private String bankName;

    @JsonProperty("branchId")
    private Long branchId;

    @JsonProperty("branchName")
    private String branchName;

    @JsonProperty("particulars")
    private String particulars;

    @JsonProperty("particularsLookupId")
    private Long particularsLookupId;

    @JsonProperty("accountType")
    private String accountType;

    @JsonProperty("accountTypeLookupId")
    private Long accountTypeLookupId;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("micrCode")
    private String micrCode;

    @JsonProperty("ifscCode")
    private String ifscCode;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("status")
    private String status;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    public BankAccountResponseDto() {}

    public static BankAccountResponseDtoBuilder builder() {
        return new BankAccountResponseDtoBuilder();
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

    public String getParticulars() { return particulars; }
    public void setParticulars(String particulars) { this.particulars = particulars; }

    public Long getParticularsLookupId() { return particularsLookupId; }
    public void setParticularsLookupId(Long particularsLookupId) { this.particularsLookupId = particularsLookupId; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public Long getAccountTypeLookupId() { return accountTypeLookupId; }
    public void setAccountTypeLookupId(Long accountTypeLookupId) { this.accountTypeLookupId = accountTypeLookupId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getMicrCode() { return micrCode; }
    public void setMicrCode(String micrCode) { this.micrCode = micrCode; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class BankAccountResponseDtoBuilder {
        private Long id;
        private Long bankId;
        private String bankName;
        private Long branchId;
        private String branchName;
        private String particulars;
        private Long particularsLookupId;
        private String accountType;
        private Long accountTypeLookupId;
        private String accountNumber;
        private String micrCode;
        private String ifscCode;
        private Boolean isActive;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        BankAccountResponseDtoBuilder() {}

        public BankAccountResponseDtoBuilder id(Long id) { this.id = id; return this; }
        public BankAccountResponseDtoBuilder bankId(Long bankId) { this.bankId = bankId; return this; }
        public BankAccountResponseDtoBuilder bankName(String bankName) { this.bankName = bankName; return this; }
        public BankAccountResponseDtoBuilder branchId(Long branchId) { this.branchId = branchId; return this; }
        public BankAccountResponseDtoBuilder branchName(String branchName) { this.branchName = branchName; return this; }
        public BankAccountResponseDtoBuilder particulars(String particulars) { this.particulars = particulars; return this; }
        public BankAccountResponseDtoBuilder particularsLookupId(Long particularsLookupId) { this.particularsLookupId = particularsLookupId; return this; }
        public BankAccountResponseDtoBuilder accountType(String accountType) { this.accountType = accountType; return this; }
        public BankAccountResponseDtoBuilder accountTypeLookupId(Long accountTypeLookupId) { this.accountTypeLookupId = accountTypeLookupId; return this; }
        public BankAccountResponseDtoBuilder accountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
        public BankAccountResponseDtoBuilder micrCode(String micrCode) { this.micrCode = micrCode; return this; }
        public BankAccountResponseDtoBuilder ifscCode(String ifscCode) { this.ifscCode = ifscCode; return this; }
        public BankAccountResponseDtoBuilder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public BankAccountResponseDtoBuilder status(String status) { this.status = status; return this; }
        public BankAccountResponseDtoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public BankAccountResponseDtoBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public BankAccountResponseDto build() {
            BankAccountResponseDto dto = new BankAccountResponseDto();
            dto.setId(id);
            dto.setBankId(bankId);
            dto.setBankName(bankName);
            dto.setBranchId(branchId);
            dto.setBranchName(branchName);
            dto.setParticulars(particulars);
            dto.setParticularsLookupId(particularsLookupId);
            dto.setAccountType(accountType);
            dto.setAccountTypeLookupId(accountTypeLookupId);
            dto.setAccountNumber(accountNumber);
            dto.setMicrCode(micrCode);
            dto.setIfscCode(ifscCode);
            dto.setIsActive(isActive);
            dto.setStatus(status);
            dto.setCreatedAt(createdAt);
            dto.setUpdatedAt(updatedAt);
            return dto;
        }
    }
}
