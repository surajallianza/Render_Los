package com.los.bank.lead.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete Data Transfer Object for Lead requests and responses.
 * Maps across bank database tables l001 through l008.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeadDto {

    @JsonProperty("leadId")
    @JsonAlias({"lead_id", "id"})
    private String leadId;

    @JsonProperty("namePrefix")
    @JsonAlias({"name_prefix", "prefix", "title"})
    private String namePrefix;

    @JsonProperty("firstName")
    @JsonAlias({"first_name", "firstNameBusinessName", "first_name_business_name"})
    private String firstName;

    @JsonProperty("middleName")
    @JsonAlias({"middle_name"})
    private String middleName;

    @JsonProperty("lastName")
    @JsonAlias({"last_name"})
    private String lastName;

    @JsonProperty("customerName")
    @JsonAlias({"customer_name"})
    private String customerName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("dateOfBirth")
    @JsonAlias({"date_of_birth", "dob"})
    private String dateOfBirth;

    @JsonProperty("passportExpiryDate")
    @JsonAlias({"passport_expiry_date"})
    private String passportExpiryDate;

    @JsonProperty("age")
    private Integer age;

    @JsonProperty("customerType")
    @JsonAlias({"customer_type"})
    private String customerType;

    @JsonProperty("mobileNumber")
    @JsonAlias({"mobile_number", "mobile"})
    private String mobileNumber;

    @JsonProperty("email")
    @JsonAlias({"email_address", "emailAddress"})
    private String email;

    @JsonProperty("panCard")
    @JsonAlias({"pan_card", "pan_number", "panNumber", "pan"})
    private String panCard;

    @JsonProperty("aadhaarCard")
    @JsonAlias({"aadhaar_card", "aadhaar_number", "aadhaarNumber", "aadhaar"})
    private String aadhaarCard;

    @JsonProperty("branch")
    @JsonAlias({"branch_code", "branch_id", "branchId"})
    private String branch;

    @JsonProperty("addressDetails")
    @JsonAlias({"address_details", "addresses"})
    @Builder.Default
    private List<AddressDetailDto> addressDetails = new ArrayList<>();

    @JsonProperty("residentialStatus")
    @JsonAlias({"residential_status"})
    private String residentialStatus;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("maritalStatus")
    @JsonAlias({"marital_status"})
    private String maritalStatus;

    @JsonProperty("passportNo")
    @JsonAlias({"passport_no", "passport_number", "passportNumber"})
    private String passportNo;

    @JsonProperty("dependents")
    @JsonAlias({"number_of_dependents", "numberOfDependents"})
    private String dependents;

    @JsonProperty("loanProductType")
    @JsonAlias({"loan_product_type"})
    private String loanProductType;

    @JsonProperty("loanAmount")
    @JsonAlias({"loan_amount"})
    private String loanAmount;

    @JsonProperty("interestRate")
    @JsonAlias({"interest_rate"})
    private String interestRate;

    @JsonProperty("loanPurpose")
    @JsonAlias({"loan_purpose", "purpose_of_loan", "purposeOfLoan"})
    private String loanPurpose;

    @JsonProperty("otherLoanPurpose")
    @JsonAlias({"other_loan_purpose"})
    private String otherLoanPurpose;

    @JsonProperty("securityAmount")
    @JsonAlias({"security_amount"})
    private String securityAmount;

    @JsonProperty("tenure")
    private String tenure;

    @JsonProperty("tenureUnit")
    @JsonAlias({"tenure_unit"})
    private String tenureUnit;

    @JsonProperty("instalments")
    @JsonAlias({"number_of_instalments", "numberOfInstalments"})
    private String instalments;

    @JsonProperty("monthlyEmi")
    @JsonAlias({"monthly_emi", "emi"})
    private String monthlyEmi;

    @JsonProperty("totalInterest")
    @JsonAlias({"total_interest"})
    private String totalInterest;

    @JsonProperty("collateral")
    @JsonAlias({"down_payment_collateral", "downPaymentCollateral"})
    private String collateral;

    @JsonProperty("employmentType")
    @JsonAlias({"employment_type"})
    private String employmentType;

    @JsonProperty("occupation")
    private String occupation;

    @JsonProperty("employerType")
    @JsonAlias({"employer_type"})
    private String employerType;

    @JsonProperty("yearsOfService")
    @JsonAlias({"years_of_service"})
    private String yearsOfService;

    @JsonProperty("incomeSource")
    @JsonAlias({"income_source"})
    private String incomeSource;

    @JsonProperty("annualIncome")
    @JsonAlias({"annual_income"})
    private String annualIncome;

    @JsonProperty("designation")
    private String designation;

    @JsonProperty("employerName")
    @JsonAlias({"employer_name", "employer_business_name", "employerBusinessName"})
    private String employerName;

    @JsonProperty("empEmailId")
    @JsonAlias({"emp_email_id", "employer_email"})
    private String empEmailId;

    @JsonProperty("location")
    private String location;

    @JsonProperty("state")
    private String state;

    @JsonProperty("takeHomePay")
    @JsonAlias({"take_home_pay"})
    private String takeHomePay;

    @JsonProperty("deductions")
    @JsonAlias({"deductions_or_emis_payable", "deductionsOrEmisPayable"})
    private String deductions;

    @JsonProperty("bankName")
    @JsonAlias({"bank_name"})
    private String bankName;

    @JsonProperty("primaryBankAccount")
    @JsonAlias({"primary_bank_account"})
    private String primaryBankAccount;

    @JsonProperty("emiBounce")
    @JsonAlias({"emi_bounce", "isEmiBounce", "is_emi_bounce"})
    private String emiBounce;

    @JsonProperty("emiBounceCount")
    @JsonAlias({"emi_bounce_count", "bounceCount", "bounce_count"})
    private Integer emiBounceCount;

    @JsonProperty("existingBankCreditFacilities")
    @JsonAlias({"existing_bank_credit_facilities", "existingCreditFacilities"})
    @Builder.Default
    private List<CreditFacilityDto> existingBankCreditFacilities = new ArrayList<>();

    @JsonProperty("otherBankCreditFacilities")
    @JsonAlias({"other_bank_credit_facilities", "otherCreditFacilities"})
    @Builder.Default
    private List<CreditFacilityDto> otherBankCreditFacilities = new ArrayList<>();

    @JsonProperty("acquisitionChannel")
    @JsonAlias({"acquisition_channel", "lead_acquisition_channel", "leadAcquisitionChannel"})
    private String acquisitionChannel;

    @JsonProperty("referralDate")
    @JsonAlias({"referral_date", "date"})
    private String referralDate;

    @JsonProperty("partnerId")
    @JsonAlias({"partner_id", "sourcing_agent_partner_id", "sourcingAgentPartnerId"})
    private String partnerId;

    @JsonProperty("partnerName")
    @JsonAlias({"partner_name", "agent_partner_name", "agentPartnerName"})
    private String partnerName;

    @JsonProperty("employeeId")
    @JsonAlias({"employee_id", "assigned_user_id", "assignedUserId"})
    private String employeeId;

    @JsonProperty("employeeName")
    @JsonAlias({"employee_name", "assigned_user_name", "assignedUserName"})
    private String employeeName;

    @JsonProperty("status")
    @JsonAlias({"lead_status", "leadStatus"})
    private String status;

    @JsonProperty("createdAt")
    @JsonAlias({"created_at"})
    private String createdAt;

    @JsonProperty("updatedAt")
    @JsonAlias({"updated_at"})
    private String updatedAt;

    public void setEmiBounce(Object val) {
        if (val == null) {
            this.emiBounce = null;
        } else if (val instanceof Boolean) {
            this.emiBounce = ((Boolean) val) ? "Yes" : "No";
        } else {
            this.emiBounce = val.toString().trim();
        }
    }

    public void setEmiBounceCount(Object val) {
        if (val == null) {
            this.emiBounceCount = null;
        } else if (val instanceof Number) {
            this.emiBounceCount = ((Number) val).intValue();
        } else {
            String s = val.toString().trim();
            if (s.isEmpty()) {
                this.emiBounceCount = null;
            } else {
                try {
                    this.emiBounceCount = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    this.emiBounceCount = null;
                }
            }
        }
    }

    /**
     * Resolves the full composite customer name if not directly supplied.
     */
    public String getEffectiveCustomerName() {
        if (customerName != null && !customerName.trim().isEmpty()) {
            return customerName.trim();
        }
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        StringBuilder sb = new StringBuilder();
        if (namePrefix != null && !namePrefix.trim().isEmpty()) {
            sb.append(namePrefix.trim()).append(" ");
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            sb.append(firstName.trim()).append(" ");
        }
        if (middleName != null && !middleName.trim().isEmpty()) {
            sb.append(middleName.trim()).append(" ");
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            sb.append(lastName.trim());
        }
        return sb.toString().trim();
    }
}
