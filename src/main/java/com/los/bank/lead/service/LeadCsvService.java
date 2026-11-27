package com.los.bank.lead.service;

import com.los.bank.lead.dto.AddressDetailDto;
import com.los.bank.lead.dto.LeadDto;
import com.los.bank.lead.entity.Lead;
import com.los.bank.lead.exception.LeadValidationException;
import com.los.bank.lead.repository.LeadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Service managing CSV export, template download, and bulk import for the Lead Module.
 */
@Service
public class LeadCsvService {

    private static final Logger logger = LoggerFactory.getLogger(LeadCsvService.class);

    private static final List<String> SAMPLE_HEADERS = Collections.unmodifiableList(Arrays.asList(
            "First Name", "Middle Name", "Last Name", "Name Prefix", "Date of Birth",
            "Gender", "Marital Status", "Customer Type", "Residential Status", "Mobile Number",
            "Email", "PAN Card", "Aadhaar Card", "Passport No", "Passport Expiry Date",
            "Branch", "Loan Product Type", "Loan Amount", "Interest Rate", "Tenure Months",
            "Tenure Unit", "Instalments", "Monthly EMI", "Total Interest", "Loan Purpose",
            "Security Amount", "Collateral", "Employment Type", "Occupation", "Employer Type",
            "Employer Name", "Designation", "Years of Service", "Annual Income", "Take Home Pay",
            "Deductions", "Bank Name", "Primary Bank Account", "EMI Bounce", "EMI Bounce Count", "Address Type", "Flat Building Name",
            "Street Road", "Landmark", "City", "State", "PIN Code",
            "Acquisition Channel", "Referral Date", "Partner ID", "Partner Name", "Assigned Employee ID"
    ));

    private static final List<String> EXPORT_HEADERS = Collections.unmodifiableList(Arrays.asList(
            "Lead ID", "Status", "First Name", "Middle Name", "Last Name",
            "Customer Name", "Name Prefix", "Date of Birth", "Age", "Gender",
            "Marital Status", "Customer Type", "Residential Status", "Mobile Number", "Email",
            "PAN Card", "Aadhaar Card", "Passport No", "Passport Expiry Date", "Branch",
            "Loan Product Type", "Loan Amount", "Interest Rate", "Tenure Months", "Tenure Unit",
            "Instalments", "Monthly EMI", "Total Interest", "Loan Purpose", "Security Amount",
            "Collateral", "Employment Type", "Occupation", "Employer Type", "Employer Name",
            "Designation", "Years of Service", "Annual Income", "Take Home Pay", "Deductions",
            "Bank Name", "Primary Bank Account", "EMI Bounce", "EMI Bounce Count", "Address Type", "Flat Building Name", "Street Road",
            "Landmark", "City", "State", "PIN Code", "Acquisition Channel",
            "Referral Date", "Partner ID", "Partner Name", "Assigned Employee ID", "Assigned Employee Name",
            "Created At", "Updated At"
    ));

    private static final List<String> MANDATORY_HEADER_KEYS = Collections.unmodifiableList(Arrays.asList(
            "firstname", "lastname", "dateofbirth", "mobilenumber", "email",
            "pancard", "aadhaarcard", "branch", "gender", "maritalstatus",
            "loanproducttype", "loanamount", "interestrate", "tenuremonths",
            "employmenttype", "annualincome", "city", "state", "pincode", "acquisitionchannel"
    ));

    private final LeadService leadService;
    private final LeadRepository leadRepository;

    public LeadCsvService(LeadService leadService, LeadRepository leadRepository) {
        this.leadService = leadService;
        this.leadRepository = leadRepository;
    }

