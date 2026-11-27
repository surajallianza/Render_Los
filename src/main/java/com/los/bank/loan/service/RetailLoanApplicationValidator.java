package com.los.bank.loan.service;

import com.los.bank.loan.dto.RelatedPartyDto;
import com.los.bank.loan.dto.RetailLoanFormDto;
import com.los.bank.loan.exception.RetailLoanValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Comprehensive validator for Retail Loan Application payloads across all sections (A-I).
 */
@Component
public class RetailLoanApplicationValidator {

    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9]{10}$");
    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[0-9]{6}$");

    public void validateForCreateOrUpdate(RetailLoanFormDto form, boolean isUpdate) {
        if (form == null) {
            throw new RetailLoanValidationException("retailLoanForm payload is required.");
        }

        // A. Loan Account and Applicant Details
        if (!isUpdate && isBlank(form.getLoanAccountNumber())) {
            throw new RetailLoanValidationException("Loan account number is required.");
        }
        if (isBlank(form.getBranch())) {
            throw new RetailLoanValidationException("Branch code is required.");
        }
        if (isBlank(form.getCustomerId())) {
            throw new RetailLoanValidationException("Customer ID is required.");
        }
        if (isBlank(form.getProductCode())) {
            throw new RetailLoanValidationException("Product code is required.");
        }
        if (isBlank(form.getFirstName())) {
            throw new RetailLoanValidationException("Applicant first name is required.");
        }
        if (isBlank(form.getLastName())) {
            throw new RetailLoanValidationException("Applicant last name is required.");
        }
        if (isBlank(form.getAccountStatus())) {
            throw new RetailLoanValidationException("Account status is required.");
        }
        if (isBlank(form.getModeOfOperation())) {
            throw new RetailLoanValidationException("Mode of operation is required.");
        }
        if (form.getDpd() != null && form.getDpd() < 0) {
            throw new RetailLoanValidationException("DPD cannot be negative.");
        }
        validateOptionalDate(form.getEffectiveDate(), "effectiveDate");
        validateOptionalDate(form.getLastTransactionDate(), "lastTransactionDate");

        // B. Related Parties
        if (form.getRelatedParties() != null) {
            for (RelatedPartyDto party : form.getRelatedParties()) {
                if (party == null) {
                    throw new RetailLoanValidationException("Related party entry cannot be null.");
                }
                if (isBlank(party.getNameType())) {
                    throw new RetailLoanValidationException("Related party nameType is required.");
                }
                if (isBlank(party.getRelation())) {
                    throw new RetailLoanValidationException("Related party relation is required.");
                }
                if (isBlank(party.getCustomerNo())) {
                    throw new RetailLoanValidationException("Related party customerNo is required.");
                }
                if (isBlank(party.getFirstName())) {
                    throw new RetailLoanValidationException("Related party firstName is required.");
                }
            }
        }

        // C. Sanction and Loan Details
        validateOptionalDate(form.getSanctionedDate(), "sanctionedDate");
        BigDecimal loanAmount = parsePositiveDecimal(form.getLoanAmount(), "Loan amount must be greater than zero.");
        int tenureMonths = parsePositiveInt(form.getTenureMonths(), "Tenure months must be greater than zero.");
        parseNonNegativeDecimal(form.getInterestRate(), "Interest rate must be zero or greater.");
        if (!isBlank(form.getOffsetRate())) {
            parseDecimal(form.getOffsetRate(), "Invalid offsetRate format.");
        }
        if (!isBlank(form.getTotalInterestRate())) {
            parseNonNegativeDecimal(form.getTotalInterestRate(), "Total interest rate must be zero or greater.");
        }

        // Critical Business Rule: emiApplicable validation
        if (isBlank(form.getEmiApplicable())) {
            throw new RetailLoanValidationException("emiApplicable is required and must be either 'Yes' or 'No'.");
        }
        String emiApp = form.getEmiApplicable().trim();
        if (!emiApp.equalsIgnoreCase("Yes") && !emiApp.equalsIgnoreCase("No")) {
            throw new RetailLoanValidationException("emiApplicable must be either 'Yes' or 'No'.");
        }

        if (emiApp.equalsIgnoreCase("Yes")) {
            if (isBlank(form.getEmiType())) {
                throw new RetailLoanValidationException("emiType is required when emiApplicable is 'Yes'.");
            }
            if (!isBlank(form.getInstalmentAmount())) {
                parsePositiveDecimal(form.getInstalmentAmount(), "Instalment amount must be greater than zero when EMI is applicable.");
            }
            validateOptionalDate(form.getInstalmentStartDate(), "instalmentStartDate");
            validateOptionalDate(form.getInstalmentEndDate(), "instalmentEndDate");
        }
        validateOptionalDate(form.getExpiryDate(), "expiryDate");

        // Moratorium validation
        if (!isBlank(form.getMoratoriumApplicable())) {
            String mor = form.getMoratoriumApplicable().trim();
            if (!mor.equalsIgnoreCase("Yes") && !mor.equalsIgnoreCase("No")) {
                throw new RetailLoanValidationException("moratoriumApplicable must be either 'Yes' or 'No'.");
            }
            if (mor.equalsIgnoreCase("Yes")) {
                parsePositiveInt(form.getMoratoriumMonths(), "moratoriumMonths must be greater than zero when moratorium is applicable.");
                validateOptionalDate(form.getMoratoriumFrom(), "moratoriumFrom");
                validateOptionalDate(form.getMoratoriumEndDate(), "moratoriumEndDate");
            }
        }

        // F. Security and Collateral
        if (isBlank(form.getSecurityType())) {
            throw new RetailLoanValidationException("Security type is required.");
        }
        if (!"Unsecured".equalsIgnoreCase(form.getSecurityType().trim())) {
            if (!isBlank(form.getSecurityAmount())) {
                parseNonNegativeDecimal(form.getSecurityAmount(), "Security amount cannot be negative.");
            }
            if (!isBlank(form.getMarginPercent())) {
                parseNonNegativeDecimal(form.getMarginPercent(), "Margin percent cannot be negative.");
            }
        }
        if (!isBlank(form.getPincode()) && !isNotApplicable(form.getPincode())
                && !PINCODE_PATTERN.matcher(form.getPincode().trim()).matches()) {
            throw new RetailLoanValidationException("Please enter a valid 6-digit pincode.");
        }

        // G. Insurance
        if (!isBlank(form.getPolicyNumber()) && !isNotApplicable(form.getPolicyNumber())) {
            if (!isBlank(form.getPremiumAmount())) {
                parseNonNegativeDecimal(form.getPremiumAmount(), "Premium amount cannot be negative.");
            }
            if (!isBlank(form.getSumAssured())) {
                parseNonNegativeDecimal(form.getSumAssured(), "Sum assured cannot be negative.");
            }
            validateOptionalDate(form.getInsuranceIssueDate(), "insuranceIssueDate");
            validateOptionalDate(form.getInsuranceExpiryDate(), "insuranceExpiryDate");
        }

        // H. Disbursement
        if (isBlank(form.getDisbursementMode())) {
            throw new RetailLoanValidationException("Disbursement mode is required.");
        }
        if (isBlank(form.getDisbursementAccountNumber())) {
            throw new RetailLoanValidationException("Disbursement account number is required.");
        }
        if (isBlank(form.getIfscCode()) || !IFSC_PATTERN.matcher(form.getIfscCode().trim().toUpperCase()).matches()) {
            throw new RetailLoanValidationException("Please enter a valid 11-character IFSC code (e.g. HDFC0001234).");
        }
        if (isBlank(form.getBeneficiaryAccountName())) {
            throw new RetailLoanValidationException("Beneficiary account name is required.");
        }
        if (isBlank(form.getBeneficiaryMobileNumber())
                || !MOBILE_PATTERN.matcher(form.getBeneficiaryMobileNumber().trim()).matches()) {
            throw new RetailLoanValidationException("Please enter a valid 10-digit beneficiary mobile number.");
        }
        BigDecimal disbAmount = parsePositiveDecimal(form.getDisbursementAmount(), "Disbursement amount must be greater than zero.");
        if (disbAmount.compareTo(loanAmount) > 0) {
            throw new RetailLoanValidationException("Disbursement amount cannot exceed sanctioned loan amount (" + tenureMonths + "M).");
        }
    }

    private void validateOptionalDate(String value, String fieldName) {
        if (isBlank(value) || isNotApplicable(value) || "Current Date".equalsIgnoreCase(value.trim())) {
            return;
        }
        try {
            LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new RetailLoanValidationException("Invalid date format for '" + fieldName + "'. Expected YYYY-MM-DD.");
        }
    }

    private BigDecimal parsePositiveDecimal(String value, String errorMessage) {
        BigDecimal num = parseDecimal(value, errorMessage);
        if (num.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RetailLoanValidationException(errorMessage);
        }
        return num;
    }

    private BigDecimal parseNonNegativeDecimal(String value, String errorMessage) {
        BigDecimal num = parseDecimal(value, errorMessage);
        if (num.compareTo(BigDecimal.ZERO) < 0) {
            throw new RetailLoanValidationException(errorMessage);
        }
        return num;
    }

    private BigDecimal parseDecimal(String value, String errorMessage) {
        if (isBlank(value)) {
            throw new RetailLoanValidationException(errorMessage);
        }
        try {
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            throw new RetailLoanValidationException(errorMessage);
        }
    }

    private int parsePositiveInt(String value, String errorMessage) {
        if (isBlank(value)) {
            throw new RetailLoanValidationException(errorMessage);
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new RetailLoanValidationException(errorMessage);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new RetailLoanValidationException(errorMessage);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isNotApplicable(String s) {
        if (s == null) return false;
        String t = s.trim();
        return "NA".equalsIgnoreCase(t) || "Not Applicable".equalsIgnoreCase(t);
    }
}
