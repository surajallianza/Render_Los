package com.los.bank.customer.service;

import com.los.bank.branch.repository.BranchRepository;
import com.los.bank.customer.config.CustomerDocumentProperties;
import com.los.bank.customer.dto.*;
import com.los.bank.customer.entity.*;
import com.los.bank.customer.exception.*;
import com.los.bank.customer.repository.*;
import com.los.bank.lead.entity.Lead;
import com.los.bank.lead.repository.LeadRepository;
import com.los.bank.lookup.dto.LookupValue;
import com.los.bank.user.context.BankContext;
import com.los.bank.user.entity.UserSecuritySetting;
import com.los.bank.user.repository.UserSecuritySettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.Year;
import java.util.*;

@Service
@Transactional(transactionManager = "bankTransactionManager")
public class CustomerServiceImpl implements CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final CustomerContactRepository customerContactRepository;
    private final CustomerPersonalRepository customerPersonalRepository;
    private final CustomerIdentityRepository customerIdentityRepository;
    private final CustomerFamilyRepository customerFamilyRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final CustomerEmploymentRepository customerEmploymentRepository;
    private final CustomerPreviousEmploymentRepository customerPreviousEmploymentRepository;
    private final CustomerIncomeRepository customerIncomeRepository;
    private final CustomerBankAccountRepository customerBankAccountRepository;
    private final CustomerTaxReturnRepository customerTaxReturnRepository;
    private final CustomerDocumentRepository customerDocumentRepository;
    private final CustomerManualLoanHistoryRepository customerManualLoanHistoryRepository;
    private final CustomerReferenceRepository customerReferenceRepository;
    private final CustomerProfileViewRepository customerProfileViewRepository;
    private final LeadRepository leadRepository;
    private final BranchRepository branchRepository;
    private final UserSecuritySettingRepository userSecuritySettingRepository;
    private final CustomerCryptoService cryptoService;
    private final CustomerLookupHelper lookupHelper;
    private final CustomerValidator customerValidator;
    private final StorageService storageService;
    private final CustomerDocumentProperties documentProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CustomerServiceImpl(CustomerRepository customerRepository,
                               CustomerContactRepository customerContactRepository,
                               CustomerPersonalRepository customerPersonalRepository,
                               CustomerIdentityRepository customerIdentityRepository,
                               CustomerFamilyRepository customerFamilyRepository,
                               CustomerAddressRepository customerAddressRepository,
                               CustomerEmploymentRepository customerEmploymentRepository,
                               CustomerPreviousEmploymentRepository customerPreviousEmploymentRepository,
                               CustomerIncomeRepository customerIncomeRepository,
                               CustomerBankAccountRepository customerBankAccountRepository,
                               CustomerTaxReturnRepository customerTaxReturnRepository,
                               CustomerDocumentRepository customerDocumentRepository,
                               CustomerManualLoanHistoryRepository customerManualLoanHistoryRepository,
                               CustomerReferenceRepository customerReferenceRepository,
                               CustomerProfileViewRepository customerProfileViewRepository,
                               LeadRepository leadRepository,
                               BranchRepository branchRepository,
                               UserSecuritySettingRepository userSecuritySettingRepository,
                               CustomerCryptoService cryptoService,
                               CustomerLookupHelper lookupHelper,
                               CustomerValidator customerValidator,
                               StorageService storageService,
                               CustomerDocumentProperties documentProperties) {

        this.customerRepository = customerRepository;
        this.customerContactRepository = customerContactRepository;
        this.customerPersonalRepository = customerPersonalRepository;
        this.customerIdentityRepository = customerIdentityRepository;
        this.customerFamilyRepository = customerFamilyRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.customerEmploymentRepository = customerEmploymentRepository;
        this.customerPreviousEmploymentRepository = customerPreviousEmploymentRepository;
        this.customerIncomeRepository = customerIncomeRepository;
        this.customerBankAccountRepository = customerBankAccountRepository;
        this.customerTaxReturnRepository = customerTaxReturnRepository;
        this.customerDocumentRepository = customerDocumentRepository;
        this.customerManualLoanHistoryRepository = customerManualLoanHistoryRepository;
        this.customerReferenceRepository = customerReferenceRepository;
        this.customerProfileViewRepository = customerProfileViewRepository;
        this.leadRepository = leadRepository;
        this.branchRepository = branchRepository;
        this.userSecuritySettingRepository = userSecuritySettingRepository;
        this.cryptoService = cryptoService;
        this.lookupHelper = lookupHelper;
        this.customerValidator = customerValidator;
        this.storageService = storageService;
        this.documentProperties = documentProperties;
    }

    /**
     * Onboarding step 1 ("Continue" on Personal Details): creates the customer as a DRAFT.
     * Personal, identity and family details are required here; any other section sent
     * in the same request is saved too.
     */
    @Override
    public CustomerProfileResponse createCustomer(CustomerSaveRequest request, Long staffUserId) {
        if (request == null) {
            throw new CustomerValidationException("request", "Request body cannot be null");
        }
        Map<String, String> missing = new LinkedHashMap<>();
        if (request.getPersonal() == null) {
            missing.put("personal", "Personal details are required");
        }
        if (request.getIdentity() == null) {
            missing.put("identity", "Identity details are required");
        }
        if (request.getFamily() == null) {
            missing.put("family", "Family details are required");
        }
        if (!missing.isEmpty()) {
            throw new CustomerValidationException("Personal, identity and family details are required to start onboarding", missing);
        }

        CustomerValidator.ResolvedLookups resolved = customerValidator.validateSections(request, false, null);

        // 1. Branch check
        Long branchId = resolveBranchId(request, staffUserId);

        // 2. Lead check
        String leadId = resolveLeadId(request);
        Lead leadToLink = null;
        if (leadId != null) {
            leadToLink = leadRepository.findById(leadId)
                    .orElseThrow(() -> new CustomerValidationException("personal.leadId", "Lead not found with ID: " + leadId));
            if (customerPersonalRepository.existsByLeadId(leadId)) {
                throw new CustomerConflictException("Lead is already linked to another customer");
            }
        }

        // 3. Duplicate checks (drafts count too, so the same person cannot be onboarded twice)
        String panHash = cryptoService.computeHmacHex(request.getIdentity().getPanNumber());
        if (customerIdentityRepository.existsByPanHash(panHash)) {
            throw new CustomerConflictException("Duplicate customer PAN");
        }

        String aadhaarHash = cryptoService.computeHmacHex(request.getIdentity().getAadhaarNumber());
        if (customerIdentityRepository.existsByAadhaarHash(aadhaarHash)) {
            throw new CustomerConflictException("Duplicate customer Aadhaar");
        }

        String phone = request.getPersonal().getMobile().trim();
        if (customerContactRepository.existsByPhone(phone)) {
            throw new CustomerConflictException("Duplicate customer mobile number");
        }

        String email = request.getPersonal().getEmail().trim();
        if (customerContactRepository.existsByEmailIgnoreCase(email)) {
            throw new CustomerConflictException("Duplicate customer email");
        }

        // 4. Generate customer code: CUS-<year>-<LPAD(nextval, 5, '0')>
        Long nextSeq = customerRepository.getNextCustomerCodeSequence();
        String customerCode = String.format("CUS-%d-%05d", Year.now().getValue(), nextSeq);

        // 5. Create c001 Core as DRAFT
        Customer customer = Customer.builder()
                .customerCode(customerCode)
                .branchId(branchId)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .isActive(true)
                .onboardingStatus(CustomerOnboardingStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        customer = customerRepository.save(customer);
        Long customerId = customer.getId();

        // 6. Create c002 Contact
        CustomerContact contact = CustomerContact.builder()
                .customerId(customerId)
                .firstName(request.getPersonal().getFirstName().trim())
                .middleName(trimOrNull(request.getPersonal().getMiddleName()))
                .lastName(request.getPersonal().getLastName().trim())
                .email(email)
                .phone(phone)
                .build();
        customerContactRepository.save(contact);

        // 7. Create c004 Personal
        CustomerPersonal personal = CustomerPersonal.builder()
                .customerId(customerId)
                .leadId(leadId)
                .titleLookupId(resolved.titleLookupId)
                .genderLookupId(resolved.genderLookupId)
                .maritalStatusLookupId(resolved.maritalStatusLookupId)
                .customerTypeLookupId(resolved.customerTypeLookupId)
                .kycStatusLookupId(resolved.kycStatusLookupId)
                .dob(request.getPersonal().getDob())
                .occupation(request.getPersonal().getOccupation())
                .alternateContact(request.getPersonal().getAlternateContact())
                .createdBy(staffUserId)
                .modifiedBy(staffUserId)
                .build();
        customerPersonalRepository.save(personal);

        // Link lead if present
        if (leadToLink != null) {
            leadToLink.setCustomerId(customerId);
            leadRepository.save(leadToLink);
        }

        // 8. Create c005 Identity (client-sent validation statuses are ignored)
        CustomerIdentity identity = CustomerIdentity.builder()
                .customerId(customerId)
                .panNumber(cryptoService.encrypt(request.getIdentity().getPanNumber()))
                .panHash(panHash)
                .panValidationStatus("UNVERIFIED")
                .aadhaarNumber(cryptoService.encrypt(request.getIdentity().getAadhaarNumber()))
                .aadhaarHash(aadhaarHash)
                .aadhaarValidationStatus("UNVERIFIED")
                .ckycNumber(cryptoService.encrypt(request.getIdentity().getCkycNumber()))
                .build();
        customerIdentityRepository.save(identity);

        // 9. Family and any other step sent in the same request
        saveSections(customerId, request, resolved);

        return getCustomerByCode(customerCode);
    }

    /**
     * Onboarding steps 2..7 ("Continue") and edits: saves only the sections present in the
     * request. A section that is missing (null) is left as it is. For lists, null means
     * "not part of this save" and [] means "clear all rows".
     */
    @Override
    public CustomerProfileResponse updateCustomer(String customerCode, CustomerSaveRequest request, Long staffUserId) {
        Customer customer = findCustomer(customerCode);
        if (request == null) {
            throw new CustomerValidationException("request", "Request body cannot be null");
        }

        Long customerId = customer.getId();
        CustomerValidator.ResolvedLookups resolved =
                customerValidator.validateSections(request, true, loadStoredContext(customerId));

        if (request.getPersonal() != null) {
            updatePersonal(customerId, request, resolved, staffUserId);
        }
        if (request.getIdentity() != null) {
            updateIdentity(customerId, customerCode, request.getIdentity());
        }

        saveSections(customerId, request, resolved);
        removeRowsInvalidatedByStepChange(customerId, request, resolved);

        // Set c001.updated_at on EVERY customer update
        customer.setUpdatedAt(LocalDateTime.now());
        customerRepository.save(customer);

        return getCustomerByCode(customerCode);
    }

    /**
     * Final "Submit": checks that every mandatory step and document is present,
     * then moves the customer from DRAFT to COMPLETED.
     */
    @Override
    public CustomerProfileResponse submitCustomer(String customerCode, Long staffUserId) {
        Customer customer = findCustomer(customerCode);
        if (!CustomerOnboardingStatus.isDraft(customer.getOnboardingStatus())) {
            throw new CustomerConflictException("Customer onboarding is already submitted");
        }
        Long customerId = customer.getId();
        Map<String, String> errors = new LinkedHashMap<>();

        CustomerPersonal personal = customerPersonalRepository.findByCustomerId(customerId).orElse(null);
        if (personal == null || customerContactRepository.findByCustomerId(customerId).isEmpty()) {
            errors.put("personal", "Personal details are required");
        }

        CustomerIdentity identity = customerIdentityRepository.findByCustomerId(customerId).orElse(null);
        if (identity == null || identity.getPanNumber() == null
                || identity.getAadhaarNumber() == null || identity.getCkycNumber() == null) {
            errors.put("identity", "Identity details (PAN, Aadhaar and CKYC) are required");
        }

        Optional<CustomerFamily> family = customerFamilyRepository.findByCustomerId(customerId);
        if (family.isEmpty()) {
            errors.put("family", "Family details are required");
        } else if (personal != null && personal.getMaritalStatusLookupId() != null
                && lookupHelper.descriptionMatches(personal.getMaritalStatusLookupId(), "Married")
                && isBlank(family.get().getSpouseFirstName())) {
            errors.put("family.spouseFirstName", "Spouse details are required when marital status is Married");
        }

        if (customerAddressRepository.findByCustomerId(customerId).isEmpty()) {
            errors.put("address", "Address details are required");
        }
        if (customerEmploymentRepository.findByCustomerId(customerId).isEmpty()) {
            errors.put("employment", "Employment details are required");
        }
        if (customerIncomeRepository.findByCustomerId(customerId).isEmpty()) {
            errors.put("income", "Income details are required");
        }
        if (customerBankAccountRepository.findByCustomerId(customerId).isEmpty()) {
            errors.put("bank", "Bank details are required");
        }
        if (customerReferenceRepository.findByCustomerIdOrderBySrNoAsc(customerId).size() != 2) {
            errors.put("references", "Exactly two references are required");
        }

        checkRequiredDocuments(customerId, errors);

        if (!errors.isEmpty()) {
            throw new CustomerValidationException("Customer onboarding is incomplete", errors);
        }

        LocalDateTime now = LocalDateTime.now();
        customer.setOnboardingStatus(CustomerOnboardingStatus.COMPLETED);
        customer.setSubmittedAt(now);
        customer.setSubmittedBy(staffUserId);
        customer.setUpdatedAt(now);
        customerRepository.save(customer);

        logger.info("Customer onboarding submitted: {} by staff user {}", customerCode, staffUserId);
        return getCustomerByCode(customerCode);
    }

    /**
     * Discards an unfinished onboarding: deletes the DRAFT customer and all its rows
     * (child tables cascade in the database) and, after commit, its uploaded files.
     */
    @Override
    public void discardDraft(String customerCode, Long staffUserId) {
        Customer customer = findCustomer(customerCode);
        if (!CustomerOnboardingStatus.isDraft(customer.getOnboardingStatus())) {
            throw new CustomerConflictException("Only DRAFT customers can be discarded");
        }
        Long customerId = customer.getId();

        // l001.customer_id has no ON DELETE action, so unlink the lead first.
        customerPersonalRepository.findByCustomerId(customerId)
                .map(CustomerPersonal::getLeadId)
                .flatMap(leadRepository::findById)
                .filter(lead -> customerId.equals(lead.getCustomerId()))
                .ifPresent(lead -> {
                    lead.setCustomerId(null);
                    leadRepository.save(lead);
                });

        List<String> storagePaths = customerDocumentRepository.findByCustomerId(customerId).stream()
                .map(CustomerDocument::getStoragePath)
                .filter(Objects::nonNull)
                .toList();

        customerRepository.delete(customer);
        deleteFilesAfterCommit(storagePaths);

        logger.info("Draft customer {} discarded by staff user {} ({} document file(s) removed)",
                customerCode, staffUserId, storagePaths.size());
    }

    private void updatePersonal(Long customerId, CustomerSaveRequest request,
                                CustomerValidator.ResolvedLookups resolved, Long staffUserId) {
        // Lead check
        String leadId = resolveLeadId(request);
        if (leadId != null) {
            leadRepository.findById(leadId)
                    .orElseThrow(() -> new CustomerValidationException("personal.leadId", "Lead not found with ID: " + leadId));
            if (customerPersonalRepository.existsByLeadIdAndCustomerIdNot(leadId, customerId)) {
                throw new CustomerConflictException("Lead is already linked to another customer");
            }
        }

        // Duplicate checks ignore this customer's own record
        String phone = request.getPersonal().getMobile().trim();
        if (customerContactRepository.existsByPhoneAndCustomerIdNot(phone, customerId)) {
            throw new CustomerConflictException("Duplicate customer mobile number");
        }

        String email = request.getPersonal().getEmail().trim();
        if (customerContactRepository.existsByEmailIgnoreCaseAndCustomerIdNot(email, customerId)) {
            throw new CustomerConflictException("Duplicate customer email");
        }

        CustomerContact contact = customerContactRepository.findByCustomerId(customerId)
                .orElse(CustomerContact.builder().customerId(customerId).build());
        contact.setFirstName(request.getPersonal().getFirstName().trim());
        contact.setMiddleName(trimOrNull(request.getPersonal().getMiddleName()));
        contact.setLastName(request.getPersonal().getLastName().trim());
        contact.setEmail(email);
        contact.setPhone(phone);
        customerContactRepository.save(contact);

        CustomerPersonal personal = customerPersonalRepository.findByCustomerId(customerId)
                .orElse(CustomerPersonal.builder().customerId(customerId).createdBy(staffUserId).build());
        String previousLeadId = personal.getLeadId();
        personal.setLeadId(leadId);
        personal.setTitleLookupId(resolved.titleLookupId);
        personal.setGenderLookupId(resolved.genderLookupId);
        personal.setMaritalStatusLookupId(resolved.maritalStatusLookupId);
        personal.setCustomerTypeLookupId(resolved.customerTypeLookupId);
        personal.setKycStatusLookupId(resolved.kycStatusLookupId);
        personal.setDob(request.getPersonal().getDob());
        personal.setOccupation(request.getPersonal().getOccupation());
        personal.setAlternateContact(request.getPersonal().getAlternateContact());
        personal.setModifiedBy(staffUserId);
        customerPersonalRepository.save(personal);

        // Keep l001.customer_id in step with c004.lead_id when the lead changes
        if (!Objects.equals(previousLeadId, leadId)) {
            if (previousLeadId != null) {
                leadRepository.findById(previousLeadId)
                        .filter(lead -> customerId.equals(lead.getCustomerId()))
                        .ifPresent(lead -> {
                            lead.setCustomerId(null);
                            leadRepository.save(lead);
                        });
            }
            if (leadId != null) {
                leadRepository.findById(leadId).ifPresent(lead -> {
                    lead.setCustomerId(customerId);
                    leadRepository.save(lead);
                });
            }
        }
    }

    private void updateIdentity(Long customerId, String customerCode, IdentityRequest identityRequest) {
        CustomerIdentity existingIdentity = customerIdentityRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Identity record missing for customer: " + customerCode));

        // Masked values (as returned by GET) mean "unchanged"
        String rawPan = identityRequest.getPanNumber();
        if (rawPan != null && !cryptoService.isMasked(rawPan)) {
            String panHash = cryptoService.computeHmacHex(rawPan);
            if (customerIdentityRepository.existsByPanHashAndCustomerIdNot(panHash, customerId)) {
                throw new CustomerConflictException("Duplicate customer PAN");
            }
            if (!panHash.equals(existingIdentity.getPanHash())) {
                existingIdentity.setPanValidationStatus("UNVERIFIED");
            }
            existingIdentity.setPanHash(panHash);
            existingIdentity.setPanNumber(cryptoService.encrypt(rawPan));
        }
        if (existingIdentity.getPanValidationStatus() == null) {
            existingIdentity.setPanValidationStatus("UNVERIFIED");
        }

        String rawAadhaar = identityRequest.getAadhaarNumber();
        if (rawAadhaar != null && !cryptoService.isMasked(rawAadhaar)) {
            String aadhaarHash = cryptoService.computeHmacHex(rawAadhaar);
            if (customerIdentityRepository.existsByAadhaarHashAndCustomerIdNot(aadhaarHash, customerId)) {
                throw new CustomerConflictException("Duplicate customer Aadhaar");
            }
            if (!aadhaarHash.equals(existingIdentity.getAadhaarHash())) {
                existingIdentity.setAadhaarValidationStatus("UNVERIFIED");
            }
            existingIdentity.setAadhaarHash(aadhaarHash);
            existingIdentity.setAadhaarNumber(cryptoService.encrypt(rawAadhaar));
        }
        if (existingIdentity.getAadhaarValidationStatus() == null) {
            existingIdentity.setAadhaarValidationStatus("UNVERIFIED");
        }

        String rawCkyc = identityRequest.getCkycNumber();
        if (rawCkyc != null && !cryptoService.isMasked(rawCkyc)) {
            existingIdentity.setCkycNumber(cryptoService.encrypt(rawCkyc));
        }
        customerIdentityRepository.save(existingIdentity);
    }

    /**
     * Upserts every section from family onwards that is present in the request.
     */
    private void saveSections(Long customerId, CustomerSaveRequest request, CustomerValidator.ResolvedLookups resolved) {
        // c006 Family
        FamilyRequest famReq = request.getFamily();
        if (famReq != null) {
            CustomerFamily family = customerFamilyRepository.findByCustomerId(customerId)
                    .orElse(CustomerFamily.builder().customerId(customerId).build());
            family.setFatherTitleLookupId(resolved.fatherTitleLookupId);
            family.setFatherFirstName(famReq.getFatherFirstName().trim());
            family.setFatherMiddleName(famReq.getFatherMiddleName());
            family.setFatherLastName(famReq.getFatherLastName().trim());
            family.setMotherTitleLookupId(resolved.motherTitleLookupId);
            family.setMotherFirstName(famReq.getMotherFirstName().trim());
            family.setMotherMiddleName(famReq.getMotherMiddleName());
            family.setMotherLastName(famReq.getMotherLastName().trim());
            family.setSpouseTitleLookupId(resolved.spouseTitleLookupId);
            family.setSpouseFirstName(trimOrNull(famReq.getSpouseFirstName()));
            family.setSpouseMiddleName(famReq.getSpouseMiddleName());
            family.setSpouseLastName(trimOrNull(famReq.getSpouseLastName()));
            customerFamilyRepository.save(family);
        }

        // c007 Address
        AddressRequest addrReq = request.getAddress();
        if (addrReq != null) {
            CustomerAddress address = customerAddressRepository.findByCustomerId(customerId)
                    .orElse(CustomerAddress.builder().customerId(customerId).build());
            address.setResidentialStatusLookupId(resolved.residentialStatusLookupId);
            address.setResidentialTypeLookupId(resolved.residentialTypeLookupId);
            address.setFlatBuildingName(addrReq.getFlatBuildingName().trim());
            address.setStreetRoad(addrReq.getStreetRoad().trim());
            address.setLandmark(addrReq.getLandmark());
            address.setCity(addrReq.getCity().trim());
            address.setStateLookupId(resolved.stateLookupId);
            address.setPincode(addrReq.getPincode().trim());
            customerAddressRepository.save(address);
        }

        // c008 Employment
        EmploymentRequest empReq = request.getEmployment();
        if (empReq != null) {
            CustomerEmployment employment = customerEmploymentRepository.findByCustomerId(customerId)
                    .orElse(CustomerEmployment.builder().customerId(customerId).build());
            employment.setEmploymentTypeLookupId(resolved.employmentTypeLookupId);
            employment.setEmployerTypeLookupId(resolved.employerTypeLookupId);
            employment.setDesignation(empReq.getDesignation().trim());
            employment.setEmployerName(empReq.getEmployerName().trim());
            employment.setDateOfJoining(empReq.getDateOfJoining());
            employment.setQualification(empReq.getQualification());
            employment.setEmployerEmail(trimOrNull(empReq.getEmployerEmail()));
            employment.setListedStatusLookupId(resolved.listedStatusLookupId);
            employment.setYearsOfService(empReq.getYearsOfService());
            employment.setLocation(empReq.getLocation());
            employment.setDateOfRetirement(empReq.getDateOfRetirement());
            customerEmploymentRepository.save(employment);
        }

        // c009 Previous Employment (replace all rows)
        if (request.getPreviousEmployment() != null) {
            customerPreviousEmploymentRepository.deleteByCustomerId(customerId);
            customerPreviousEmploymentRepository.flush();
            for (int i = 0; i < request.getPreviousEmployment().size(); i++) {
                PreviousEmploymentRequest peReq = request.getPreviousEmployment().get(i);
                Long peEmpTypeId = (resolved.previousEmploymentTypeLookupIds.size() > i)
                        ? resolved.previousEmploymentTypeLookupIds.get(i) : null;
                customerPreviousEmploymentRepository.save(CustomerPreviousEmployment.builder()
                        .customerId(customerId)
                        .employmentTypeLookupId(peEmpTypeId)
                        .employerName(peReq.getEmployerName().trim())
                        .designation(peReq.getDesignation().trim())
                        .location(peReq.getLocation())
                        .dateOfJoining(peReq.getDateOfJoining())
                        .dateOfRelieving(peReq.getDateOfRelieving())
                        .build());
            }
        }

        // c010 Income
        IncomeRequest incReq = request.getIncome();
        if (incReq != null) {
            CustomerIncome income = customerIncomeRepository.findByCustomerId(customerId)
                    .orElse(CustomerIncome.builder().customerId(customerId).build());
            income.setAnnualIncome(incReq.getAnnualIncome());
            income.setOtherSourceIncome(incReq.getOtherSourceIncome());
            income.setTotalGrossIncome(incReq.getTotalGrossIncome());
            income.setTotalNetIncome(incReq.getTotalNetIncome());
            income.setIncomeTax(incReq.getIncomeTax());
            income.setIncomeTaxFilerStatusLookupId(resolved.incomeTaxFilerStatusLookupId);
            income.setRiskCategoryLookupId(resolved.riskCategoryLookupId);
            customerIncomeRepository.save(income);
        }

        // c011 Bank Account
        BankRequest bankReq = request.getBank();
        if (bankReq != null) {
            CustomerBankAccount bankAccount = customerBankAccountRepository.findByCustomerId(customerId)
                    .orElse(CustomerBankAccount.builder().customerId(customerId).build());
            bankAccount.setBankName(bankReq.getBankName().trim());
            String rawAccount = bankReq.getAccountNumber();
            if (rawAccount != null && !cryptoService.isMasked(rawAccount)) {
                bankAccount.setAccountNumber(cryptoService.encrypt(rawAccount.trim()));
            }
            bankAccount.setAccountTypeLookupId(resolved.accountTypeLookupId);
            bankAccount.setAccountStatusLookupId(resolved.accountStatusLookupId);
            customerBankAccountRepository.save(bankAccount);
        }

        // c012 Tax Returns (replace all rows; empty rows are skipped)
        if (request.getTaxReturns() != null) {
            customerTaxReturnRepository.deleteByCustomerId(customerId);
            customerTaxReturnRepository.flush();
            for (TaxReturnRequest trReq : request.getTaxReturns()) {
                if (trReq.getGrossIncome() == null && trReq.getTdsPaid() == null) {
                    continue;
                }
                customerTaxReturnRepository.save(CustomerTaxReturn.builder()
                        .customerId(customerId)
                        .financialYear(trReq.getFinancialYear().trim())
                        .grossIncome(trReq.getGrossIncome())
                        .tdsPaid(trReq.getTdsPaid())
                        .build());
            }
        }

        // c014 Manual Loan History (replace all rows)
        if (request.getLoanHistory() != null) {
            customerManualLoanHistoryRepository.deleteByCustomerId(customerId);
            customerManualLoanHistoryRepository.flush();
            for (LoanHistoryRequest lhReq : request.getLoanHistory()) {
                customerManualLoanHistoryRepository.save(CustomerManualLoanHistory.builder()
                        .customerId(customerId)
                        .bankName(lhReq.getBankName().trim())
                        .accountNumber(lhReq.getAccountNumber().trim())
                        .loanType(lhReq.getLoanType().trim())
                        .totalOutstanding(lhReq.getTotalOutstanding())
                        .emi(lhReq.getEmi())
                        .expiryDate(lhReq.getExpiryDate())
                        .build());
            }
        }

        // c015 References (replace all rows)
        if (request.getReferences() != null) {
            customerReferenceRepository.deleteByCustomerId(customerId);
            customerReferenceRepository.flush();
            for (ReferenceRequest refReq : request.getReferences()) {
                customerReferenceRepository.save(CustomerReference.builder()
                        .customerId(customerId)
                        .srNo(refReq.getSrNo())
                        .referenceName(refReq.getReferenceName().trim())
                        .relation(refReq.getRelation().trim())
                        .location(refReq.getLocation().trim())
                        .contactNo(refReq.getContactNo().trim())
                        .email(refReq.getEmail().trim())
                        .build());
            }
        }
    }

    /**
     * When a step is changed on its own, rows saved earlier can become invalid.
     * Self employed customers cannot have previous employment, and non-filers
     * cannot have tax returns, so those rows are removed.
     */
    private void removeRowsInvalidatedByStepChange(Long customerId, CustomerSaveRequest request,
                                                   CustomerValidator.ResolvedLookups resolved) {
        if (request.getEmployment() != null && request.getPreviousEmployment() == null
                && resolved.employmentTypeLookupId != null
                && lookupHelper.descriptionMatches(resolved.employmentTypeLookupId, "Self Employed")) {
            customerPreviousEmploymentRepository.deleteByCustomerId(customerId);
        }
        if (request.getIncome() != null && request.getTaxReturns() == null
                && resolved.incomeTaxFilerStatusLookupId != null
                && !lookupHelper.descriptionMatches(resolved.incomeTaxFilerStatusLookupId, "Filer")) {
            customerTaxReturnRepository.deleteByCustomerId(customerId);
        }
    }

    private CustomerValidator.StoredContext loadStoredContext(Long customerId) {
        CustomerValidator.StoredContext stored = new CustomerValidator.StoredContext();
        customerPersonalRepository.findByCustomerId(customerId)
                .ifPresent(p -> stored.maritalStatusLookupId = p.getMaritalStatusLookupId());
        customerEmploymentRepository.findByCustomerId(customerId)
                .ifPresent(e -> stored.employmentTypeLookupId = e.getEmploymentTypeLookupId());
        customerIncomeRepository.findByCustomerId(customerId)
                .ifPresent(i -> stored.incomeTaxFilerStatusLookupId = i.getIncomeTaxFilerStatusLookupId());
        return stored;
    }

    private void checkRequiredDocuments(Long customerId, Map<String, String> errors) {
        List<CustomerDocument> activeDocs =
                customerDocumentRepository.findByCustomerIdAndIsDeletedFalseOrderByUploadedAtDesc(customerId);

        int minCount = Math.max(0, documentProperties.getMinCount());
        if (activeDocs.size() < minCount) {
            errors.put("documents", "At least " + minCount + " document(s) must be uploaded");
        }

        Set<Long> uploadedTypeIds = new HashSet<>();
        for (CustomerDocument doc : activeDocs) {
            if (doc.getDocumentTypeLookupId() != null) {
                uploadedTypeIds.add(doc.getDocumentTypeLookupId());
            }
        }
        for (String typeCode : documentProperties.getRequiredTypes()) {
            if (isBlank(typeCode)) {
                continue;
            }
            String code = typeCode.trim();
            Map<String, String> lookupErrors = new LinkedHashMap<>();
            Long typeId = lookupHelper.resolveId("Document Type", lookupHelper.getProperties().getDocumentType(),
                    code, "documents." + code, lookupErrors);
            if (typeId == null) {
                logger.warn("Required document type '{}' is not configured in the Document Type lookup", code);
                continue;
            }
            if (!uploadedTypeIds.contains(typeId)) {
                LookupValue type = lookupHelper.resolveLookupValue(typeId);
                String label = type != null && type.getName() != null ? type.getName() : code;
                errors.put("documents." + code, label + " document is required");
            }
        }
    }

    private void deleteFilesAfterCommit(List<String> storagePaths) {
        if (storagePaths.isEmpty()) {
            return;
        }
        Runnable deleteFiles = () -> storagePaths.forEach(path -> {
            try {
                storageService.deleteFile(path);
            } catch (RuntimeException e) {
                logger.warn("Could not delete document file {}: {}", path, e.getMessage());
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteFiles.run();
                }
            });
        } else {
            deleteFiles.run();
        }
    }

    private Customer findCustomer(String customerCode) {
        return customerRepository.findByCustomerCode(customerCode)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with code: " + customerCode));
    }

    private static String trimOrNull(String value) {
        return value != null ? value.trim() : null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public CustomerProfileResponse getCustomerByCode(String customerCode) {
        Customer customer = customerRepository.findByCustomerCode(customerCode)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with code: " + customerCode));

        Long customerId = customer.getId();
        CustomerContact contact = customerContactRepository.findByCustomerId(customerId).orElse(null);
        CustomerPersonal personal = customerPersonalRepository.findByCustomerId(customerId).orElse(null);
        CustomerIdentity identity = customerIdentityRepository.findByCustomerId(customerId).orElse(null);
        CustomerFamily family = customerFamilyRepository.findByCustomerId(customerId).orElse(null);
        CustomerAddress address = customerAddressRepository.findByCustomerId(customerId).orElse(null);
        CustomerEmployment employment = customerEmploymentRepository.findByCustomerId(customerId).orElse(null);
        List<CustomerPreviousEmployment> prevList = customerPreviousEmploymentRepository.findByCustomerId(customerId);
        CustomerIncome income = customerIncomeRepository.findByCustomerId(customerId).orElse(null);
        CustomerBankAccount bankAccount = customerBankAccountRepository.findByCustomerId(customerId).orElse(null);
        List<CustomerTaxReturn> taxReturns = customerTaxReturnRepository.findByCustomerIdOrderByFinancialYearDesc(customerId);
        List<CustomerManualLoanHistory> loans = customerManualLoanHistoryRepository.findByCustomerId(customerId);
        List<CustomerReference> references = customerReferenceRepository.findByCustomerIdOrderBySrNoAsc(customerId);

        // Build Personal Response
        PersonalResponse personalResp = null;
        if (personal != null && contact != null) {
            Integer age = personal.getDob() != null ? Period.between(personal.getDob(), LocalDate.now()).getYears() : null;
            personalResp = PersonalResponse.builder()
                    .title(lookupHelper.resolveLookupValue(personal.getTitleLookupId()))
                    .firstName(contact.getFirstName())
                    .middleName(contact.getMiddleName())
                    .lastName(contact.getLastName())
                    .dob(personal.getDob())
                    .age(age)
                    .gender(lookupHelper.resolveLookupValue(personal.getGenderLookupId()))
                    .maritalStatus(lookupHelper.resolveLookupValue(personal.getMaritalStatusLookupId()))
                    .customerType(lookupHelper.resolveLookupValue(personal.getCustomerTypeLookupId()))
                    .kycStatus(lookupHelper.resolveLookupValue(personal.getKycStatusLookupId()))
                    .occupation(personal.getOccupation())
                    .mobile(contact.getPhone())
                    .alternateContact(personal.getAlternateContact())
                    .email(contact.getEmail())
                    .leadId(personal.getLeadId())
                    .branchId(customer.getBranchId())
                    .build();
        }

        // Build Identity Response with Masked values
        IdentityResponse identityResp = null;
        if (identity != null) {
            String decPan = cryptoService.decrypt(identity.getPanNumber());
            String decAadhaar = cryptoService.decrypt(identity.getAadhaarNumber());
            String decCkyc = cryptoService.decrypt(identity.getCkycNumber());

            identityResp = IdentityResponse.builder()
                    .panNumber(cryptoService.maskPan(decPan))
                    .panValidationStatus(identity.getPanValidationStatus())
                    .aadhaarNumber(cryptoService.maskAadhaar(decAadhaar))
                    .aadhaarValidationStatus(identity.getAadhaarValidationStatus())
                    .ckycNumber(cryptoService.maskCkyc(decCkyc))
                    .build();
        }

        // Build Family Response
        FamilyResponse familyResp = null;
        if (family != null) {
            familyResp = FamilyResponse.builder()
                    .fatherTitle(lookupHelper.resolveLookupValue(family.getFatherTitleLookupId()))
                    .fatherFirstName(family.getFatherFirstName())
                    .fatherMiddleName(family.getFatherMiddleName())
                    .fatherLastName(family.getFatherLastName())
                    .motherTitle(lookupHelper.resolveLookupValue(family.getMotherTitleLookupId()))
                    .motherFirstName(family.getMotherFirstName())
                    .motherMiddleName(family.getMotherMiddleName())
                    .motherLastName(family.getMotherLastName())
                    .spouseTitle(lookupHelper.resolveLookupValue(family.getSpouseTitleLookupId()))
                    .spouseFirstName(family.getSpouseFirstName())
                    .spouseMiddleName(family.getSpouseMiddleName())
                    .spouseLastName(family.getSpouseLastName())
                    .build();
        }

        // Build Address Response
        AddressResponse addressResp = null;
        if (address != null) {
            addressResp = AddressResponse.builder()
                    .residentialStatus(lookupHelper.resolveLookupValue(address.getResidentialStatusLookupId()))
                    .residentialType(lookupHelper.resolveLookupValue(address.getResidentialTypeLookupId()))
                    .flatBuildingName(address.getFlatBuildingName())
                    .streetRoad(address.getStreetRoad())
                    .landmark(address.getLandmark())
                    .city(address.getCity())
                    .state(lookupHelper.resolveLookupValue(address.getStateLookupId()))
                    .pincode(address.getPincode())
                    .build();
        }

        // Build Employment Response
        EmploymentResponse employmentResp = null;
        if (employment != null) {
            employmentResp = EmploymentResponse.builder()
                    .employmentType(lookupHelper.resolveLookupValue(employment.getEmploymentTypeLookupId()))
                    .employerType(lookupHelper.resolveLookupValue(employment.getEmployerTypeLookupId()))
                    .designation(employment.getDesignation())
                    .employerName(employment.getEmployerName())
                    .dateOfJoining(employment.getDateOfJoining())
                    .qualification(employment.getQualification())
                    .employerEmail(employment.getEmployerEmail())
                    .listedStatus(lookupHelper.resolveLookupValue(employment.getListedStatusLookupId()))
                    .yearsOfService(employment.getYearsOfService())
                    .location(employment.getLocation())
                    .dateOfRetirement(employment.getDateOfRetirement())
                    .build();
        }

        // Build Previous Employment Response
        List<PreviousEmploymentResponse> prevRespList = new ArrayList<>();
        for (CustomerPreviousEmployment pe : prevList) {
            prevRespList.add(PreviousEmploymentResponse.builder()
                    .id(pe.getId())
                    .employmentType(lookupHelper.resolveLookupValue(pe.getEmploymentTypeLookupId()))
                    .employerName(pe.getEmployerName())
                    .designation(pe.getDesignation())
                    .location(pe.getLocation())
                    .dateOfJoining(pe.getDateOfJoining())
                    .dateOfRelieving(pe.getDateOfRelieving())
                    .build());
        }

        // Build Income Response
        IncomeResponse incomeResp = null;
        if (income != null) {
            incomeResp = IncomeResponse.builder()
                    .annualIncome(income.getAnnualIncome())
                    .otherSourceIncome(income.getOtherSourceIncome())
                    .totalGrossIncome(income.getTotalGrossIncome())
                    .totalNetIncome(income.getTotalNetIncome())
                    .incomeTax(income.getIncomeTax())
                    .incomeTaxFilerStatus(lookupHelper.resolveLookupValue(income.getIncomeTaxFilerStatusLookupId()))
                    .riskCategory(lookupHelper.resolveLookupValue(income.getRiskCategoryLookupId()))
                    .build();
        }

        // Build Bank Response
        BankResponse bankResp = null;
        if (bankAccount != null) {
            String decAccount = cryptoService.decrypt(bankAccount.getAccountNumber());
            bankResp = BankResponse.builder()
                    .bankName(bankAccount.getBankName())
                    .accountNumber(cryptoService.maskAccount(decAccount))
                    .accountType(lookupHelper.resolveLookupValue(bankAccount.getAccountTypeLookupId()))
                    .accountStatus(lookupHelper.resolveLookupValue(bankAccount.getAccountStatusLookupId()))
                    .build();
        }

        // Build Tax Returns Response
        List<TaxReturnResponse> taxRespList = new ArrayList<>();
        for (CustomerTaxReturn tr : taxReturns) {
            taxRespList.add(TaxReturnResponse.builder()
                    .id(tr.getId())
                    .financialYear(tr.getFinancialYear())
                    .grossIncome(tr.getGrossIncome())
                    .tdsPaid(tr.getTdsPaid())
                    .build());
        }

        // Build Loan History Response
        List<LoanHistoryResponse> loanRespList = new ArrayList<>();
        for (CustomerManualLoanHistory lh : loans) {
            loanRespList.add(LoanHistoryResponse.builder()
                    .id(lh.getId())
                    .bankName(lh.getBankName())
                    .accountNumber(lh.getAccountNumber())
                    .loanType(lh.getLoanType())
                    .totalOutstanding(lh.getTotalOutstanding())
                    .emi(lh.getEmi())
                    .expiryDate(lh.getExpiryDate())
                    .source("MANUAL")
                    .build());
        }

        // Build References Response
        List<ReferenceResponse> refRespList = new ArrayList<>();
        for (CustomerReference ref : references) {
            refRespList.add(ReferenceResponse.builder()
                    .id(ref.getId())
                    .srNo(ref.getSrNo())
                    .referenceName(ref.getReferenceName())
                    .relation(ref.getRelation())
                    .location(ref.getLocation())
                    .contactNo(ref.getContactNo())
                    .email(ref.getEmail())
                    .build());
        }

        return CustomerProfileResponse.builder()
                .customerCode(customer.getCustomerCode())
                .customerId(customer.getId())
                .branchId(customer.getBranchId())
                .isActive(customer.getIsActive())
                .onboardingStatus(customer.getOnboardingStatus())
                .submittedAt(customer.getSubmittedAt())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .personal(personalResp)
                .identity(identityResp)
                .family(familyResp)
                .address(addressResp)
                .employment(employmentResp)
                .previousEmployment(prevRespList)
                .income(incomeResp)
                .bank(bankResp)
                .taxReturns(taxRespList)
                .loanHistory(loanRespList)
                .references(refRespList)
                .build();
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public CustomerPageResponse<CustomerListItemDto> getCustomers(String search, String status, String customerType, String onboardingStatus, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<CustomerProfileView> spec = (root, query, cb) -> cb.conjunction();

        if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("customerCode")), pattern),
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("mobileNumber")), pattern)
            ));
        }

        if (status != null && !status.trim().isEmpty()) {
            boolean activeVal = "active".equalsIgnoreCase(status.trim()) || "true".equalsIgnoreCase(status.trim());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), activeVal));
        }

        // Onboarding status: default COMPLETED (drafts are hidden), DRAFT, or ALL
        String onboardingFilter = isBlank(onboardingStatus) ? CustomerOnboardingStatus.COMPLETED : onboardingStatus.trim().toUpperCase();
        if (!"ALL".equals(onboardingFilter)) {
            if (!CustomerOnboardingStatus.DRAFT.equals(onboardingFilter) && !CustomerOnboardingStatus.COMPLETED.equals(onboardingFilter)) {
                throw new CustomerValidationException("onboardingStatus", "onboardingStatus must be DRAFT, COMPLETED or ALL");
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("onboardingStatus"), onboardingFilter));
        }

        if (customerType != null && !customerType.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("customerType")), customerType.trim().toLowerCase()));
        }

        Page<CustomerProfileView> profilePage = customerProfileViewRepository.findAll(spec, pageable);

        List<CustomerListItemDto> dtoList = profilePage.getContent().stream().map(view -> {
            String fullName = view.getFirstName();
            if (view.getMiddleName() != null && !view.getMiddleName().trim().isEmpty()) {
                fullName += " " + view.getMiddleName().trim();
            }
            if (view.getLastName() != null && !view.getLastName().trim().isEmpty()) {
                fullName += " " + view.getLastName().trim();
            }

            return CustomerListItemDto.builder()
                    .customerCode(view.getCustomerCode())
                    .fullName(fullName)
                    .customerType(view.getCustomerType())
                    .kycStatus(view.getKycStatus())
                    .mobile(view.getMobileNumber())
                    .email(view.getEmail())
                    .branch(view.getBranchName() != null ? view.getBranchName() : view.getBranchCode())
                    .isActive(view.getIsActive())
                    .onboardingStatus(view.getOnboardingStatus())
                    .dob(view.getDob())
                    .age(view.getAge())
                    .createdAt(view.getCreatedAt())
                    .build();
        }).toList();

        return CustomerPageResponse.<CustomerListItemDto>builder()
                .content(dtoList)
                .page(profilePage.getNumber())
                .size(profilePage.getSize())
                .totalElements(profilePage.getTotalElements())
                .totalPages(profilePage.getTotalPages())
                .last(profilePage.isLast())
                .build();
    }

    /**
     * Uploads one document. When the document type allows only one file per customer
     * (not listed in app.customer.documents.multiple-allowed-types), the customer's
     * previous active file of that type is replaced (soft deleted).
     */
    @Override
    public CustomerDocumentResponse uploadDocument(String customerCode, MultipartFile file, String documentTypeCode, LocalDate expiryDate, String remarks, Long staffUserId) {
        Customer customer = findCustomer(customerCode);
        Long docTypeLookupId = resolveDocumentTypeId(documentTypeCode);

        CustomerDocument saved = storeDocument(customer, file, docTypeLookupId, expiryDate, remarks, staffUserId);

        List<Long> replacedIds = new ArrayList<>();
        if (docTypeLookupId != null && !allowsMultipleFiles(docTypeLookupId)) {
            for (CustomerDocument old : customerDocumentRepository
                    .findByCustomerIdAndDocumentTypeLookupIdAndIsDeletedFalse(customer.getId(), docTypeLookupId)) {
                if (!old.getId().equals(saved.getId())) {
                    softDelete(old, staffUserId);
                    replacedIds.add(old.getId());
                }
            }
        }
        return toDocumentResponse(saved, customerCode, replacedIds);
    }

    /**
     * Replaces one uploaded document with a new file in a single step: the new file is
     * stored and the old one soft deleted. Type, expiry date and remarks are kept from
     * the old document unless new values are sent.
     */
    @Override
    public CustomerDocumentResponse replaceDocument(String customerCode, Long documentId, MultipartFile file, String documentTypeCode, LocalDate expiryDate, String remarks, Long staffUserId) {
        Customer customer = findCustomer(customerCode);
        CustomerDocument old = customerDocumentRepository.findByIdAndCustomerIdAndIsDeletedFalse(documentId, customer.getId())
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId + " for customer: " + customerCode));

        Long docTypeLookupId = isBlank(documentTypeCode) ? old.getDocumentTypeLookupId() : resolveDocumentTypeId(documentTypeCode);
        LocalDate newExpiry = expiryDate != null ? expiryDate : old.getExpiryDate();
        String newRemarks = remarks != null ? remarks : old.getRemarks();

        CustomerDocument saved = storeDocument(customer, file, docTypeLookupId, newExpiry, newRemarks, staffUserId);

        softDelete(old, staffUserId);
        List<Long> replacedIds = new ArrayList<>(List.of(old.getId()));

        if (docTypeLookupId != null && !allowsMultipleFiles(docTypeLookupId)) {
            for (CustomerDocument other : customerDocumentRepository
                    .findByCustomerIdAndDocumentTypeLookupIdAndIsDeletedFalse(customer.getId(), docTypeLookupId)) {
                if (!other.getId().equals(saved.getId()) && !other.getId().equals(old.getId())) {
                    softDelete(other, staffUserId);
                    replacedIds.add(other.getId());
                }
            }
        }
        return toDocumentResponse(saved, customerCode, replacedIds);
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public List<CustomerDocumentResponse> getCustomerDocuments(String customerCode) {
        Customer customer = findCustomer(customerCode);

        List<CustomerDocument> docs = customerDocumentRepository.findByCustomerIdAndIsDeletedFalseOrderByUploadedAtDesc(customer.getId());
        return docs.stream().map(doc -> toDocumentResponse(doc, customerCode, null)).toList();
    }

    @Override
    @Transactional(transactionManager = "bankTransactionManager", readOnly = true)
    public CustomerDocumentFile getDocumentFile(String customerCode, Long documentId) {
        Customer customer = findCustomer(customerCode);

        CustomerDocument doc = customerDocumentRepository.findByIdAndCustomerIdAndIsDeletedFalse(documentId, customer.getId())
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId + " for customer: " + customerCode));

        byte[] data = storageService.loadFile(doc.getStoragePath());
        return new CustomerDocumentFile(doc.getOriginalFileName(), doc.getContentType(), data);
    }

    @Override
    public void deleteDocument(String customerCode, Long documentId, Long staffUserId) {
        Customer customer = findCustomer(customerCode);

        CustomerDocument doc = customerDocumentRepository.findByIdAndCustomerIdAndIsDeletedFalse(documentId, customer.getId())
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId + " for customer: " + customerCode));

        softDelete(doc, staffUserId);
    }

    private CustomerDocument storeDocument(Customer customer, MultipartFile file, Long docTypeLookupId,
                                           LocalDate expiryDate, String remarks, Long staffUserId) {
        CustomerPersonal personal = customerPersonalRepository.findByCustomerId(customer.getId()).orElse(null);
        String tenant = BankContext.getCurrentBankCode();
        if (tenant == null || tenant.trim().isEmpty()) {
            tenant = BankContext.getCurrentDatabase();
        }
        if (tenant == null || tenant.trim().isEmpty()) {
            tenant = "default";
        }
        String entityFolder = (personal != null && personal.getLeadId() != null && !personal.getLeadId().trim().isEmpty())
                ? personal.getLeadId().trim()
                : customer.getCustomerCode();
        String subFolder = tenant + "/" + entityFolder;

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new DocumentStorageException("Could not read uploaded file content: " + e.getMessage(), e);
        }

        String relativePath = storageService.storeFile(subFolder, file.getOriginalFilename(), content, file.getContentType());

        CustomerDocument doc = CustomerDocument.builder()
                .customerId(customer.getId())
                .documentTypeLookupId(docTypeLookupId)
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .storagePath(relativePath)
                .expiryDate(expiryDate)
                .remarks(remarks)
                .isDeleted(false)
                .uploadedBy(staffUserId)
                .uploadedAt(LocalDateTime.now())
                .build();
        return customerDocumentRepository.save(doc);
    }

    private Long resolveDocumentTypeId(String documentTypeCode) {
        if (isBlank(documentTypeCode)) {
            return null;
        }
        Map<String, String> errors = new LinkedHashMap<>();
        Long id = lookupHelper.resolveId("Document Type", lookupHelper.getProperties().getDocumentType(),
                documentTypeCode.trim(), "documentTypeCode", errors);
        if (!errors.isEmpty()) {
            throw new CustomerValidationException("Document validation failed", errors);
        }
        return id;
    }

    private boolean allowsMultipleFiles(Long docTypeLookupId) {
        List<String> multiple = documentProperties.getMultipleAllowedTypes();
        if (multiple == null || multiple.isEmpty()) {
            return false;
        }
        LookupValue type = lookupHelper.resolveLookupValue(docTypeLookupId);
        if (type == null || type.getCode() == null) {
            return false;
        }
        return multiple.stream().anyMatch(code -> code != null && code.trim().equalsIgnoreCase(type.getCode().trim()));
    }

    private void softDelete(CustomerDocument doc, Long staffUserId) {
        doc.setIsDeleted(true);
        doc.setDeletedBy(staffUserId);
        doc.setDeletedAt(LocalDateTime.now());
        customerDocumentRepository.save(doc);
    }

    private CustomerDocumentResponse toDocumentResponse(CustomerDocument doc, String customerCode, List<Long> replacedIds) {
        return CustomerDocumentResponse.builder()
                .id(doc.getId())
                .customerCode(customerCode)
                .documentType(lookupHelper.resolveLookupValue(doc.getDocumentTypeLookupId()))
                .originalFileName(doc.getOriginalFileName())
                .contentType(doc.getContentType())
                .fileSizeBytes(doc.getFileSizeBytes())
                .expiryDate(doc.getExpiryDate())
                .remarks(doc.getRemarks())
                .uploadedAt(doc.getUploadedAt())
                .replacedDocumentIds(replacedIds)
                .build();
    }

    private Long resolveBranchId(CustomerSaveRequest request, Long staffUserId) {
        Long branchId = request.getBranchId();
        if (branchId == null && request.getPersonal() != null) {
            branchId = request.getPersonal().getBranchId();
        }
        if (branchId == null && staffUserId != null) {
            Optional<UserSecuritySetting> secSetting = userSecuritySettingRepository.findById(staffUserId);
            if (secSetting.isPresent() && secSetting.get().getLoginBranchId() != null) {
                branchId = secSetting.get().getLoginBranchId();
            }
        }
        if (branchId == null) {
            throw new CustomerValidationException("personal.branchId", "Branch ID is required");
        }
        if (branchRepository.findById(branchId).isEmpty()) {
            throw new CustomerValidationException("personal.branchId", "Invalid branch ID: " + branchId);
        }
        return branchId;
    }

    private String resolveLeadId(CustomerSaveRequest request) {
        if (request.getLeadId() != null && !request.getLeadId().trim().isEmpty()) {
            return request.getLeadId().trim();
        }
        if (request.getPersonal() != null && request.getPersonal().getLeadId() != null && !request.getPersonal().getLeadId().trim().isEmpty()) {
            return request.getPersonal().getLeadId().trim();
        }
        return null;
    }
}
