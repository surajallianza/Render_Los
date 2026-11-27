package com.los.bank.lead.service;

import com.los.bank.lead.dto.AddressDetailDto;
import com.los.bank.lead.dto.LeadAssignRequestDto;
import com.los.bank.lead.dto.LeadDto;
import com.los.bank.lead.exception.LeadValidationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Service providing comprehensive validation for Lead operations.
 */
@Service
public class LeadValidationService {

    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9]{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^[0-9]{12}$");
    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[0-9]{6}$");

    /**
     * Validates full lead payload for creation.
     */
    public void validateCreate(LeadDto lead) {
        if (lead == null) {
            throw new LeadValidationException("Lead request body cannot be null.");
        }

        if (isBlank(lead.getFirstName())) {
            throw new LeadValidationException("First name is required.");
        }

        if (isBlank(lead.getLastName())) {
            throw new LeadValidationException("Last name is required.");
        }

        if (isBlank(lead.getDateOfBirth())) {
            throw new LeadValidationException("Please provide a valid date of birth.");
        }
        try {
            LocalDate dob = LocalDate.parse(lead.getDateOfBirth().trim());
            if (dob.isAfter(LocalDate.now())) {
                throw new LeadValidationException("Please provide a valid date of birth.");
            }
        } catch (DateTimeParseException ex) {
            throw new LeadValidationException("Please provide a valid date of birth.");
        }

        if (isBlank(lead.getMobileNumber()) || !MOBILE_PATTERN.matcher(lead.getMobileNumber().trim()).matches()) {
            throw new LeadValidationException("Please enter a valid 10-digit mobile number.");
        }

        if (isBlank(lead.getEmail()) || !EMAIL_PATTERN.matcher(lead.getEmail().trim()).matches()) {
            throw new LeadValidationException("Please enter a valid email address.");
        }

        if (isBlank(lead.getPanCard()) || !PAN_PATTERN.matcher(lead.getPanCard().trim().toUpperCase()).matches()) {
            throw new LeadValidationException("Please enter a valid PAN number.");
        }

        if (isBlank(lead.getAadhaarCard()) || !AADHAAR_PATTERN.matcher(lead.getAadhaarCard().trim()).matches()) {
            throw new LeadValidationException("Please enter a valid 12-digit Aadhaar number.");
        }

        if (isBlank(lead.getBranch())) {
            throw new LeadValidationException("Branch is required.");
        }

        if (isBlank(lead.getGender())) {
            throw new LeadValidationException("Please select a valid gender.");
        }

        if (isBlank(lead.getMaritalStatus())) {
            throw new LeadValidationException("Marital status is required.");
        }

        if (isBlank(lead.getLoanProductType())) {
            throw new LeadValidationException("Please select a loan product type.");
        }

        if (isBlank(lead.getLoanAmount())) {
            throw new LeadValidationException("Loan amount must be greater than zero.");
        }
        try {
            BigDecimal amount = new BigDecimal(lead.getLoanAmount().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new LeadValidationException("Loan amount must be greater than zero.");
            }
        } catch (NumberFormatException ex) {
            throw new LeadValidationException("Loan amount must be greater than zero.");
        }

        if (isBlank(lead.getInterestRate())) {
            throw new LeadValidationException("Please enter a valid interest rate.");
        }
        try {
            BigDecimal rate = new BigDecimal(lead.getInterestRate().trim());
            if (rate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new LeadValidationException("Please enter a valid interest rate.");
            }
        } catch (NumberFormatException ex) {
            throw new LeadValidationException("Please enter a valid interest rate.");
        }

        if (isBlank(lead.getTenure())) {
            throw new LeadValidationException("Loan tenure must be greater than zero.");
        }
        try {
            int tenure = Integer.parseInt(lead.getTenure().trim());
            if (tenure <= 0) {
                throw new LeadValidationException("Loan tenure must be greater than zero.");
            }
        } catch (NumberFormatException ex) {
            throw new LeadValidationException("Loan tenure must be greater than zero.");
        }

        if (isBlank(lead.getEmploymentType())) {
            throw new LeadValidationException("Employment type is required.");
        }

        if (isBlank(lead.getAnnualIncome())) {
            throw new LeadValidationException("Please enter a valid annual income.");
        }
        try {
            BigDecimal income = new BigDecimal(lead.getAnnualIncome().trim());
            if (income.compareTo(BigDecimal.ZERO) < 0) {
                throw new LeadValidationException("Please enter a valid annual income.");
            }
        } catch (NumberFormatException ex) {
            throw new LeadValidationException("Please enter a valid annual income.");
        }

        if (lead.getAddressDetails() == null || lead.getAddressDetails().isEmpty()) {
            throw new LeadValidationException("At least one address is required.");
        }

        for (AddressDetailDto addr : lead.getAddressDetails()) {
            if (addr == null) {
                throw new LeadValidationException("Address details item cannot be null.");
            }
            if (isBlank(addr.getCity())) {
                throw new LeadValidationException("City is required.");
            }
            if (isBlank(addr.getState())) {
                throw new LeadValidationException("State is required.");
            }
            if (isBlank(addr.getPinCode()) || !PINCODE_PATTERN.matcher(addr.getPinCode().trim()).matches()) {
                throw new LeadValidationException("Please enter a valid PIN code.");
            }
        }

        if (isBlank(lead.getAcquisitionChannel())) {
            throw new LeadValidationException("Acquisition channel is required.");
        }

        validateEmiBounce(lead);
    }

    /**
     * Validates lead ID and fields for update.
     */
    public void validateUpdate(String leadId, LeadDto lead) {
        if (isBlank(leadId)) {
            throw new LeadValidationException("Lead ID is required for update.");
        }
        if (lead == null) {
            throw new LeadValidationException("Update payload cannot be null.");
        }

        // Validate optional formats if present during update
        if (lead.getMobileNumber() != null && !lead.getMobileNumber().trim().isEmpty()) {
            if (!MOBILE_PATTERN.matcher(lead.getMobileNumber().trim()).matches()) {
                throw new LeadValidationException("Please enter a valid 10-digit mobile number.");
            }
        }
        if (lead.getEmail() != null && !lead.getEmail().trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(lead.getEmail().trim()).matches()) {
                throw new LeadValidationException("Please enter a valid email address.");
            }
        }
        if (lead.getPanCard() != null && !lead.getPanCard().trim().isEmpty()) {
            if (!PAN_PATTERN.matcher(lead.getPanCard().trim().toUpperCase()).matches()) {
                throw new LeadValidationException("Please enter a valid PAN number.");
            }
        }
        if (lead.getAadhaarCard() != null && !lead.getAadhaarCard().trim().isEmpty()) {
            if (!AADHAAR_PATTERN.matcher(lead.getAadhaarCard().trim()).matches()) {
                throw new LeadValidationException("Please enter a valid 12-digit Aadhaar number.");
            }
        }
        if (lead.getAddressDetails() != null) {
            for (AddressDetailDto addr : lead.getAddressDetails()) {
                if (addr != null && addr.getPinCode() != null && !addr.getPinCode().trim().isEmpty()) {
                    if (!PINCODE_PATTERN.matcher(addr.getPinCode().trim()).matches()) {
                        throw new LeadValidationException("Please enter a valid PIN code.");
                    }
                }
            }
        }

        validateEmiBounce(lead);
    }

    /**
     * Validates EMI Bounce ('Yes'/'No') and EMI Bounce Count business rules.
     */
    private void validateEmiBounce(LeadDto lead) {
        if (lead == null) return;

        String bounce = lead.getEmiBounce();
        Integer count = lead.getEmiBounceCount();

        if (bounce != null && !bounce.trim().isEmpty()) {
            String b = bounce.trim();
            boolean isYes = b.equalsIgnoreCase("Yes") || b.equalsIgnoreCase("true");
            boolean isNo = b.equalsIgnoreCase("No") || b.equalsIgnoreCase("false");

            if (!isYes && !isNo) {
                throw new LeadValidationException("EMI bounce must be either 'Yes' or 'No'.");
            }

            if (isYes) {
                if (count == null || count <= 0) {
                    throw new LeadValidationException("EMI bounce count must be greater than zero when EMI bounce is 'Yes'.");
                }
            } else {
                if (count != null && count > 0) {
                    throw new LeadValidationException("EMI bounce count is applicable only when EMI bounce is 'Yes'.");
                }
            }
        } else if (count != null && count > 0) {
            throw new LeadValidationException("EMI bounce count is applicable only when EMI bounce is 'Yes'.");
        }
    }

    /**
     * Validates assignment request payload.
     */
    public void validateAssign(String leadId, LeadAssignRequestDto assignDto) {
        if (isBlank(leadId)) {
            throw new LeadValidationException("Lead ID is required.");
        }
        if (assignDto == null || isBlank(assignDto.getEmployeeId())) {
            throw new LeadValidationException("Employee ID is required for assignment.");
        }
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
