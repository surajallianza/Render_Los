package com.los.bank.bankmaster.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entity mapping table s001 (Bank Accounts).
 * Logical name: bank_accounts.
 */
@Entity
@Table(name = "s001")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "particulars", length = 150)
    private String particulars;

    @Column(name = "particulars_lookup_id")
    private Long particularsLookupId;

    @Column(name = "account_type", nullable = false, length = 50)
    private String accountType;

    @Column(name = "account_type_lookup_id")
    private Long accountTypeLookupId;

    @Column(name = "account_number", nullable = false, length = 100)
    private String accountNumber;

    @Column(name = "micr_code", length = 30)
    private String micrCode;

    @Column(name = "ifsc_code", nullable = false, length = 30)
    private String ifscCode;

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

    public BankAccount() {
    }

    public BankAccount(Long id, Long bankId, String bankName, Long branchId, String particulars,
                       Long particularsLookupId, String accountType, Long accountTypeLookupId,
                       String accountNumber, String micrCode, String ifscCode, Boolean isActive,
                       String status, Long createdBy, LocalDateTime createdAt, Long modifiedBy,
                       LocalDateTime updatedAt) {
        this.id = id;
        this.bankId = bankId;
        this.bankName = bankName;
        this.branchId = branchId;
        this.particulars = particulars;
        this.particularsLookupId = particularsLookupId;
        this.accountType = accountType;
        this.accountTypeLookupId = accountTypeLookupId;
        this.accountNumber = accountNumber;
        this.micrCode = micrCode;
        this.ifscCode = ifscCode;
        this.isActive = isActive != null ? isActive : true;
        this.status = status != null ? status : "ACTIVE";
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.modifiedBy = modifiedBy;
        this.updatedAt = updatedAt;
    }

    public static BankAccountBuilder builder() {
        return new BankAccountBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBankId() { return bankId; }
    public void setBankId(Long bankId) { this.bankId = bankId; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

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

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(Long modifiedBy) { this.modifiedBy = modifiedBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class BankAccountBuilder {
        private Long id;
        private Long bankId;
        private String bankName;
        private Long branchId;
        private String particulars;
        private Long particularsLookupId;
        private String accountType;
        private Long accountTypeLookupId;
        private String accountNumber;
        private String micrCode;
        private String ifscCode;
        private Boolean isActive = true;
        private String status = "ACTIVE";
        private Long createdBy;
        private LocalDateTime createdAt = LocalDateTime.now();
        private Long modifiedBy;
        private LocalDateTime updatedAt;

        BankAccountBuilder() {}

        public BankAccountBuilder id(Long id) { this.id = id; return this; }
        public BankAccountBuilder bankId(Long bankId) { this.bankId = bankId; return this; }
        public BankAccountBuilder bankName(String bankName) { this.bankName = bankName; return this; }
        public BankAccountBuilder branchId(Long branchId) { this.branchId = branchId; return this; }
        public BankAccountBuilder particulars(String particulars) { this.particulars = particulars; return this; }
        public BankAccountBuilder particularsLookupId(Long particularsLookupId) { this.particularsLookupId = particularsLookupId; return this; }
        public BankAccountBuilder accountType(String accountType) { this.accountType = accountType; return this; }
        public BankAccountBuilder accountTypeLookupId(Long accountTypeLookupId) { this.accountTypeLookupId = accountTypeLookupId; return this; }
        public BankAccountBuilder accountNumber(String accountNumber) { this.accountNumber = accountNumber; return this; }
        public BankAccountBuilder micrCode(String micrCode) { this.micrCode = micrCode; return this; }
        public BankAccountBuilder ifscCode(String ifscCode) { this.ifscCode = ifscCode; return this; }
        public BankAccountBuilder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public BankAccountBuilder status(String status) { this.status = status; return this; }
        public BankAccountBuilder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public BankAccountBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public BankAccountBuilder modifiedBy(Long modifiedBy) { this.modifiedBy = modifiedBy; return this; }
        public BankAccountBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public BankAccount build() {
            return new BankAccount(id, bankId, bankName, branchId, particulars, particularsLookupId,
                    accountType, accountTypeLookupId, accountNumber, micrCode, ifscCode, isActive,
                    status, createdBy, createdAt, modifiedBy, updatedAt);
        }
    }
}
