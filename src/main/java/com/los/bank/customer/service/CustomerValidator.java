package com.los.bank.customer.service;

import com.los.bank.customer.config.CustomerLookupProperties;
import com.los.bank.customer.dto.*;
import com.los.bank.customer.exception.CustomerValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class CustomerValidator {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z\\s.'-]{2,49}$");
    private static final Pattern OPTIONAL_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z\\s.'-]{0,49}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern CKYC_PATTERN = Pattern.compile("^\\d{14}$");
    private static final Pattern PINCODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^\\d{9,18}$");
    private static final Pattern FINANCIAL_YEAR_PATTERN = Pattern.compile("^\\d{4}-\\d{2}$");

    private final CustomerLookupHelper lookupHelper;
    private final CustomerCryptoService cryptoService;

    public CustomerValidator(CustomerLookupHelper lookupHelper, CustomerCryptoService cryptoService) {
        this.lookupHelper = lookupHelper;
        this.cryptoService = cryptoService;
    }

    /**
     * Holds resolved lookup IDs discovered during validation to avoid double-resolving.
     */
    public static class ResolvedLookups {
        public Long titleLookupId;
        public Long genderLookupId;
        public Long maritalStatusLookupId;
        public Long customerTypeLookupId;
        public Long kycStatusLookupId;
        public Long fatherTitleLookupId;
        public Long motherTitleLookupId;
        public Long spouseTitleLookupId;
        public Long residentialStatusLookupId;
        public Long residentialTypeLookupId;
        public Long stateLookupId;
        public Long employmentTypeLookupId;
        public Long employerTypeLookupId;
        public Long listedStatusLookupId;
        public Long incomeTaxFilerStatusLookupId;
        public Long riskCategoryLookupId;
        public Long accountTypeLookupId;
        public Long accountStatusLookupId;
        public String derivedGenderCode;
        public List<Long> previousEmploymentTypeLookupIds = new ArrayList<>();
    }

    /**
     * Values already saved for a draft customer that other sections depend on.
     * Used when a step is saved on its own (for example income without employment).
     */
    public static class StoredContext {
        public Long maritalStatusLookupId;
        public Long employmentTypeLookupId;
        public Long incomeTaxFilerStatusLookupId;
    }

    /**
     * Full validation: every mandatory section must be present.
     */
    public ResolvedLookups validate(CustomerSaveRequest request, boolean isUpdate) {
        return validate(request, isUpdate, false, null);
    }

    /**
     * Step validation for the onboarding wizard: only the sections present in the
     * request are validated (each one completely). A missing section is left as stored.
     * Rules that depend on another section use the request value when present,
     * otherwise the stored value from {@code stored}.
     */
    public ResolvedLookups validateSections(CustomerSaveRequest request, boolean isUpdate, StoredContext stored) {
        return validate(request, isUpdate, true, stored);
    }

    private ResolvedLookups validate(CustomerSaveRequest request, boolean isUpdate, boolean partial, StoredContext stored) {
        Map<String, String> errors = new LinkedHashMap<>();
        ResolvedLookups resolved = new ResolvedLookups();
        CustomerLookupProperties props = lookupHelper.getProperties();

        if (request == null) {
            throw new CustomerValidationException("request", "Request body cannot be null");
        }

        // 1. Personal
        PersonalRequest personal = request.getPersonal();
        if (personal == null) {
            if (!partial) {
                errors.put("personal", "Personal details are required");
            }
        } else {
            // Title
            if (isBlank(personal.getTitle())) {
                errors.put("personal.title", "Title is required");
            } else {
                resolved.titleLookupId = lookupHelper.resolveId("Title", props.getTitle(), personal.getTitle(), "personal.title", errors);
            }

            // Names
            validateName(personal.getFirstName(), "personal.firstName", "First name", true, errors);
            validateName(personal.getLastName(), "personal.lastName", "Last name", true, errors);
            if (!isBlank(personal.getMiddleName())) {
                validateOptionalName(personal.getMiddleName(), "personal.middleName", "Middle name", errors);
            }

            // DOB
            LocalDate dob = personal.getDob();
            if (dob == null) {
                errors.put("personal.dob", "Date of birth is required");
            } else {
                if (dob.isAfter(LocalDate.now())) {
                    errors.put("personal.dob", "Date of birth cannot be in the future");
                } else {
                    int age = Period.between(dob, LocalDate.now()).getYears();
                    if (age > 80) {
                        errors.put("personal.dob", "Age cannot be above 80 years");
                    }
                }
            }

            // Gender: derive from title if not supplied
            String genderCode = personal.getGender();
            if (isBlank(genderCode)) {
                if (resolved.titleLookupId != null) {
                    genderCode = lookupHelper.deriveGenderCodeFromTitleId(resolved.titleLookupId);
                    resolved.derivedGenderCode = genderCode;
                }
            }
            if (isBlank(genderCode)) {
                errors.put("personal.gender", "Gender is required or could not be derived from title");
            } else {
                resolved.genderLookupId = lookupHelper.resolveId("Gender", props.getGender(), genderCode, "personal.gender", errors);
            }

            // Marital status
            if (isBlank(personal.getMaritalStatus())) {
                errors.put("personal.maritalStatus", "Marital status is required");
            } else {
                resolved.maritalStatusLookupId = lookupHelper.resolveId("Marital Status", props.getMaritalStatus(), personal.getMaritalStatus(), "personal.maritalStatus", errors);
            }

            // Customer type
            if (isBlank(personal.getCustomerType())) {
                errors.put("personal.customerType", "Customer type is required");
            } else {
                resolved.customerTypeLookupId = lookupHelper.resolveId("Customer Type", props.getCustomerType(), personal.getCustomerType(), "personal.customerType", errors);
            }

            // KYC status
            if (isBlank(personal.getKycStatus())) {
                errors.put("personal.kycStatus", "KYC status is required");
            } else {
                resolved.kycStatusLookupId = lookupHelper.resolveId("KYC Status", props.getKycStatus(), personal.getKycStatus(), "personal.kycStatus", errors);
            }

            // Mobile & Email
            if (isBlank(personal.getMobile()) || !MOBILE_PATTERN.matcher(personal.getMobile()).matches()) {
                errors.put("personal.mobile", "Mobile number must be a valid 10-digit number starting with 6-9");
            }
            if (!isBlank(personal.getAlternateContact()) && !MOBILE_PATTERN.matcher(personal.getAlternateContact()).matches()) {
                errors.put("personal.alternateContact", "Alternate contact must be a valid 10-digit number starting with 6-9");
            }
            if (isBlank(personal.getEmail()) || !EMAIL_PATTERN.matcher(personal.getEmail().trim()).matches()) {
                errors.put("personal.email", "Valid email address is required");
            }
        }

        // 2. Identity
        IdentityRequest identity = request.getIdentity();
        if (identity == null) {
            if (!partial) {
                errors.put("identity", "Identity details are required");
            }
        } else {
            String pan = identity.getPanNumber();
            if (isBlank(pan)) {
                errors.put("identity.panNumber", "PAN number is required");
            } else {
                String cleanPan = pan.trim().toUpperCase();
                identity.setPanNumber(cleanPan);
                if (isUpdate && cryptoService.isMasked(cleanPan)) {
                    // Allowed masked value on update
                } else if (!PAN_PATTERN.matcher(cleanPan).matches()) {
                    errors.put("identity.panNumber", "Invalid PAN format. Must be 5 uppercase letters, 4 digits, 1 uppercase letter");
                }
            }

            String aadhaar = identity.getAadhaarNumber();
            if (isBlank(aadhaar)) {
                errors.put("identity.aadhaarNumber", "Aadhaar number is required");
            } else {
                String cleanAadhaar = aadhaar.trim();
                identity.setAadhaarNumber(cleanAadhaar);
                if (isUpdate && cryptoService.isMasked(cleanAadhaar)) {
                    // Allowed masked value on update
                } else if (!AADHAAR_PATTERN.matcher(cleanAadhaar).matches()) {
                    errors.put("identity.aadhaarNumber", "Aadhaar number must be exactly 12 digits");
                }
            }

            String ckyc = identity.getCkycNumber();
            if (isBlank(ckyc)) {
                errors.put("identity.ckycNumber", "CKYC number is required");
            } else {
                String cleanCkyc = ckyc.trim();
                identity.setCkycNumber(cleanCkyc);
                if (isUpdate && cryptoService.isMasked(cleanCkyc)) {
                    // Allowed masked value on update
                } else if (!CKYC_PATTERN.matcher(cleanCkyc).matches()) {
                    errors.put("identity.ckycNumber", "CKYC number must be exactly 14 digits");
                }
            }
        }

        // 3. Family
        FamilyRequest family = request.getFamily();
        if (family == null) {
            if (!partial) {
                errors.put("family", "Family details are required");
            } else if (personal != null) {
                // Spouse rules depend on marital status, so personal and family are saved together.
                errors.put("family", "Family details must be sent together with personal details");
            }
        } else {
            if (isBlank(family.getFatherTitle())) {
                errors.put("family.fatherTitle", "Father title is required");
            } else {
                resolved.fatherTitleLookupId = lookupHelper.resolveId("Title", props.getTitle(), family.getFatherTitle(), "family.fatherTitle", errors);
            }
            validateName(family.getFatherFirstName(), "family.fatherFirstName", "Father first name", true, errors);
            validateName(family.getFatherLastName(), "family.fatherLastName", "Father last name", true, errors);
            if (!isBlank(family.getFatherMiddleName())) {
                validateOptionalName(family.getFatherMiddleName(), "family.fatherMiddleName", "Father middle name", errors);
            }

            if (isBlank(family.getMotherTitle())) {
                errors.put("family.motherTitle", "Mother title is required");
            } else {
                resolved.motherTitleLookupId = lookupHelper.resolveId("Title", props.getTitle(), family.getMotherTitle(), "family.motherTitle", errors);
            }
            validateName(family.getMotherFirstName(), "family.motherFirstName", "Mother first name", true, errors);
            validateName(family.getMotherLastName(), "family.motherLastName", "Mother last name", true, errors);
            if (!isBlank(family.getMotherMiddleName())) {
                validateOptionalName(family.getMotherMiddleName(), "family.motherMiddleName", "Mother middle name", errors);
            }

            // Spouse is required ONLY if marital status is Married
            Long maritalStatusId = personal != null
                    ? resolved.maritalStatusLookupId
                    : (stored != null ? stored.maritalStatusLookupId : null);
            boolean isMarried = maritalStatusId != null &&
                    lookupHelper.descriptionMatches(maritalStatusId, "Married");

            if (isMarried) {
                if (isBlank(family.getSpouseTitle())) {
                    errors.put("family.spouseTitle", "Spouse title is required when marital status is Married");
                } else {
                    resolved.spouseTitleLookupId = lookupHelper.resolveId("Title", props.getTitle(), family.getSpouseTitle(), "family.spouseTitle", errors);
                }
                validateName(family.getSpouseFirstName(), "family.spouseFirstName", "Spouse first name", true, errors);
                validateName(family.getSpouseLastName(), "family.spouseLastName", "Spouse last name", true, errors);
                if (!isBlank(family.getSpouseMiddleName())) {
                    validateOptionalName(family.getSpouseMiddleName(), "family.spouseMiddleName", "Spouse middle name", errors);
                }
            } else if (!isBlank(family.getSpouseTitle())) {
                resolved.spouseTitleLookupId = lookupHelper.resolveId("Title", props.getTitle(), family.getSpouseTitle(), "family.spouseTitle", errors);
            }
        }

        // 4. Address
        AddressRequest address = request.getAddress();
        if (address == null) {
            if (!partial) {
                errors.put("address", "Address details are required");
            }
        } else {
            if (isBlank(address.getResidentialStatus())) {
                errors.put("address.residentialStatus", "Residential status is required");
            } else {
                resolved.residentialStatusLookupId = lookupHelper.resolveId("Residential Status", props.getResidentialStatus(), address.getResidentialStatus(), "address.residentialStatus", errors);
            }

            if (isBlank(address.getResidentialType())) {
                errors.put("address.residentialType", "Residential type is required");
            } else {
                resolved.residentialTypeLookupId = lookupHelper.resolveId("Residential Type", props.getResidentialType(), address.getResidentialType(), "address.residentialType", errors);
            }

            if (isBlank(address.getFlatBuildingName())) {
                errors.put("address.flatBuildingName", "Flat or building name is required");
            }
            if (isBlank(address.getStreetRoad())) {
                errors.put("address.streetRoad", "Street or road is required");
            }
            if (isBlank(address.getCity())) {
                errors.put("address.city", "City is required");
            }

            if (isBlank(address.getState())) {
                errors.put("address.state", "State is required");
            } else {
                resolved.stateLookupId = lookupHelper.resolveId("State", props.getState(), address.getState(), "address.state", errors);
            }

            if (isBlank(address.getPincode()) || !PINCODE_PATTERN.matcher(address.getPincode().trim()).matches()) {
                errors.put("address.pincode", "Pincode must be exactly 6 digits");
            }
        }

        // 5. Employment & Previous Employment
        EmploymentRequest employment = request.getEmployment();
        boolean isSelfEmployed = false;
        if (employment == null) {
            if (!partial) {
                errors.put("employment", "Employment details are required");
            } else {
                boolean employmentKnown = stored != null && stored.employmentTypeLookupId != null;
                if (employmentKnown) {
                    isSelfEmployed = lookupHelper.descriptionMatches(stored.employmentTypeLookupId, "Self Employed");
                }
                if (request.getPreviousEmployment() != null) {
                    if (!employmentKnown) {
                        errors.put("previousEmployment", "Employment details must be saved before previous employment");
                    } else {
                        validatePreviousEmployment(request.getPreviousEmployment(), isSelfEmployed, resolved, errors);
                    }
                }
            }
        } else {
            if (isBlank(employment.getEmploymentType())) {
                errors.put("employment.employmentType", "Employment type is required");
            } else {
                resolved.employmentTypeLookupId = lookupHelper.resolveId("Employment Type", props.getEmploymentType(), employment.getEmploymentType(), "employment.employmentType", errors);
            }

            if (!isBlank(employment.getEmployerType())) {
                resolved.employerTypeLookupId = lookupHelper.resolveId("Employer Type", props.getEmployerType(), employment.getEmployerType(), "employment.employerType", errors);
            }
            if (!isBlank(employment.getListedStatus())) {
                resolved.listedStatusLookupId = lookupHelper.resolveId("Listed Status", props.getListedStatus(), employment.getListedStatus(), "employment.listedStatus", errors);
            }

            isSelfEmployed = resolved.employmentTypeLookupId != null &&
                    lookupHelper.descriptionMatches(resolved.employmentTypeLookupId, "Self Employed");

            if (isSelfEmployed) {
                // Self employed rules
                if (isBlank(employment.getEmployerName())) {
                    errors.put("employment.employerName", "Business name is required for self-employed customers");
                }
                if (isBlank(employment.getDesignation())) {
                    errors.put("employment.designation", "Nature of business is required for self-employed customers");
                }
                if (employment.getDateOfJoining() == null) {
                    errors.put("employment.dateOfJoining", "Business start date is required for self-employed customers");
                } else if (employment.getDateOfJoining().isAfter(LocalDate.now())) {
                    errors.put("employment.dateOfJoining", "Business start date cannot be in the future");
                }
                if (isBlank(employment.getLocation())) {
                    errors.put("employment.location", "Business location is required for self-employed customers");
                }
                if (employment.getDateOfRetirement() != null) {
                    errors.put("employment.dateOfRetirement", "Date of retirement must not be provided for self-employed customers");
                }
            } else {
                // Salaried rules
                if (isBlank(employment.getEmployerName())) {
                    errors.put("employment.employerName", "Employer name is required");
                }
                if (isBlank(employment.getDesignation())) {
                    errors.put("employment.designation", "Designation is required");
                }
                if (employment.getDateOfJoining() == null) {
                    errors.put("employment.dateOfJoining", "Date of joining is required");
                } else if (employment.getDateOfJoining().isAfter(LocalDate.now())) {
                    errors.put("employment.dateOfJoining", "Date of joining cannot be in the future");
                }
                if (!isBlank(employment.getEmployerEmail()) && !EMAIL_PATTERN.matcher(employment.getEmployerEmail().trim()).matches()) {
                    errors.put("employment.employerEmail", "Employer email must be a valid email address");
                }
                if (employment.getDateOfRetirement() != null && employment.getDateOfJoining() != null) {
                    if (!employment.getDateOfRetirement().isAfter(employment.getDateOfJoining())) {
                        errors.put("employment.dateOfRetirement", "Date of retirement must be after date of joining");
                    }
                }
                if (employment.getYearsOfService() != null && employment.getYearsOfService().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.put("employment.yearsOfService", "Years of service must be a positive number");
                }
            }
            validatePreviousEmployment(request.getPreviousEmployment(), isSelfEmployed, resolved, errors);
        }

        // 6. Income & Tax Returns
        IncomeRequest income = request.getIncome();
        if (income == null) {
            if (!partial) {
                errors.put("income", "Income details are required");
            } else if (request.getTaxReturns() != null) {
                if (stored == null || stored.incomeTaxFilerStatusLookupId == null) {
                    errors.put("taxReturns", "Income details must be saved before tax returns");
                } else {
                    validateTaxReturns(request.getTaxReturns(),
                            lookupHelper.descriptionMatches(stored.incomeTaxFilerStatusLookupId, "Filer"), errors);
                }
            }
        } else {
            if (isBlank(income.getIncomeTaxFilerStatus())) {
                errors.put("income.incomeTaxFilerStatus", "Income tax filer status is required");
            } else {
                resolved.incomeTaxFilerStatusLookupId = lookupHelper.resolveId("Income Tax Filer Status", props.getIncomeTaxFilerStatus(), income.getIncomeTaxFilerStatus(), "income.incomeTaxFilerStatus", errors);
            }

            if (!isBlank(income.getRiskCategory())) {
                resolved.riskCategoryLookupId = lookupHelper.resolveId("Risk Category", props.getRiskCategory(), income.getRiskCategory(), "income.riskCategory", errors);
            }

            if (isSelfEmployed) {
                // Rule 1: Self Employed income: only annualIncome is required (positive).
                // otherSourceIncome, totalGrossIncome, totalNetIncome and incomeTax are optional; when present they must be non-negative.
                if (income.getAnnualIncome() == null || income.getAnnualIncome().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.put("income.annualIncome", "Annual income is required and must be positive");
                }
                if (income.getOtherSourceIncome() != null && income.getOtherSourceIncome().compareTo(BigDecimal.ZERO) < 0) {
                    errors.put("income.otherSourceIncome", "Other source income must be non-negative");
                }
                if (income.getTotalGrossIncome() != null && income.getTotalGrossIncome().compareTo(BigDecimal.ZERO) < 0) {
                    errors.put("income.totalGrossIncome", "Total gross income must be non-negative");
                }
                if (income.getTotalNetIncome() != null && income.getTotalNetIncome().compareTo(BigDecimal.ZERO) < 0) {
                    errors.put("income.totalNetIncome", "Total net income must be non-negative");
                }
                if (income.getIncomeTax() != null && income.getIncomeTax().compareTo(BigDecimal.ZERO) < 0) {
                    errors.put("income.incomeTax", "Income tax must be non-negative");
                }
            } else {
                if (income.getAnnualIncome() == null || income.getAnnualIncome().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.put("income.annualIncome", "Annual income is required and must be positive");
                }
            }

            // Rule 2: Filer status: taxReturns are optional (see validateTaxReturns).
            boolean isFiler = resolved.incomeTaxFilerStatusLookupId != null &&
                    lookupHelper.descriptionMatches(resolved.incomeTaxFilerStatusLookupId, "Filer");
            validateTaxReturns(request.getTaxReturns(), isFiler, errors);
        }

        // 7. Bank Account
        BankRequest bank = request.getBank();
        if (bank == null) {
            if (!partial) {
                errors.put("bank", "Bank details are required");
            }
        } else {
            if (isBlank(bank.getBankName())) {
                errors.put("bank.bankName", "Bank name is required");
            }
            if (isBlank(bank.getAccountNumber())) {
                errors.put("bank.accountNumber", "Bank account number is required");
            } else {
                String acc = bank.getAccountNumber().trim();
                bank.setAccountNumber(acc);
                if (isUpdate && cryptoService.isMasked(acc)) {
                    // Allowed masked value on update
                } else if (!ACCOUNT_PATTERN.matcher(acc).matches()) {
                    errors.put("bank.accountNumber", "Bank account number must be between 9 and 18 digits");
                }
            }
            if (isBlank(bank.getAccountType())) {
                errors.put("bank.accountType", "Bank account type is required");
            } else {
                resolved.accountTypeLookupId = lookupHelper.resolveId("Bank Account Type", props.getBankAccountType(), bank.getAccountType(), "bank.accountType", errors);
            }
            if (!isBlank(bank.getAccountStatus())) {
                resolved.accountStatusLookupId = lookupHelper.resolveId("Bank Account Status", props.getBankAccountStatus(), bank.getAccountStatus(), "bank.accountStatus", errors);
            }
        }

        // 8. Manual Loan History (optional)
        if (request.getLoanHistory() != null) {
            List<LoanHistoryRequest> loans = request.getLoanHistory();
            for (int i = 0; i < loans.size(); i++) {
                LoanHistoryRequest lh = loans.get(i);
                String prefix = "loanHistory[" + i + "].";
                if (isBlank(lh.getBankName())) {
                    errors.put(prefix + "bankName", "Bank name is required");
                }
                if (isBlank(lh.getAccountNumber())) {
                    errors.put(prefix + "accountNumber", "Account number is required");
                }
                if (isBlank(lh.getLoanType())) {
                    errors.put(prefix + "loanType", "Loan type is required");
                }
                if (lh.getTotalOutstanding() == null || lh.getTotalOutstanding().compareTo(BigDecimal.ZERO) < 0) {
                    errors.put(prefix + "totalOutstanding", "Total outstanding must be non-negative");
                }
                if (lh.getEmi() == null || lh.getEmi().compareTo(BigDecimal.ZERO) < 0) {
                    errors.put(prefix + "emi", "EMI must be non-negative");
                }
                if (lh.getExpiryDate() == null) {
                    errors.put(prefix + "expiryDate", "Expiry date is required");
                }
            }
        }

        // 9. References (exactly 2 required: sr_no 1 and 2)
        List<ReferenceRequest> references = request.getReferences();
        if (references == null && partial) {
            // References step is not part of this save.
        } else if (references == null || references.size() != 2) {
            errors.put("references", "Exactly two references are required");
        } else {
            Set<Integer> seenSrNos = new HashSet<>();
            for (int i = 0; i < references.size(); i++) {
                ReferenceRequest ref = references.get(i);
                String prefix = "references[" + i + "].";

                if (ref.getSrNo() == null || (ref.getSrNo() != 1 && ref.getSrNo() != 2)) {
                    errors.put(prefix + "srNo", "Reference serial number must be 1 or 2");
                } else if (!seenSrNos.add(ref.getSrNo())) {
                    errors.put(prefix + "srNo", "Duplicate reference serial number: " + ref.getSrNo());
                }

                validateName(ref.getReferenceName(), prefix + "referenceName", "Reference name", true, errors);
                if (isBlank(ref.getRelation())) {
                    errors.put(prefix + "relation", "Relation is required");
                }
                if (isBlank(ref.getLocation())) {
                    errors.put(prefix + "location", "Location is required");
                }
                if (isBlank(ref.getContactNo()) || !MOBILE_PATTERN.matcher(ref.getContactNo().trim()).matches()) {
                    errors.put(prefix + "contactNo", "Contact number must be a valid 10-digit number starting with 6-9");
                }
                if (isBlank(ref.getEmail()) || !EMAIL_PATTERN.matcher(ref.getEmail().trim()).matches()) {
                    errors.put(prefix + "email", "Valid email is required");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new CustomerValidationException("Validation failed for customer", errors);
        }

        return resolved;
    }

    private void validatePreviousEmployment(List<PreviousEmploymentRequest> prevList, boolean isSelfEmployed,
                                            ResolvedLookups resolved, Map<String, String> errors) {
        if (prevList == null) {
            return;
        }
        if (isSelfEmployed) {
            if (!prevList.isEmpty()) {
                errors.put("previousEmployment", "Previous employment must not be provided for self-employed customers");
            }
            return;
        }
        CustomerLookupProperties props = lookupHelper.getProperties();
        for (int i = 0; i < prevList.size(); i++) {
            PreviousEmploymentRequest p = prevList.get(i);
            String prefix = "previousEmployment[" + i + "].";
            if (!isBlank(p.getEmploymentType())) {
                Long peTypeId = lookupHelper.resolveId("Employment Type", props.getEmploymentType(), p.getEmploymentType(), prefix + "employmentType", errors);
                resolved.previousEmploymentTypeLookupIds.add(peTypeId);
            } else {
                resolved.previousEmploymentTypeLookupIds.add(null);
            }
            if (isBlank(p.getEmployerName())) {
                errors.put(prefix + "employerName", "Employer name is required");
            }
            if (isBlank(p.getDesignation())) {
                errors.put(prefix + "designation", "Designation is required");
            }
            if (p.getDateOfJoining() == null) {
                errors.put(prefix + "dateOfJoining", "Date of joining is required");
            }
            if (p.getDateOfRelieving() == null) {
                errors.put(prefix + "dateOfRelieving", "Date of relieving is required");
            } else if (p.getDateOfJoining() != null && !p.getDateOfRelieving().isAfter(p.getDateOfJoining())) {
                errors.put(prefix + "dateOfRelieving", "Date of relieving must be after date of joining");
            }
        }
    }

    /**
     * Filer: rows where both grossIncome and tdsPaid are empty are ignored; a row with any value
     * needs both, positive, and a financialYear in YYYY-YY format, unique per customer.
     * Non-Filer: any non-empty row is rejected.
     */
    private void validateTaxReturns(List<TaxReturnRequest> taxList, boolean isFiler, Map<String, String> errors) {
        if (taxList == null) {
            return;
        }
        if (isFiler) {
            Set<String> seenYears = new HashSet<>();
            for (int i = 0; i < taxList.size(); i++) {
                TaxReturnRequest tr = taxList.get(i);
                boolean hasGross = tr.getGrossIncome() != null;
                boolean hasTds = tr.getTdsPaid() != null;
                boolean hasYear = !isBlank(tr.getFinancialYear());

                if (!hasGross && !hasTds) {
                    continue;
                }

                String prefix = "taxReturns[" + i + "].";
                if (!hasYear || !FINANCIAL_YEAR_PATTERN.matcher(tr.getFinancialYear().trim()).matches()) {
                    errors.put(prefix + "financialYear", "Financial year must follow format YYYY-YY (e.g. 2025-26)");
                } else if (!seenYears.add(tr.getFinancialYear().trim())) {
                    errors.put(prefix + "financialYear", "Duplicate financial year: " + tr.getFinancialYear().trim());
                }

                if (tr.getGrossIncome() == null || tr.getGrossIncome().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.put(prefix + "grossIncome", "Gross income is required and must be positive");
                }
                if (tr.getTdsPaid() == null || tr.getTdsPaid().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.put(prefix + "tdsPaid", "TDS paid is required and must be positive");
                }
            }
        } else {
            boolean hasNonEmptyRow = taxList.stream().anyMatch(tr ->
                    tr.getGrossIncome() != null || tr.getTdsPaid() != null || !isBlank(tr.getFinancialYear())
            );
            if (hasNonEmptyRow) {
                errors.put("taxReturns", "Tax returns must be empty for non-filers");
            }
        }
    }

    private void validateName(String value, String fieldKey, String label, boolean required, Map<String, String> errors) {
        if (isBlank(value)) {
            if (required) {
                errors.put(fieldKey, label + " is required");
            }
            return;
        }
        if (!NAME_PATTERN.matcher(value.trim()).matches()) {
            errors.put(fieldKey, label + " must be 3-50 characters, letters plus space . ' - only, starting with a letter");
        }
    }

    private void validateOptionalName(String value, String fieldKey, String label, Map<String, String> errors) {
        if (!OPTIONAL_NAME_PATTERN.matcher(value.trim()).matches()) {
            errors.put(fieldKey, label + " contains invalid characters");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
