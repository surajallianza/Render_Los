package com.los.bank.lead.entity;

import com.los.bank.lead.dto.AddressDetailDto;
import com.los.bank.lead.dto.CreditFacilityDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Root Lead entity strictly mapped to PostgreSQL Bank database tables:
 * - Primary table: l001
 * - Secondary tables: l002, l003, l004, l005, l006
 *
 * All secondary tables share lead_id as their PRIMARY KEY and FOREIGN KEY.
 */
@Entity
@Table(name = "l001")
@SecondaryTables({
        @SecondaryTable(name = "l002", pkJoinColumns = @PrimaryKeyJoinColumn(name = "lead_id")),
        @SecondaryTable(name = "l003", pkJoinColumns = @PrimaryKeyJoinColumn(name = "lead_id")),
        @SecondaryTable(name = "l004", pkJoinColumns = @PrimaryKeyJoinColumn(name = "lead_id")),
        @SecondaryTable(name = "l005", pkJoinColumns = @PrimaryKeyJoinColumn(name = "lead_id")),
        @SecondaryTable(name = "l006", pkJoinColumns = @PrimaryKeyJoinColumn(name = "lead_id"))
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    // =========================================================================
    // Table: l001 (Core Header)
    // =========================================================================
    @Id
    @Column(name = "lead_id", length = 50, nullable = false)
    private String leadId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "lead_status", length = 50)
    private String status;

    @Column(name = "assigned_user_id")
    private Long assignedUserId;

    @Column(name = "assignment_timestamp")
    private LocalDateTime assignmentTimestamp;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @Column(name = "lead_acquisition_channel", length = 100)
    private String acquisitionChannel;

    @Column(name = "date")
    private LocalDate referralDate;

    @Column(name = "sourcing_agent_partner_id", length = 100)
    private String partnerId;

    @Column(name = "agent_partner_name", length = 150)
    private String partnerName;

    @Column(name = "employee_name", length = 150)
    private String employeeName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "branch", length = 50)
    private String branch;

    @Transient
    private String employeeId;

    // =========================================================================
    // Table: l002 (Applicant / Personal Details)
    // =========================================================================
    @Column(name = "name_prefix", length = 20, table = "l002")
    private String namePrefix;

    @Column(name = "first_name_business_name", length = 150, table = "l002")
    private String firstName;

    @Column(name = "middle_name", length = 80, table = "l002")
    private String middleName;

    @Column(name = "last_name", length = 100, table = "l002")
    private String lastName;

    @Column(name = "customer_name", length = 250, table = "l002")
    private String customerName;

    @Column(name = "dob", table = "l002")
    private LocalDate dateOfBirth;

    @Column(name = "age", table = "l002")
    private Integer age;

    @Column(name = "customer_type", length = 50, table = "l002")
    private String customerType;

    @Column(name = "gender", length = 20, table = "l002")
    private String gender;

    @Column(name = "marital_status", length = 50, table = "l002")
    private String maritalStatus;

    @Column(name = "residential_status", length = 50, table = "l002")
    private String residentialStatus;

    @Column(name = "email_address", length = 150, table = "l002")
    private String email;

    @Column(name = "mobile_number", length = 50, table = "l002")
    private String mobileNumber;

    @Column(name = "pin_code", length = 20, table = "l002")
    private String pinCode;

    @Column(name = "number_of_dependents", table = "l002")
    private Integer dependents;

    // =========================================================================
    // Table: l003 (KYC / Documents)
    // =========================================================================
    @Column(name = "pan_number", columnDefinition = "BYTEA", table = "l003")
    private byte[] panNumber;

    @Column(name = "aadhaar_number", columnDefinition = "BYTEA", table = "l003")
    private byte[] aadhaarNumber;

    @Column(name = "passport_number", columnDefinition = "BYTEA", table = "l003")
    private byte[] passportNumber;

    @Column(name = "passport_expiry_date", table = "l003")
    private LocalDate passportExpiryDate;

    @Column(name = "pan_validation_status", length = 50, table = "l003")
    private String panValidationStatus;

    @Column(name = "aadhaar_validation_status", length = 50, table = "l003")
    private String aadhaarValidationStatus;

    @Column(name = "dedupe_status", length = 50, table = "l003")
    private String dedupeStatus;

    @Column(name = "blacklist_status", length = 50, table = "l003")
    private String blacklistStatus;

    // =========================================================================
    // Table: l004 (Loan Particulars)
    // =========================================================================
    @Column(name = "loan_product_type", length = 100, table = "l004")
    private String loanProductType;

    @Column(name = "loan_amount", precision = 15, scale = 2, table = "l004")
    private BigDecimal loanAmount;

    @Column(name = "purpose_of_loan", length = 255, table = "l004")
    private String loanPurpose;

    @Column(name = "tenure", table = "l004")
    private Integer tenure;

    @Column(name = "number_of_instalments", table = "l004")
    private Integer instalments;

    @Column(name = "emi", precision = 15, scale = 2, table = "l004")
    private BigDecimal monthlyEmi;

    @Column(name = "interest_rate", precision = 8, scale = 4, table = "l004")
    private BigDecimal interestRate;

    @Column(name = "total_interest", precision = 15, scale = 2, table = "l004")
    private BigDecimal totalInterest;

    @Column(name = "security_amount", precision = 15, scale = 2, table = "l004")
    private BigDecimal securityAmount;

    @Column(name = "down_payment_collateral", length = 255, table = "l004")
    private String collateral;

    @Column(name = "other_loan_purpose", length = 255, table = "l004")
    private String otherLoanPurpose;

    @Column(name = "tenure_unit", length = 20, table = "l004")
    private String tenureUnit;

    // =========================================================================
    // Table: l005 (Employment & Financials)
    // =========================================================================
    @Column(name = "employment_type", length = 50, table = "l005")
    private String employmentType;

    @Column(name = "annual_income", precision = 15, scale = 2, table = "l005")
    private BigDecimal annualIncome;

    @Column(name = "designation", length = 100, table = "l005")
    private String designation;

    @Column(name = "employer_business_name", length = 200, table = "l005")
    private String employerName;

    @Column(name = "location", length = 100, table = "l005")
    private String location;

    @Column(name = "take_home_pay", precision = 15, scale = 2, table = "l005")
    private BigDecimal takeHomePay;

    @Column(name = "deductions_or_emis_payable", precision = 15, scale = 2, table = "l005")
    private BigDecimal deductions;

    @Column(name = "bank_name", length = 150, table = "l005")
    private String bankName;

    @Column(name = "primary_bank_account", columnDefinition = "BYTEA", table = "l005")
    private byte[] primaryBankAccount;

    @Column(name = "account_statement_consent", length = 50, table = "l005")
    private String accountStatementConsent;

    @Column(name = "cibil_liability_check", length = 50, table = "l005")
    private String cibilLiabilityCheck;

    @Column(name = "debt_to_income_ratio", precision = 8, scale = 4, table = "l005")
    private BigDecimal debtToIncomeRatio;

    @Column(name = "loan_to_value_ratio", precision = 8, scale = 4, table = "l005")
    private BigDecimal loanToValueRatio;

    @Column(name = "debt_service_coverage_ratio", precision = 8, scale = 4, table = "l005")
    private BigDecimal debtServiceCoverageRatio;

    @Column(name = "net_disposable_income", precision = 15, scale = 2, table = "l005")
    private BigDecimal netDisposableIncome;

    @Column(name = "occupation", length = 100, table = "l005")
    private String occupation;

    @Column(name = "employer_type", length = 100, table = "l005")
    private String employerType;

    @Column(name = "years_of_service", length = 50, table = "l005")
    private String yearsOfService;

    @Column(name = "income_source", length = 100, table = "l005")
    private String incomeSource;

    @Column(name = "emp_email_id", length = 150, table = "l005")
    private String empEmailId;

    @Column(name = "emi_bounce", length = 20, table = "l005")
    private String emiBounce;

    @Column(name = "emi_bounce_count", table = "l005")
    private Integer emiBounceCount;

    // =========================================================================
    // Table: l006 (Lead Address Details)
    // =========================================================================
    @Column(name = "particulars", length = 100, table = "l006")
    private String addressType;

    @Column(name = "house_flat_no", length = 100, table = "l006")
    private String flatBuildingName;

    @Column(name = "street_address", length = 255, table = "l006")
    private String streetRoad;

    @Column(name = "locality", length = 150, table = "l006")
    private String landmark;

    @Column(name = "city", length = 100, table = "l006")
    private String city;

    @Column(name = "state", length = 100, table = "l006")
    private String state;

    @Column(name = "pincode", length = 10, table = "l006")
    private String addressPinCode;

    // Transient collections for DTO responses
    @Transient
    @Builder.Default
    private List<AddressDetailDto> addressDetails = new ArrayList<>();

    @Transient
    @Builder.Default
    private List<CreditFacilityDto> existingBankCreditFacilities = new ArrayList<>();

    @Transient
    @Builder.Default
    private List<CreditFacilityDto> otherBankCreditFacilities = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (status == null || status.trim().isEmpty()) status = "Pending";
        if (customerType == null || customerType.trim().isEmpty()) customerType = "Normal";
        if (residentialStatus == null || residentialStatus.trim().isEmpty()) residentialStatus = "Resident";
        if (tenureUnit == null || tenureUnit.trim().isEmpty()) tenureUnit = "month";
        if (panValidationStatus == null) panValidationStatus = "VERIFIED";
        if (aadhaarValidationStatus == null) aadhaarValidationStatus = "VERIFIED";
        if (dedupeStatus == null) dedupeStatus = "CLEARED";
        if (blacklistStatus == null) blacklistStatus = "CLEARED";
        if (accountStatementConsent == null) accountStatementConsent = "CONSENTED";
        if (cibilLiabilityCheck == null) cibilLiabilityCheck = "PASSED";
        if (emiBounce == null || emiBounce.trim().isEmpty()) emiBounce = "No";
        if (emiBounceCount == null) emiBounceCount = 0;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
