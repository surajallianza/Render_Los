package com.los.bank.loan.service;

import com.los.bank.branch.entity.Branch;
import com.los.bank.branch.repository.BranchRepository;
import com.los.bank.customer.entity.Customer;
import com.los.bank.customer.repository.CustomerRepository;
import com.los.bank.loan.dto.RelatedPartyDto;
import com.los.bank.loan.dto.RepaymentScheduleRowDto;
import com.los.bank.loan.dto.RetailLoanFormDto;
import com.los.bank.loan.entity.RetailLoanApplication;
import com.los.bank.loan.exception.RetailLoanNotFoundException;
import com.los.bank.loan.exception.RetailLoanValidationException;
import com.los.bank.loan.repository.RetailLoanApplicationRepository;
import com.los.bank.product.entity.LoanProduct;
import com.los.bank.product.repository.LoanProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service implementation for Retail Loan Application Module.
 * Persists core relational records in a001-a005 via bankTransactionManager and
 * enforces mandatory EMI vs Bullet Payment rules centrally.
 */
@Service
public class RetailLoanApplicationServiceImpl implements RetailLoanApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(RetailLoanApplicationServiceImpl.class);
    private static final DateTimeFormatter MONTH_YEAR_FMT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final RetailLoanApplicationRepository applicationRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final LoanProductRepository loanProductRepository;
    private final RetailLoanApplicationValidator validator;

    /**
     * In-memory cache of extended form attributes (related parties, security, insurance,
     * disbursement, moratorium, and repayment chart) for fields without columns in a001-a005.
     * Ensures full round-trip fidelity during the session while keeping a001-a005 schema untouched.
     */
    private final Map<Long, RetailLoanFormDto> extendedFormStore = new ConcurrentHashMap<>();

    public RetailLoanApplicationServiceImpl(
            RetailLoanApplicationRepository applicationRepository,
            CustomerRepository customerRepository,
            BranchRepository branchRepository,
            LoanProductRepository loanProductRepository,
            RetailLoanApplicationValidator validator) {
        this.applicationRepository = applicationRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.loanProductRepository = loanProductRepository;
        this.validator = validator;
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager")
    public RetailLoanFormDto createApplication(RetailLoanFormDto form) {
        validator.validateForCreateOrUpdate(form, false);

        String appNumber = form.getLoanAccountNumber().trim();
        if (applicationRepository.existsByApplicationNumber(appNumber)) {
            throw new RetailLoanValidationException("Loan application / account number already exists: " + appNumber);
        }

        Customer customer = resolveCustomer(form.getCustomerId());
        Branch branch = resolveBranch(form.getBranch());
        LoanProduct product = resolveProduct(form.getProductCode());

        // Mandatory EMI vs Bullet Payment business rule
        boolean isEmi = isEmiApplicable(form.getEmiApplicable());
        String authoritativeBulletPayment = isEmi ? "No" : "Yes";
        form.setEmiApplicable(isEmi ? "Yes" : "No");
        form.setBulletPayment(authoritativeBulletPayment);

        List<RepaymentScheduleRowDto> schedule = isEmi ? generateRepaymentChart(form) : null;
        form.setRepaymentSchedule(schedule);

        BigDecimal loanAmount = parseBigDecimal(form.getLoanAmount());
        BigDecimal interestRate = parseBigDecimal(
                !isBlank(form.getTotalInterestRate()) ? form.getTotalInterestRate() : form.getInterestRate());
        Integer tenureMonths = parseInteger(form.getTenureMonths());
        BigDecimal instalmentAmount = isEmi ? resolveEmiAmount(form, schedule) : null;
        BigDecimal collateralValue = resolveCollateralValue(form);

        RetailLoanApplication entity = RetailLoanApplication.builder()
                .applicationNumber(appNumber)
                .customerId(customer.getId())
                .branchId(branch.getId())
                .productCode(product.getCode())
                .status(form.getAccountStatus().trim())
                // a002
                .collateralValue(collateralValue)
                .emi(instalmentAmount)
                // a003
                .appliedAmount(loanAmount)
                .interestRate(interestRate)
                .tenureMonths(tenureMonths)
                .sanctionedAmount(loanAmount)
                .eligibilityStatus(isEmi ? "EMI" : "BULLET")
                // a004
                .checkerRemarks(form.getRemarks())
                .approvedAt(parseSanctionedTimestamp(form.getSanctionedDate()))
                .build();

        RetailLoanApplication saved = applicationRepository.save(entity);
        saved.setCustomerCode(customer.getCustomerCode());
        saved.setBranchCode(branch.getCode());
        saved.setProductName(product.getName());

        RetailLoanFormDto storedCopy = copyFormWithPersistedMetadata(form, saved, customer, branch, product);
        extendedFormStore.put(saved.getId(), storedCopy);

        logger.info("Created Retail Loan Application ID={}, number='{}', emiApplicable='{}', bulletPayment='{}'",
                saved.getId(), saved.getApplicationNumber(), storedCopy.getEmiApplicable(), storedCopy.getBulletPayment());

        return storedCopy;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "bankTransactionManager")
    public Map<String, Object> getAllApplications(int page, int size, String status, String search) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? Math.min(size, 100) : 10;
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));

        Page<RetailLoanApplication> resultPage = applicationRepository.searchApplications(
                !isBlank(status) ? status.trim() : null,
                !isBlank(search) ? search.trim() : null,
                pageable);

        List<RetailLoanFormDto> content = resultPage.getContent().stream()
                .map(this::buildResponseFromEntity)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", content);
        response.put("count", content.size());
        response.put("page", resultPage.getNumber());
        response.put("size", resultPage.getSize());
        response.put("totalElements", resultPage.getTotalElements());
        response.put("totalPages", resultPage.getTotalPages());
        response.put("first", resultPage.isFirst());
        response.put("last", resultPage.isLast());
        response.put("hasNext", resultPage.hasNext());
        response.put("hasPrevious", resultPage.hasPrevious());
        return response;
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "bankTransactionManager")
    public RetailLoanFormDto getApplicationById(Long id) {
        if (id == null || id <= 0) {
            throw new RetailLoanValidationException("Valid retail loan application ID is required.");
        }
        RetailLoanApplication entity = applicationRepository.findById(id)
                .orElseThrow(() -> new RetailLoanNotFoundException("Retail loan application not found with ID: " + id));
        return buildResponseFromEntity(entity);
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager")
    public RetailLoanFormDto updateApplication(Long id, RetailLoanFormDto form) {
        if (id == null || id <= 0) {
            throw new RetailLoanValidationException("Valid retail loan application ID is required.");
        }
        RetailLoanApplication existing = applicationRepository.findById(id)
                .orElseThrow(() -> new RetailLoanNotFoundException("Retail loan application not found with ID: " + id));

        validator.validateForCreateOrUpdate(form, true);

        if (!isBlank(form.getLoanAccountNumber())) {
            String appNumber = form.getLoanAccountNumber().trim();
            if (applicationRepository.existsByApplicationNumberAndIdNot(appNumber, id)) {
                throw new RetailLoanValidationException("Loan application / account number already exists: " + appNumber);
            }
            existing.setApplicationNumber(appNumber);
        }

        Customer customer = resolveCustomer(form.getCustomerId());
        Branch branch = resolveBranch(form.getBranch());
        LoanProduct product = resolveProduct(form.getProductCode());

        existing.setCustomerId(customer.getId());
        existing.setBranchId(branch.getId());
        existing.setProductCode(product.getCode());
        existing.setStatus(form.getAccountStatus().trim());

        // Mandatory EMI vs Bullet Payment transition rule
        boolean isEmi = isEmiApplicable(form.getEmiApplicable());
        form.setEmiApplicable(isEmi ? "Yes" : "No");
        form.setBulletPayment(isEmi ? "No" : "Yes");

        List<RepaymentScheduleRowDto> schedule = isEmi ? generateRepaymentChart(form) : null;
        form.setRepaymentSchedule(schedule);

        BigDecimal loanAmount = parseBigDecimal(form.getLoanAmount());
        BigDecimal interestRate = parseBigDecimal(
                !isBlank(form.getTotalInterestRate()) ? form.getTotalInterestRate() : form.getInterestRate());
        Integer tenureMonths = parseInteger(form.getTenureMonths());

        existing.setCollateralValue(resolveCollateralValue(form));
        existing.setEmi(isEmi ? resolveEmiAmount(form, schedule) : null);
        existing.setAppliedAmount(loanAmount);
        existing.setInterestRate(interestRate);
        existing.setTenureMonths(tenureMonths);
        existing.setSanctionedAmount(loanAmount);
        existing.setEligibilityStatus(isEmi ? "EMI" : "BULLET");
        if (form.getRemarks() != null) {
            existing.setCheckerRemarks(form.getRemarks());
        }
        if (!isBlank(form.getSanctionedDate())) {
            existing.setApprovedAt(parseSanctionedTimestamp(form.getSanctionedDate()));
        }

        RetailLoanApplication updated = applicationRepository.save(existing);

        // Preserve omitted child relatedParties if update omits them
        RetailLoanFormDto prevExtended = extendedFormStore.get(id);
        if ((form.getRelatedParties() == null || form.getRelatedParties().isEmpty())
                && prevExtended != null && prevExtended.getRelatedParties() != null) {
            form.setRelatedParties(new ArrayList<>(prevExtended.getRelatedParties()));
        }

        RetailLoanFormDto storedCopy = copyFormWithPersistedMetadata(form, updated, customer, branch, product);
        extendedFormStore.put(updated.getId(), storedCopy);

        logger.info("Updated Retail Loan Application ID={}, emiApplicable='{}', bulletPayment='{}'",
                updated.getId(), storedCopy.getEmiApplicable(), storedCopy.getBulletPayment());

        return storedCopy;
    }

    // =========================================================================
    // EMI & Repayment Chart Generation (Reducing Balance)
    // =========================================================================

    private List<RepaymentScheduleRowDto> generateRepaymentChart(RetailLoanFormDto form) {
        BigDecimal principal = parseBigDecimal(form.getLoanAmount());
        Integer tenureObj = parseInteger(form.getTenureMonths());
        BigDecimal annualRate = parseBigDecimal(
                !isBlank(form.getTotalInterestRate()) ? form.getTotalInterestRate() : form.getInterestRate());

        if (principal == null || tenureObj == null || tenureObj <= 0) {
            return new ArrayList<>();
        }
        int tenure = tenureObj;
        if (annualRate == null) {
            annualRate = BigDecimal.ZERO;
        }

        LocalDate startDate = parseDateOrDefault(form.getInstalmentStartDate(), LocalDate.now().plusMonths(1));
        MathContext mc = new MathContext(12, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), mc);

        BigDecimal emi;
        if (monthlyRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate, mc);
            BigDecimal pow = onePlusR.pow(tenure, mc);
            BigDecimal numerator = principal.multiply(monthlyRate, mc).multiply(pow, mc);
            BigDecimal denominator = pow.subtract(BigDecimal.ONE, mc);
            emi = numerator.divide(denominator, 2, RoundingMode.HALF_UP);
        } else {
            emi = principal.divide(BigDecimal.valueOf(tenure), 2, RoundingMode.HALF_UP);
        }

        List<RepaymentScheduleRowDto> rows = new ArrayList<>();
        BigDecimal balance = principal.setScale(2, RoundingMode.HALF_UP);
        DecimalFormat df = new DecimalFormat("#,##0.00");

        for (int i = 1; i <= tenure; i++) {
            BigDecimal openingBalance = balance;
            BigDecimal interest = openingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalComp;
            BigDecimal installmentAmt = emi;

            if (i == tenure) {
                principalComp = openingBalance;
                installmentAmt = principalComp.add(interest);
                balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                principalComp = installmentAmt.subtract(interest);
                if (principalComp.compareTo(openingBalance) > 0) {
                    principalComp = openingBalance;
                }
                balance = openingBalance.subtract(principalComp).setScale(2, RoundingMode.HALF_UP);
            }

            String monthLabel = startDate.plusMonths(i - 1L).format(MONTH_YEAR_FMT);
            rows.add(RepaymentScheduleRowDto.builder()
                    .installmentNumber(i)
                    .month(monthLabel)
                    .principalAmount(df.format(openingBalance))
                    .installmentAmount(df.format(installmentAmt))
                    .interest(df.format(interest))
                    .principal(df.format(principalComp))
                    .balanceAmount(df.format(balance))
                    .build());
        }
        return rows;
    }

    // =========================================================================
    // Master Resolution & Mapping Helpers
    // =========================================================================

    private Customer resolveCustomer(String customerIdOrCode) {
        String key = customerIdOrCode.trim();
        return customerRepository.findByCustomerCode(key)
                .or(() -> isNumeric(key) ? customerRepository.findById(Long.parseLong(key)) : java.util.Optional.empty())
                .orElseThrow(() -> new RetailLoanValidationException("Customer not found for customerId: " + key));
    }

    private Branch resolveBranch(String branchCodeOrId) {
        String key = branchCodeOrId.trim();
        return branchRepository.findByCodeIgnoreCase(key)
                .or(() -> isNumeric(key) ? branchRepository.findById(Long.parseLong(key)) : java.util.Optional.empty())
                .orElseThrow(() -> new RetailLoanValidationException("Branch not found for branch: " + key));
    }

    private LoanProduct resolveProduct(String productCode) {
        String key = productCode.trim();
        return loanProductRepository.findByCodeIgnoreCase(key)
                .orElseThrow(() -> new RetailLoanValidationException("Loan product not found for productCode: " + key));
    }

    private RetailLoanFormDto buildResponseFromEntity(RetailLoanApplication entity) {
        RetailLoanFormDto cached = extendedFormStore.get(entity.getId());
        boolean isEmi = "EMI".equalsIgnoreCase(entity.getEligibilityStatus()) || entity.getEmi() != null;

        if (cached != null) {
            cached.setId(entity.getId());
            cached.setLoanAccountNumber(entity.getApplicationNumber());
            cached.setAccountStatus(entity.getStatus());
            cached.setEmiApplicable(isEmi ? "Yes" : "No");
            cached.setBulletPayment(isEmi ? "No" : "Yes");
            if (!isEmi) {
                cached.setRepaymentSchedule(null);
            }
            cached.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
            cached.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
            return cached;
        }

        // Fallback mapping purely from persisted a001-a004 columns
        String customerCode = customerRepository.findById(entity.getCustomerId())
                .map(Customer::getCustomerCode)
                .orElse(String.valueOf(entity.getCustomerId()));
        String branchCode = entity.getBranchId() != null
                ? branchRepository.findById(entity.getBranchId()).map(Branch::getCode).orElse(String.valueOf(entity.getBranchId()))
                : null;
        String productName = loanProductRepository.findByCodeIgnoreCase(entity.getProductCode())
                .map(LoanProduct::getName)
                .orElse(null);

        RetailLoanFormDto dto = RetailLoanFormDto.builder()
                .id(entity.getId())
                .loanAccountNumber(entity.getApplicationNumber())
                .branch(branchCode)
                .customerId(customerCode)
                .productCode(entity.getProductCode())
                .productName(productName)
                .accountStatus(entity.getStatus())
                .loanAmount(entity.getAppliedAmount() != null ? entity.getAppliedAmount().toPlainString() : null)
                .interestRate(entity.getInterestRate() != null ? entity.getInterestRate().toPlainString() : null)
                .tenureMonths(entity.getTenureMonths() != null ? String.valueOf(entity.getTenureMonths()) : null)
                .instalmentAmount(entity.getEmi() != null ? entity.getEmi().toPlainString() : null)
                .securityAmount(entity.getCollateralValue() != null ? entity.getCollateralValue().toPlainString() : "0")
                .emiApplicable(isEmi ? "Yes" : "No")
                .bulletPayment(isEmi ? "No" : "Yes")
                .remarks(entity.getCheckerRemarks())
                .sanctionedDate(entity.getApprovedAt() != null ? entity.getApprovedAt().toLocalDate().toString() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .build();

        if (isEmi) {
            dto.setRepaymentSchedule(generateRepaymentChart(dto));
        }
        return dto;
    }

    private RetailLoanFormDto copyFormWithPersistedMetadata(
            RetailLoanFormDto form,
            RetailLoanApplication saved,
            Customer customer,
            Branch branch,
            LoanProduct product) {

        List<RelatedPartyDto> partiesCopy = form.getRelatedParties() != null
                ? new ArrayList<>(form.getRelatedParties())
                : new ArrayList<>();

        return RetailLoanFormDto.builder()
                .id(saved.getId())
                .loanAccountNumber(saved.getApplicationNumber())
                .branch(branch.getCode())
                .effectiveDate(form.getEffectiveDate())
                .customerId(customer.getCustomerCode())
                .title(form.getTitle())
                .firstName(form.getFirstName())
                .middleName(form.getMiddleName())
                .lastName(form.getLastName())
                .productCode(product.getCode())
                .productName(!isBlank(form.getProductName()) ? form.getProductName() : product.getName())
                .accountStatus(saved.getStatus())
                .modeOfOperation(form.getModeOfOperation())
                .lastTransactionDate(form.getLastTransactionDate())
                .dpd(form.getDpd())
                .relatedParties(partiesCopy)
                .sanctionedDate(form.getSanctionedDate())
                .sanctioningAuthority(form.getSanctioningAuthority())
                .loanAmount(form.getLoanAmount())
                .tenureMonths(form.getTenureMonths())
                .interestRate(form.getInterestRate())
                .offsetRate(form.getOffsetRate())
                .totalInterestRate(form.getTotalInterestRate())
                .emiApplicable(form.getEmiApplicable())
                .emiFixDate(form.getEmiFixDate())
                .emiType(form.getEmiType())
                .moratoriumApplicable(form.getMoratoriumApplicable())
                .moratoriumMonths(form.getMoratoriumMonths())
                .moratoriumFrom(form.getMoratoriumFrom())
                .moratoriumEndDate(form.getMoratoriumEndDate())
                .principalMoratorium(form.getPrincipalMoratorium())
                .interestMoratorium(form.getInterestMoratorium())
                .bulletPayment(form.getBulletPayment())
                .instalmentAmount(form.getInstalmentAmount())
                .instalmentStartDate(form.getInstalmentStartDate())
                .instalmentEndDate(form.getInstalmentEndDate())
                .expiryDate(form.getExpiryDate())
                .month(form.getMonth())
                .principalAmountRs(form.getPrincipalAmountRs())
                .installmentAmountRs(form.getInstallmentAmountRs())
                .interestRs(form.getInterestRs())
                .principalRs(form.getPrincipalRs())
                .balanceAmountRs(form.getBalanceAmountRs())
                .repaymentSchedule(form.getRepaymentSchedule())
                .purposeCode(form.getPurposeCode())
                .prioritySector(form.getPrioritySector())
                .prioritySectorCode(form.getPrioritySectorCode())
                .industryType(form.getIndustryType())
                .industrySubType(form.getIndustrySubType())
                .physicallyChallenged(form.getPhysicallyChallenged())
                .termCode(form.getTermCode())
                .securityId(form.getSecurityId())
                .securityType(form.getSecurityType())
                .securityEffectiveDate(form.getSecurityEffectiveDate())
                .assetType(form.getAssetType())
                .securityAmount(form.getSecurityAmount())
                .marginPercent(form.getMarginPercent())
                .assetAddress(form.getAssetAddress())
                .assetAddressLine2(form.getAssetAddressLine2())
                .city(form.getCity())
                .state(form.getState())
                .pincode(form.getPincode())
                .valuationAmount(form.getValuationAmount())
                .valuerName(form.getValuerName())
                .valuationDate(form.getValuationDate())
                .inspectionBy(form.getInspectionBy())
                .inspectionDate(form.getInspectionDate())
                .inspectionReportName(form.getInspectionReportName())
                .securityValue(form.getSecurityValue())
                .policyType(form.getPolicyType())
                .premiumFrequency(form.getPremiumFrequency())
                .policyStatus(form.getPolicyStatus())
                .autoRenew(form.getAutoRenew())
                .premiumAmount(form.getPremiumAmount())
                .policyNumber(form.getPolicyNumber())
                .sumAssured(form.getSumAssured())
                .insuranceCompany(form.getInsuranceCompany())
                .insuranceIssueDate(form.getInsuranceIssueDate())
                .insuranceExpiryDate(form.getInsuranceExpiryDate())
                .disbursementMode(form.getDisbursementMode())
                .disbursementAccountNumber(form.getDisbursementAccountNumber())
                .ifscCode(form.getIfscCode())
                .bankName(form.getBankName())
                .beneficiaryAccountName(form.getBeneficiaryAccountName())
                .beneficiaryMobileNumber(form.getBeneficiaryMobileNumber())
                .disbursementAmount(form.getDisbursementAmount())
                .remarks(saved.getCheckerRemarks())
                .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null)
                .updatedAt(saved.getUpdatedAt() != null ? saved.getUpdatedAt().toString() : null)
                .build();
    }

    private boolean isEmiApplicable(String val) {
        return "Yes".equalsIgnoreCase(val != null ? val.trim() : "");
    }

    private BigDecimal resolveEmiAmount(RetailLoanFormDto form, List<RepaymentScheduleRowDto> schedule) {
        BigDecimal explicit = parseBigDecimal(form.getInstalmentAmount());
        if (explicit != null) {
            return explicit;
        }
        if (schedule != null && !schedule.isEmpty()) {
            return parseBigDecimal(schedule.get(0).getInstallmentAmount());
        }
        return null;
    }

    private BigDecimal resolveCollateralValue(RetailLoanFormDto form) {
        BigDecimal val = parseBigDecimal(form.getSecurityAmount());
        return val != null ? val : BigDecimal.ZERO;
    }

    private LocalDateTime parseSanctionedTimestamp(String dateStr) {
        LocalDate d = parseDateOrDefault(dateStr, null);
        return d != null ? d.atStartOfDay() : null;
    }

    private LocalDate parseDateOrDefault(String value, LocalDate defaultDate) {
        if (isBlank(value) || "NA".equalsIgnoreCase(value.trim()) || "Not Applicable".equalsIgnoreCase(value.trim())) {
            return defaultDate;
        }
        if ("Current Date".equalsIgnoreCase(value.trim())) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return defaultDate;
        }
    }

    private BigDecimal parseBigDecimal(String val) {
        if (isBlank(val) || "NA".equalsIgnoreCase(val.trim()) || "Not Applicable".equalsIgnoreCase(val.trim())) {
            return null;
        }
        try {
            return new BigDecimal(val.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInteger(String val) {
        if (isBlank(val)) return null;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isNumeric(String s) {
        if (isBlank(s)) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }
}
