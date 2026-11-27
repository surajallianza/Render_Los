package com.los.bank.loan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete Retail Loan Application Form DTO matching every field in the reference JSON payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetailLoanFormDto {

    private Long id;

    // A. Loan Account and Applicant Details
    private String loanAccountNumber;
    private String branch;
    private String effectiveDate;
    private String customerId;
    private String title;
    private String firstName;
    private String middleName;
    private String lastName;
    private String productCode;
    private String productName;
    private String accountStatus;
    private String modeOfOperation;
    private String lastTransactionDate;
    private Integer dpd;

    // B. Related Parties
    @Builder.Default
    private List<RelatedPartyDto> relatedParties = new ArrayList<>();

    // C. Sanction and Loan Details
    private String sanctionedDate;
    private String sanctioningAuthority;
    private String loanAmount;
    private String tenureMonths;
    private String interestRate;
    private String offsetRate;
    private String totalInterestRate;
    private String emiApplicable;
    private String emiFixDate;
    private String emiType;
    private String moratoriumApplicable;
    private String moratoriumMonths;
    private String moratoriumFrom;
    private String moratoriumEndDate;
    private String principalMoratorium;
    private String interestMoratorium;
    private String bulletPayment;
    private String instalmentAmount;
    private String instalmentStartDate;
    private String instalmentEndDate;
    private String expiryDate;

    // D. Repayment Schedule (Summary fields + generated chart)
    @JsonProperty("Month")
    private String month;

    @JsonProperty("Principal Amount (Rs.)")
    private String principalAmountRs;

    @JsonProperty("Installment Amount (Rs.)")
    private String installmentAmountRs;

    @JsonProperty("Interest (Rs.)")
    private String interestRs;

    @JsonProperty("Principal (Rs.)")
    private String principalRs;

    @JsonProperty("Balance Amount (Rs.)")
    private String balanceAmountRs;

    private List<RepaymentScheduleRowDto> repaymentSchedule;

    // E. Purpose and Classification
    private String purposeCode;
    private String prioritySector;
    private String prioritySectorCode;
    private String industryType;
    private String industrySubType;
    private String physicallyChallenged;
    private String termCode;

    // F. Security and Collateral
    private String securityId;
    private String securityType;
    private String securityEffectiveDate;
    private String assetType;
    private String securityAmount;
    private String marginPercent;
    private String assetAddress;
    private String assetAddressLine2;
    private String city;
    private String state;
    private String pincode;
    private String valuationAmount;
    private String valuerName;
    private String valuationDate;
    private String inspectionBy;
    private String inspectionDate;
    private String inspectionReportName;
    private String securityValue;

    // G. Insurance
    private String policyType;
    private String premiumFrequency;
    private String policyStatus;
    private String autoRenew;
    private String premiumAmount;
    private String policyNumber;
    private String sumAssured;
    private String insuranceCompany;
    private String insuranceIssueDate;
    private String insuranceExpiryDate;

    // H. Disbursement
    private String disbursementMode;
    private String disbursementAccountNumber;
    private String ifscCode;
    private String bankName;
    private String beneficiaryAccountName;
    private String beneficiaryMobileNumber;
    private String disbursementAmount;

    // I. Remarks
    private String remarks;

    // Audit metadata
    private String createdAt;
    private String updatedAt;
}