    /**
     * Generates a downloadable sample CSV containing template headers and valid sample rows.
     */
    public byte[] generateSampleCsv() {
        StringBuilder sb = new StringBuilder();
        appendCsvRow(sb, SAMPLE_HEADERS);

        // Sample Row 1: Salaried Home Loan Applicant
        List<String> sampleRow1 = Arrays.asList(
                "Ramesh", "K", "Sharma", "Mr", "1988-06-20",
                "Male", "Married", "Individual", "Resident", "9812345678",
                "ramesh.sharma@example.com", "ABCPS1234K", "123456789012", "L1234567", "2030-12-31",
                "SBI-BR-001", "Home Loan", "5000000", "8.5", "120",
                "month", "120", "62000", "2440000", "Purchase of Residential Apartment",
                "500000", "Residential Flat", "Salaried", "Software Engineer", "Private Limited",
                "Tech Solutions Pvt Ltd", "Senior Consultant", "6", "1200000", "85000",
                "15000", "State Bank of India", "1234567890", "No", "0", "Permanent", "Flat 402, Green Valley Apts",
                "MG Road", "Near Metro Station", "Mumbai", "Maharashtra", "400001",
                "Direct", "2026-11-20", "PART-001", "Prime Finvest", "1"
        );
        appendCsvRow(sb, sampleRow1);

        // Sample Row 2: Self-Employed Personal Loan Applicant
        List<String> sampleRow2 = Arrays.asList(
                "Priya", "S", "Patel", "Ms", "1992-11-14",
                "Female", "Single", "Individual", "Resident", "9876543210",
                "priya.patel@example.com", "BCDPE5678L", "987654321098", "", "",
                "SBI-BR-001", "Personal Loan", "800000", "10.5", "36",
                "month", "36", "26000", "136000", "Home Renovation",
                "", "", "Self-Employed", "Architect", "Proprietorship",
                "Patel Design Studio", "Principal Architect", "8", "950000", "70000",
                "10000", "HDFC Bank", "987654321012", "Yes", "2", "Current", "Bungalow 12, Sunrise Enclave",
                "FC Road", "Opposite City Mall", "Pune", "Maharashtra", "411001",
                "Referral", "2026-11-21", "PART-002", "Apex Advisors", ""
        );
        appendCsvRow(sb, sampleRow2);

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Exports existing leads matching optional status and keyword filter to CSV format.
     */
    @Transactional(transactionManager = "leadTransactionManager", readOnly = true)
    public byte[] exportLeads(String status, String search) {
        Page<Lead> leadPage = leadRepository.searchLeads(
                status != null && !status.trim().isEmpty() ? status.trim() : null,
                search != null && !search.trim().isEmpty() ? search.trim() : null,
                PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<Lead> leads = leadPage.getContent();

        StringBuilder sb = new StringBuilder();
        appendCsvRow(sb, EXPORT_HEADERS);

        for (Lead lead : leads) {
            List<String> row = new ArrayList<>();
            row.add(lead.getLeadId() != null ? lead.getLeadId() : "");
            row.add(lead.getStatus() != null ? lead.getStatus() : "");
            row.add(lead.getFirstName() != null ? lead.getFirstName() : "");
            row.add(lead.getMiddleName() != null ? lead.getMiddleName() : "");
            row.add(lead.getLastName() != null ? lead.getLastName() : "");
            row.add(lead.getCustomerName() != null ? lead.getCustomerName() : "");
            row.add(lead.getNamePrefix() != null ? lead.getNamePrefix() : "");
            row.add(lead.getDateOfBirth() != null ? lead.getDateOfBirth().toString() : "");
            row.add(lead.getAge() != null ? String.valueOf(lead.getAge()) : "");
            row.add(lead.getGender() != null ? lead.getGender() : "");
            row.add(lead.getMaritalStatus() != null ? lead.getMaritalStatus() : "");
            row.add(lead.getCustomerType() != null ? lead.getCustomerType() : "");
            row.add(lead.getResidentialStatus() != null ? lead.getResidentialStatus() : "");
            row.add(lead.getMobileNumber() != null ? lead.getMobileNumber() : "");
            row.add(lead.getEmail() != null ? lead.getEmail() : "");
            row.add(lead.getPanNumber() != null ? new String(lead.getPanNumber(), StandardCharsets.UTF_8) : "");
            row.add(lead.getAadhaarNumber() != null ? new String(lead.getAadhaarNumber(), StandardCharsets.UTF_8) : "");
            row.add(lead.getPassportNumber() != null ? new String(lead.getPassportNumber(), StandardCharsets.UTF_8) : "");
            row.add(lead.getPassportExpiryDate() != null ? lead.getPassportExpiryDate().toString() : "");
            row.add(lead.getBranch() != null ? lead.getBranch() : "");
            row.add(lead.getLoanProductType() != null ? lead.getLoanProductType() : "");
            row.add(lead.getLoanAmount() != null ? lead.getLoanAmount().stripTrailingZeros().toPlainString() : "");
            row.add(lead.getInterestRate() != null ? lead.getInterestRate().stripTrailingZeros().toPlainString() : "");
            row.add(lead.getTenure() != null ? String.valueOf(lead.getTenure()) : "");
            row.add(lead.getTenureUnit() != null ? lead.getTenureUnit() : "");
            row.add(lead.getInstalments() != null ? String.valueOf(lead.getInstalments()) : "");
            row.add(lead.getMonthlyEmi() != null ? lead.getMonthlyEmi().stripTrailingZeros().toPlainString() : "");
            row.add(lead.getTotalInterest() != null ? lead.getTotalInterest().stripTrailingZeros().toPlainString() : "");
            row.add(lead.getLoanPurpose() != null ? lead.getLoanPurpose() : "");
            row.add(lead.getSecurityAmount() != null ? lead.getSecurityAmount().stripTrailingZeros().toPlainString() : "");
            row.add(lead.getCollateral() != null ? lead.getCollateral() : "");
            row.add(lead.getEmploymentType() != null ? lead.getEmploymentType() : "");
            row.add(lead.getOccupation() != null ? lead.getOccupation() : "");
            row.add(lead.getEmployerType() != null ? lead.getEmployerType() : "");
            row.add(lead.getEmployerName() != null ? lead.getEmployerName() : "");
            row.add(lead.getDesignation() != null ? lead.getDesignation() : "");
            row.add(lead.getYearsOfService() != null ? lead.getYearsOfService() : "");
            row.add(lead.getAnnualIncome() != null ? lead.getAnnualIncome().stripTrailingZeros().toPlainString() : "");
            row.add(lead.getTakeHomePay() != null ? lead.getTakeHomePay().stripTrailingZeros().toPlainString() : "");
            row.add(lead.getDeductions() != null ? lead.getDeductions().stripTrailingZeros().toPlainString() : "");
            row.add(lead.getBankName() != null ? lead.getBankName() : "");
            row.add(lead.getPrimaryBankAccount() != null ? new String(lead.getPrimaryBankAccount(), StandardCharsets.UTF_8) : "");
            row.add(lead.getEmiBounce() != null ? lead.getEmiBounce() : "No");
            row.add(lead.getEmiBounceCount() != null ? String.valueOf(lead.getEmiBounceCount()) : "0");
            row.add(lead.getAddressType() != null ? lead.getAddressType() : "");
            row.add(lead.getFlatBuildingName() != null ? lead.getFlatBuildingName() : "");
            row.add(lead.getStreetRoad() != null ? lead.getStreetRoad() : "");
            row.add(lead.getLandmark() != null ? lead.getLandmark() : "");
            row.add(lead.getCity() != null ? lead.getCity() : "");
            row.add(lead.getState() != null ? lead.getState() : "");
            row.add(lead.getAddressPinCode() != null ? lead.getAddressPinCode() : "");
            row.add(lead.getAcquisitionChannel() != null ? lead.getAcquisitionChannel() : "");
            row.add(lead.getReferralDate() != null ? lead.getReferralDate().toString() : "");
            row.add(lead.getPartnerId() != null ? lead.getPartnerId() : "");
            row.add(lead.getPartnerName() != null ? lead.getPartnerName() : "");
            row.add(lead.getEmployeeId() != null ? lead.getEmployeeId() : "");
            row.add(lead.getEmployeeName() != null ? lead.getEmployeeName() : "");
            row.add(lead.getCreatedAt() != null ? lead.getCreatedAt().toString() : "");
            row.add(lead.getUpdatedAt() != null ? lead.getUpdatedAt().toString() : "");

            appendCsvRow(sb, row);
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Parses CSV bytes, validates records, creates leads using existing LeadService,
     * and returns structured import results.
     */
    public Map<String, Object> importLeads(byte[] csvBytes) {
        Map<String, Object> response = new LinkedHashMap<>();

        if (csvBytes == null || csvBytes.length == 0) {
            response.put("success", false);
            response.put("message", "CSV file or content is empty. Please provide a valid CSV file.");
            response.put("totalRows", 0);
            response.put("successfulImports", 0);
            response.put("failedImports", 0);
            response.put("successfulLeads", Collections.emptyList());
            response.put("errors", Collections.emptyList());
            return response;
        }

        List<List<String>> records;
        try (Reader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8))) {
            records = parseCsv(reader);
        } catch (Exception ex) {
            logger.error("Failed to parse CSV stream: ", ex);
            response.put("success", false);
            response.put("message", "Malformed CSV format: " + ex.getMessage());
            response.put("totalRows", 0);
            response.put("successfulImports", 0);
            response.put("failedImports", 0);
            response.put("successfulLeads", Collections.emptyList());
            response.put("errors", Collections.emptyList());
            return response;
        }

        if (records.isEmpty()) {
            response.put("success", false);
            response.put("message", "CSV file contains no header or data rows.");
            response.put("totalRows", 0);
            response.put("successfulImports", 0);
            response.put("failedImports", 0);
            response.put("successfulLeads", Collections.emptyList());
            response.put("errors", Collections.emptyList());
            return response;
        }

        // 1. Process header row
        List<String> headerRow = records.get(0);
        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            String col = headerRow.get(i);
            if (col != null) {
                String normalized = normalizeKey(col);
                if (!normalized.isEmpty()) {
                    headerMap.put(normalized, i);
                }
            }
        }

        // Validate mandatory header presence
        List<String> missingHeaders = new ArrayList<>();
        for (String mandatory : MANDATORY_HEADER_KEYS) {
            if (!headerMap.containsKey(mandatory)) {
                missingHeaders.add(mandatory);
            }
        }
        if (!missingHeaders.isEmpty()) {
            response.put("success", false);
            response.put("message", "CSV is missing mandatory header(s): " + String.join(", ", missingHeaders));
            response.put("totalRows", 0);
            response.put("successfulImports", 0);
            response.put("failedImports", 0);
            response.put("successfulLeads", Collections.emptyList());
            response.put("errors", Collections.emptyList());
            return response;
        }

        // 2. Process data rows
        List<Map<String, Object>> successfulLeads = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        int dataRowCount = records.size() - 1;

        for (int r = 1; r < records.size(); r++) {
            int displayRow = r + 1; // 1-based index including header
            List<String> row = records.get(r);

            // Skip empty rows
            if (isRowEmpty(row)) {
                continue;
            }

            LeadDto leadDto = null;
            try {
                leadDto = mapCsvRowToLeadDto(row, headerMap);
                LeadDto created = leadService.createLead(leadDto);

                Map<String, Object> successItem = new LinkedHashMap<>();
                successItem.put("row", displayRow);
                successItem.put("leadId", created.getLeadId());
                successItem.put("customerName", created.getEffectiveCustomerName());
                successItem.put("mobileNumber", created.getMobileNumber());
                successfulLeads.add(successItem);
            } catch (LeadValidationException ex) {
                Map<String, Object> errorItem = new LinkedHashMap<>();
                errorItem.put("row", displayRow);
                errorItem.put("identifier", getRowIdentifier(leadDto, row, headerMap, displayRow));
                errorItem.put("message", ex.getMessage());
                errors.add(errorItem);
            } catch (Exception ex) {
                logger.warn("Unexpected error processing row {}: {}", displayRow, ex.getMessage());
                Map<String, Object> errorItem = new LinkedHashMap<>();
                errorItem.put("row", displayRow);
                errorItem.put("identifier", getRowIdentifier(leadDto, row, headerMap, displayRow));
                errorItem.put("message", ex.getMessage() != null ? ex.getMessage() : "Unknown processing error");
                errors.add(errorItem);
            }
        }

        int successCount = successfulLeads.size();
        int failureCount = errors.size();
        int totalProcessed = successCount + failureCount;

        boolean overallSuccess = failureCount == 0 && successCount > 0;
        String summary = String.format("Lead import completed: %d successfully imported, %d failed out of %d total data records.",
                successCount, failureCount, totalProcessed);

        response.put("success", overallSuccess);
        response.put("message", summary);
        response.put("totalRows", totalProcessed);
        response.put("successfulImports", successCount);
        response.put("failedImports", failureCount);
        response.put("successfulLeads", successfulLeads);
        response.put("errors", errors);

        return response;
    }

    // =========================================================================
    // CSV Parsing & Serializing Engine
    // =========================================================================

    public static List<List<String>> parseCsv(Reader in) throws Exception {
        BufferedReader reader = in instanceof BufferedReader ? (BufferedReader) in : new BufferedReader(in);
        List<List<String>> records = new ArrayList<>();
        List<String> currentRecord = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int ch;

        while ((ch = reader.read()) != -1) {
            if (ch == '\uFEFF') {
                continue; // Skip UTF-8 BOM
            }

            if (ch == '"') {
                if (inQuotes) {
                    reader.mark(1);
                    int next = reader.read();
                    if (next == '"') {
                        field.append('"');
                    } else {
                        inQuotes = false;
                        if (next != -1) {
                            reader.reset();
                        }
                    }
                } else {
                    inQuotes = true;
                }
            } else if (ch == ',' && !inQuotes) {
                currentRecord.add(field.toString().trim());
                field.setLength(0);
            } else if ((ch == '\r' || ch == '\n') && !inQuotes) {
                if (ch == '\r') {
                    reader.mark(1);
                    int next = reader.read();
                    if (next != '\n' && next != -1) {
                        reader.reset();
                    }
                }
                currentRecord.add(field.toString().trim());
                field.setLength(0);

                if (!isRowEmpty(currentRecord)) {
                    records.add(currentRecord);
                }
                currentRecord = new ArrayList<>();
            } else {
                field.append((char) ch);
            }
        }

        if (field.length() > 0 || !currentRecord.isEmpty()) {
            currentRecord.add(field.toString().trim());
            if (!isRowEmpty(currentRecord)) {
                records.add(currentRecord);
            }
        }

        return records;
    }

    private static void appendCsvRow(StringBuilder sb, List<String> fields) {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escapeCsv(fields.get(i)));
        }
        sb.append("\r\n");
    }

    public static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (needsQuotes) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static boolean isRowEmpty(List<String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String normalizeKey(String key) {
        if (key == null) return "";
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String getVal(List<String> row, Map<String, Integer> headerMap, String... keys) {
        for (String key : keys) {
            String norm = normalizeKey(key);
            Integer idx = headerMap.get(norm);
            if (idx != null && idx < row.size()) {
                String v = row.get(idx);
                if (v != null && !v.trim().isEmpty()) {
                    return v.trim();
                }
            }
        }
        return null;
    }

    private LeadDto mapCsvRowToLeadDto(List<String> row, Map<String, Integer> headerMap) {
        String firstName = getVal(row, headerMap, "firstname", "first_name", "first name");
        String middleName = getVal(row, headerMap, "middlename", "middle_name", "middle name");
        String lastName = getVal(row, headerMap, "lastname", "last_name", "last name");
        String namePrefix = getVal(row, headerMap, "nameprefix", "name_prefix", "prefix", "title");
        String dob = normalizeDate(getVal(row, headerMap, "dateofbirth", "date_of_birth", "dob", "birthdate"));

        String gender = getVal(row, headerMap, "gender", "sex");
        String maritalStatus = getVal(row, headerMap, "maritalstatus", "marital_status");
        String customerType = getVal(row, headerMap, "customertype", "customer_type");
        String residentialStatus = getVal(row, headerMap, "residentialstatus", "residential_status");

        String mobile = getVal(row, headerMap, "mobilenumber", "mobile_number", "mobile", "phone");
        String email = getVal(row, headerMap, "email", "emailaddress", "email_address");
        String pan = getVal(row, headerMap, "pancard", "pan_card", "pan", "pannumber", "pan_number");
        if (pan != null) pan = pan.toUpperCase(Locale.ROOT);
        String aadhaar = getVal(row, headerMap, "aadhaarcard", "aadhaar_card", "aadhaar", "aadhaarnumber", "aadhaar_number", "aadhar");

        String passportNo = getVal(row, headerMap, "passportno", "passport_no", "passportnumber", "passport_number");
        String passportExpiryDate = normalizeDate(getVal(row, headerMap, "passportexpirydate", "passport_expiry_date"));
        String branch = getVal(row, headerMap, "branch", "branchcode", "branch_code", "branchid", "branch_id");

        String loanProductType = getVal(row, headerMap, "loanproducttype", "loan_product_type", "producttype", "product_type", "loantype", "loan_type");
        String loanAmount = cleanNumeric(getVal(row, headerMap, "loanamount", "loan_amount", "amount"));
        String interestRate = cleanNumeric(getVal(row, headerMap, "interestrate", "interest_rate", "rate"));
        String tenure = cleanNumeric(getVal(row, headerMap, "tenuremonths", "tenure_months", "tenure"));
        String tenureUnit = getVal(row, headerMap, "tenureunit", "tenure_unit");
        String instalments = cleanNumeric(getVal(row, headerMap, "instalments", "installments", "numberofinstalments", "number_of_instalments"));
        String monthlyEmi = cleanNumeric(getVal(row, headerMap, "monthlyemi", "monthly_emi", "emi"));
        String totalInterest = cleanNumeric(getVal(row, headerMap, "totalinterest", "total_interest"));
        String loanPurpose = getVal(row, headerMap, "loanpurpose", "loan_purpose", "purpose");
        String otherLoanPurpose = getVal(row, headerMap, "otherloanpurpose", "other_loan_purpose");
        String securityAmount = cleanNumeric(getVal(row, headerMap, "securityamount", "security_amount"));
        String collateral = getVal(row, headerMap, "collateral");

        String employmentType = getVal(row, headerMap, "employmenttype", "employment_type");
        String occupation = getVal(row, headerMap, "occupation");
        String employerType = getVal(row, headerMap, "employertype", "employer_type");
        String employerName = getVal(row, headerMap, "employername", "employer_name", "companyname", "company_name");
        String designation = getVal(row, headerMap, "designation");
        String yearsOfService = getVal(row, headerMap, "yearsofservice", "years_of_service");
        String annualIncome = cleanNumeric(getVal(row, headerMap, "annualincome", "annual_income", "income"));
        String takeHomePay = cleanNumeric(getVal(row, headerMap, "takehomepay", "take_home_pay"));
        String deductions = cleanNumeric(getVal(row, headerMap, "deductions", "deduction"));
        String bankName = getVal(row, headerMap, "bankname", "bank_name");
        String primaryBankAccount = getVal(row, headerMap, "primarybankaccount", "primary_bank_account", "accountnumber", "account_number");

        // EMI Bounce Fields
        String emiBounce = getVal(row, headerMap, "emibounce", "emi_bounce", "emi bounce", "is_emi_bounce", "isemibounce");
        String emiBounceCountStr = cleanNumeric(getVal(row, headerMap, "emibouncecount", "emi_bounce_count", "emi bounce count", "bouncecount", "bounce_count"));
        Integer emiBounceCount = null;
        if (emiBounceCountStr != null) {
            try {
                emiBounceCount = Integer.parseInt(emiBounceCountStr);
            } catch (Exception ignored) {}
        }

        // Address Fields
        String addressType = getVal(row, headerMap, "addresstype", "address_type");
        String flatBuildingName = getVal(row, headerMap, "flatbuildingname", "flat_building_name", "addressline1", "address_line_1", "address");
        String streetRoad = getVal(row, headerMap, "streetroad", "street_road", "addressline2", "address_line_2");
        String landmark = getVal(row, headerMap, "landmark");
        String city = getVal(row, headerMap, "city");
        String state = getVal(row, headerMap, "state");
        String pinCode = getVal(row, headerMap, "pincode", "pin_code", "zip", "zipcode", "postalcode");

        // Sourcing / Channel Fields
        String acquisitionChannel = getVal(row, headerMap, "acquisitionchannel", "acquisition_channel", "channel", "leadsource", "lead_source");
        String referralDate = normalizeDate(getVal(row, headerMap, "referraldate", "referral_date", "date"));
        String partnerId = getVal(row, headerMap, "partnerid", "partner_id");
        String partnerName = getVal(row, headerMap, "partnername", "partner_name");
        String employeeId = getVal(row, headerMap, "assignedemployeeid", "employeeid", "employee_id", "assigneduserid", "assigned_user_id");
        String employeeName = getVal(row, headerMap, "assignedemployeename", "employeename", "employee_name");
        String status = getVal(row, headerMap, "status", "leadstatus", "lead_status");

        // Calculate age if not present but DOB is valid
        Integer age = null;
        String ageStr = cleanNumeric(getVal(row, headerMap, "age"));
        if (ageStr != null) {
            try {
                age = Integer.parseInt(ageStr);
            } catch (Exception ignored) {}
        }
        if (age == null && dob != null) {
            try {
                LocalDate birthDate = LocalDate.parse(dob);
                age = Period.between(birthDate, LocalDate.now()).getYears();
            } catch (Exception ignored) {}
        }

        AddressDetailDto addressDetail = AddressDetailDto.builder()
                .id(1L)
                .addressType(addressType != null ? addressType : "Current")
                .flatBuildingName(flatBuildingName)
                .streetRoad(streetRoad)
                .landmark(landmark)
                .city(city)
                .state(state)
                .pinCode(pinCode)
                .build();

        return LeadDto.builder()
                .namePrefix(namePrefix)
                .firstName(firstName)
                .middleName(middleName)
                .lastName(lastName)
                .dateOfBirth(dob)
                .age(age)
                .gender(gender)
                .maritalStatus(maritalStatus)
                .customerType(customerType != null ? customerType : "Individual")
                .residentialStatus(residentialStatus != null ? residentialStatus : "Resident")
                .mobileNumber(mobile)
                .email(email)
                .panCard(pan)
                .aadhaarCard(aadhaar)
                .passportNo(passportNo)
                .passportExpiryDate(passportExpiryDate)
                .branch(branch)
                .loanProductType(loanProductType)
                .loanAmount(loanAmount)
                .interestRate(interestRate)
                .tenure(tenure)
                .tenureUnit(tenureUnit != null ? tenureUnit : "month")
                .instalments(instalments)
                .monthlyEmi(monthlyEmi)
                .totalInterest(totalInterest)
                .loanPurpose(loanPurpose)
                .otherLoanPurpose(otherLoanPurpose)
                .securityAmount(securityAmount)
                .collateral(collateral)
                .employmentType(employmentType)
                .occupation(occupation)
                .employerType(employerType)
                .employerName(employerName)
                .designation(designation)
                .yearsOfService(yearsOfService)
                .annualIncome(annualIncome)
                .takeHomePay(takeHomePay)
                .deductions(deductions)
                .bankName(bankName)
                .primaryBankAccount(primaryBankAccount)
                .emiBounce(emiBounce)
                .emiBounceCount(emiBounceCount)
                .addressDetails(Collections.singletonList(addressDetail))
                .acquisitionChannel(acquisitionChannel)
                .referralDate(referralDate)
                .partnerId(partnerId)
                .partnerName(partnerName)
                .employeeId(employeeId)
                .employeeName(employeeName)
                .status(status != null ? status : "Pending")
                .build();
    }

    private String cleanNumeric(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        return val.replace(",", "").trim();
    }

    private String normalizeDate(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        String trimmed = val.trim();
        // Standard ISO format YYYY-MM-DD
        if (trimmed.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return trimmed;
        }
        // DD/MM/YYYY or DD-MM-YYYY
        if (trimmed.matches("^\\d{2}[/-]\\d{2}[/-]\\d{4}$")) {
            String[] parts = trimmed.split("[/-]");
            return parts[2] + "-" + parts[1] + "-" + parts[0];
        }
        return trimmed;
    }

    private String getRowIdentifier(LeadDto dto, List<String> row, Map<String, Integer> headerMap, int rowNum) {
        if (dto != null) {
            String name = dto.getEffectiveCustomerName();
            String mobile = dto.getMobileNumber();
            if (!name.isEmpty() && mobile != null) {
                return name + " (" + mobile + ")";
            } else if (!name.isEmpty()) {
                return name;
            } else if (mobile != null) {
                return "Mobile: " + mobile;
            }
        }
        String fn = getVal(row, headerMap, "firstname", "first_name");
        String ln = getVal(row, headerMap, "lastname", "last_name");
        String mob = getVal(row, headerMap, "mobilenumber", "mobile_number", "mobile");
        if (fn != null || ln != null) {
            return (fn != null ? fn : "") + " " + (ln != null ? ln : "") + (mob != null ? " (" + mob + ")" : "");
        }
        return "Row " + rowNum;
    }
}
