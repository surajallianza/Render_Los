package com.los.bank.bankmaster.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for creating or updating a Bank Account (s001).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankAccountRequestDto {

    private Long bankId;

    @NotBlank(message = "Bank name is required.")
    @JsonProperty("bankName")
    @JsonAlias({"bank_name"})
    private String bankName;

    @JsonProperty("branchId")
    @JsonAlias({"branch_id"})
    private Long branchId;

    @JsonProperty("particulars")
    private String particulars;

    @JsonProperty("particularsLookupId")
    @JsonAlias({"particulars_lookup_id"})
    private Long particularsLookupId;

    @NotBlank(message = "Account type is required.")
    @JsonProperty("accountType")
    @JsonAlias({"account_type"})
    private String accountType;

    @JsonProperty("accountTypeLookupId")
    @JsonAlias({"account_type_lookup_id"})
    private Long accountTypeLookupId;

    @NotBlank(message = "Account number is required.")
    @Pattern(regexp = "^[0-9A-Za-z]{6,34}$", message = "Account number must be between 6 and 34 alphanumeric characters.")
    @JsonProperty("accountNumber")
    @JsonAlias({"account_number"})
    private String accountNumber;

    @JsonProperty("micrCode")
    @JsonAlias({"micr_code", "micr"})
    private String micrCode;

    @NotBlank(message = "IFSC code is required.")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Please enter a valid 11-character IFSC code (e.g. SBIN0001234).")
    @JsonProperty("ifscCode")
    @JsonAlias({"IFSCCode", "ifsc_code", "ifsc"})
    private String ifscCode;

    @JsonProperty("status")
    private String status;

    public BankAccountRequestDto() {}

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
