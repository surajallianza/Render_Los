package com.los.bank.setup.service;

import com.los.bank.dbArchitecture.DatabaseMigrationService;
import com.los.bank.setup.dto.BankDto;
import com.los.bank.setup.dto.BankSetupDataDto;
import com.los.bank.setup.dto.BankSetupRequestDto;
import com.los.bank.setup.dto.BankSetupResponseDto;
import com.los.bank.setup.dto.BranchDto;
import com.los.bank.setup.dto.InitialUserDto;
import com.los.bank.setup.entity.BankInformation;
import com.los.bank.setup.entity.DatabaseConfiguration;
import com.los.bank.setup.entity.InitialAdminSetup;
import com.los.bank.setup.entity.SetupActivityLog;
import com.los.bank.setup.entity.SystemSetup;
import com.los.bank.setup.exception.BankSetupException;
import com.los.bank.setup.repository.BankInformationRepository;
import com.los.bank.setup.repository.DatabaseConfigurationRepository;
import com.los.bank.setup.repository.InitialAdminSetupRepository;
import com.los.bank.setup.repository.SetupActivityLogRepository;
import com.los.bank.setup.repository.SystemSetupRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Orchestrator service for full bank setup flow and bank management operations:
 * 1. Validate bank and user info
 * 2. Save bank details in Master DB
 * 3. Generate unique database name
 * 4. Provision PostgreSQL bank database
 * 5. Run Flyway schema migrations against target bank database
 * 6. Insert Initial Super Admin user into migrated user tables
 * 7. Track status and log audit activities in Master DB
 * 8. Support CRUD and query operations for banks and initial users
 */
@Service
public class BankSetupService {

    private static final Logger logger = LoggerFactory.getLogger(BankSetupService.class);

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern GST_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

    private final BankInformationRepository bankInformationRepository;
    private final DatabaseConfigurationRepository databaseConfigurationRepository;
    private final SystemSetupRepository systemSetupRepository;
    private final InitialAdminSetupRepository initialAdminSetupRepository;
    private final SetupActivityLogRepository setupActivityLogRepository;

    private final DatabaseProvisioningService databaseProvisioningService;
    private final DatabaseMigrationService databaseMigrationService;
    private final InitialUserCreationService initialUserCreationService;

    public BankSetupService(
            BankInformationRepository bankInformationRepository,
            DatabaseConfigurationRepository databaseConfigurationRepository,
            SystemSetupRepository systemSetupRepository,
            InitialAdminSetupRepository initialAdminSetupRepository,
            SetupActivityLogRepository setupActivityLogRepository,
            DatabaseProvisioningService databaseProvisioningService,
            DatabaseMigrationService databaseMigrationService,
            InitialUserCreationService initialUserCreationService) {
        this.bankInformationRepository = bankInformationRepository;
        this.databaseConfigurationRepository = databaseConfigurationRepository;
        this.systemSetupRepository = systemSetupRepository;
        this.initialAdminSetupRepository = initialAdminSetupRepository;
        this.setupActivityLogRepository = setupActivityLogRepository;
        this.databaseProvisioningService = databaseProvisioningService;
        this.databaseMigrationService = databaseMigrationService;
        this.initialUserCreationService = initialUserCreationService;
    }

    /**
     * Executes the complete bank setup process.
     */
    public BankSetupResponseDto initializeBank(BankSetupRequestDto requestDto) {
        validateRequest(requestDto);

        BankDto bankDto = requestDto.getBank();
        InitialUserDto userDto = requestDto.getInitialUser();

        String bankCode = bankDto.getEffectiveBankCode();
        String bankName = bankDto.getEffectiveBankName();
        logger.info("Starting initialization process for Bank: '{}' (Code: '{}')", bankName, bankCode);

        // 1. Check duplicate bank code
        BankInformation bankInfo = bankInformationRepository.findByBankCode(bankCode).orElse(null);
        if (bankInfo != null) {
            SystemSetup existingSetup = systemSetupRepository.findByBankId(bankInfo.getId()).orElse(null);
            if (existingSetup != null && Boolean.TRUE.equals(existingSetup.getSetupCompleted())) {
                throw new BankSetupException(String.format("Bank with code '%s' already exists and setup is fully completed.", bankCode));
            }
            logger.info("Found existing incomplete setup for bank code '{}'. Resuming setup workflow.", bankCode);
            mapDtoToEntity(bankDto, bankInfo);
            bankInfo = bankInformationRepository.save(bankInfo);
        } else {
            // Save initial BankInformation with all enhanced fields
            bankInfo = new BankInformation();
            mapDtoToEntity(bankDto, bankInfo);
            bankInfo = bankInformationRepository.save(bankInfo);
            logActivity(bankInfo.getId(), "BANK_REGISTRATION", "Bank information registered in Master DB", "SUCCESS", null);
        }

        Long bankId = bankInfo.getId();

        // Ensure SystemSetup record
        SystemSetup systemSetup = systemSetupRepository.findByBankId(bankId)
                .orElseGet(() -> {
                    SystemSetup setup = SystemSetup.builder()
                            .bankId(bankId)
                            .setupStartedAt(LocalDateTime.now())
                            .setupCompleted(false)
                            .databaseCreated(false)
                            .migrationCompleted(false)
                            .superAdminCreated(false)
                            .build();
                    return systemSetupRepository.save(setup);
                });

        // 2. Determine target database name (bank_<bankcode_lowercase>)
        String dbName = "bank_" + bankCode.toLowerCase().replaceAll("[^a-z0-9_]", "");

        // 3. Save or update DatabaseConfiguration in Master DB
        DatabaseConfiguration dbConfig = databaseConfigurationRepository.findByBankId(bankId)
                .orElseGet(() -> DatabaseConfiguration.builder()
                        .bankId(bankId)
                        .databaseName(dbName)
                        .databaseHost(databaseProvisioningService.getDbHost())
                        .databasePort(databaseProvisioningService.getDbPort())
                        .databaseType("POSTGRESQL")
                        .isConnected(false)
                        .build());
        dbConfig = databaseConfigurationRepository.save(dbConfig);

        HikariDataSource dynamicBankDataSource = null;
        try {
            // 4. Provision PostgreSQL database (verify existence or create)
            logger.info("[Step 1/4] Provisioning/verifying database '{}' for Bank '{}'", dbName, bankCode);
            databaseProvisioningService.createDatabaseIfNotExists(dbName);
            systemSetup.setDatabaseCreated(true);
            dbConfig.setIsConnected(true);
            databaseConfigurationRepository.save(dbConfig);
            systemSetupRepository.save(systemSetup);
            logActivity(bankId, "DATABASE_CREATED", "Database " + dbName + " verified/created successfully", "SUCCESS", null);

            // Create dynamic DataSource for bank database
            dynamicBankDataSource = createBankDataSource(dbName);

            // 5. Run Flyway schema migrations against target bank database
            logger.info("[Step 2/4] Executing Flyway migrations against database '{}'", dbName);
            databaseMigrationService.migrateBankDatabase(dynamicBankDataSource);
            systemSetup.setMigrationCompleted(true);
            systemSetupRepository.save(systemSetup);
            logActivity(bankId, "FLYWAY_MIGRATION", "All bank schema migrations applied successfully", "SUCCESS", null);

            // 6. Ensure branch exists in b001 and create Initial User in bank database
            BranchDto createdBranch = null;
            if (!Boolean.TRUE.equals(systemSetup.getSuperAdminCreated())) {
                // Step 1: Read branch information from the setup request
                // Step 2: Ensure the branch exists in b001
                // Step 3: Retrieve its actual generated b001.id
                // Step 4: Assign that ID to InitialUserDto.loginBranch
                Long branchId = ensureBranchExists(dynamicBankDataSource, requestDto.getBranch(), bankDto, userDto);
                userDto.setLoginBranch(branchId);

                // Step 5: Continue the existing Super Admin creation workflow
                // Step 6: Ensure the existing u004 insertion logic stores the correct user ID and branch ID
                logger.info("[Step 3/4] Creating initial Super Admin user in database '{}' with login_branch: {}", dbName, branchId);
                Long userId = initialUserCreationService.createInitialUser(dynamicBankDataSource, userDto);

                InitialAdminSetup adminSetup = initialAdminSetupRepository.findByBankId(bankId)
                        .orElseGet(() -> InitialAdminSetup.builder().bankId(bankId).build());
                adminSetup.setAdminName(userDto.getEffectiveFullName());
                adminSetup.setAdminEmail(userDto.getEffectiveEmail());
                adminSetup.setUsername(userDto.getEffectiveUsername());
                adminSetup.setUserId(userId);
                adminSetup.setIsCreated(true);
                initialAdminSetupRepository.save(adminSetup);

                systemSetup.setSuperAdminCreated(true);
                systemSetupRepository.save(systemSetup);
                logActivity(bankId, "SUPER_ADMIN_CREATED", "Initial user " + userDto.getEffectiveUsername() + " created successfully", "SUCCESS", null);
            }

            // Step 7: Retrieve branch metadata if available for response
            if (userDto.getLoginBranch() != null && userDto.getLoginBranch() > 0) {
                createdBranch = getBranchById(dynamicBankDataSource, userDto.getLoginBranch());
                if (createdBranch != null) {
                    if (userDto.getBranchName() == null || userDto.getBranchName().trim().isEmpty()) {
                        userDto.setBranchName(createdBranch.getBranchName());
                    }
                    if (userDto.getBranchCode() == null || userDto.getBranchCode().trim().isEmpty()) {
                        userDto.setBranchCode(createdBranch.getBranchCode());
                    }
                }
            }

            // 7. Mark setup complete
            systemSetup.setSetupCompleted(true);
            systemSetup.setSetupCompletedAt(LocalDateTime.now());
            systemSetupRepository.save(systemSetup);
            logActivity(bankId, "SETUP_COMPLETED", "Bank initial setup completed successfully", "SUCCESS", null);

            logger.info("Bank setup for '{}' (Code: '{}') completed successfully.", bankName, bankCode);

            // Sanitize userDto for response
            InitialUserDto sanitizedUser = sanitizeUserDto(userDto);

            return BankSetupResponseDto.builder()
                    .success(true)
                    .message("Bank setup completed successfully")
                    .data(BankSetupDataDto.builder()
                            .bankId(bankId)
                            .bankName(bankInfo.getBankName())
                            .bankCode(bankInfo.getBankCode())
                            .databaseName(dbName)
                            .databaseCreated(systemSetup.getDatabaseCreated())
                            .migrationCompleted(systemSetup.getMigrationCompleted())
                            .superAdminCreated(systemSetup.getSuperAdminCreated())
                            .setupCompleted(systemSetup.getSetupCompleted())
                            .bank(toBankDto(bankInfo))
                            .branch(createdBranch)
                            .initialUser(sanitizedUser)
                            .build())
                    .build();

        } catch (Exception e) {
            String errorMsg = "Bank setup failed for bank code " + bankCode + ": " + e.getMessage();
            logger.error(errorMsg, e);
            logActivity(bankId, "SETUP_ERROR", "Setup halted due to error", "FAILED", e.getMessage());
            throw new BankSetupException(errorMsg, e);
        } finally {
            if (dynamicBankDataSource != null && !dynamicBankDataSource.isClosed()) {
                try {
                    dynamicBankDataSource.close();
                    logger.debug("Closed dynamic Hikari connection pool for '{}'", dbName);
                } catch (Exception ex) {
                    logger.warn("Failed to close dynamic HikariDataSource for {}: {}", dbName, ex.getMessage());
                }
            }
        }
    }

    /**
     * Retrieves all registered banks, enriched with branch details from their bank databases.
     */
    public List<BankDto> getAllBanks() {
        return bankInformationRepository.findAll().stream()
                .map(entity -> {
                    BankDto dto = toBankDto(entity);
                    enrichWithBranchData(dto, entity.getId());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single bank by ID, enriched with branch details.
     */
    public BankDto getBankById(Long id) {
        BankInformation bankInfo = bankInformationRepository.findById(id)
                .orElseThrow(() -> new BankSetupException("Bank not found with ID: " + id));
        BankDto dto = toBankDto(bankInfo);
        enrichWithBranchData(dto, bankInfo.getId());
        return dto;
    }

    /**
     * Retrieves a single bank by Bank Code, enriched with branch details.
     */
    public BankDto getBankByCode(String bankCode) {
        BankInformation bankInfo = bankInformationRepository.findByBankCode(bankCode)
                .orElseThrow(() -> new BankSetupException("Bank not found with code: " + bankCode));
        BankDto dto = toBankDto(bankInfo);
        enrichWithBranchData(dto, bankInfo.getId());
        return dto;
    }

    /**
     * Updates an existing bank record without overwriting omitted optional fields.
     */
    public BankDto updateBank(Long id, BankDto updateDto) {
        BankInformation bankInfo = bankInformationRepository.findById(id)
                .orElseThrow(() -> new BankSetupException("Bank not found with ID: " + id));

        validateOptionalFields(updateDto);

        if (updateDto.getBankName() != null && !updateDto.getBankName().trim().isEmpty()) {
            bankInfo.setBankName(updateDto.getBankName().trim());
        }
        if (updateDto.getLegalName() != null) {
            bankInfo.setLegalName(updateDto.getLegalName().trim());
        }
        if (updateDto.getBankType() != null) {
            bankInfo.setBankType(updateDto.getBankType().trim());
        }
        if (updateDto.getContactEmail() != null) {
            bankInfo.setBankEmail(updateDto.getContactEmail().trim());
        }
        if (updateDto.getContactPhone() != null) {
            bankInfo.setContactNumber(updateDto.getContactPhone().trim());
        }
        if (updateDto.getPan() != null) {
            bankInfo.setPan(updateDto.getPan().trim().toUpperCase());
        }
        if (updateDto.getGstNo() != null) {
            bankInfo.setGstNo(updateDto.getGstNo().trim().toUpperCase());
        }
        if (updateDto.getLicenseNo() != null) {
            bankInfo.setLicenseNo(updateDto.getLicenseNo().trim());
        }
        if (updateDto.getWebsite() != null) {
            bankInfo.setWebsite(updateDto.getWebsite().trim());
        }
        if (updateDto.getLogoUrl() != null) {
            bankInfo.setLogoUrl(updateDto.getLogoUrl().trim());
        }
        if (updateDto.getStatus() != null) {
            bankInfo.setStatus(updateDto.getStatus().trim());
        }
        if (updateDto.getRegulatoryStatus() != null) {
            bankInfo.setRegulatoryStatus(updateDto.getRegulatoryStatus().trim());
        }
        if (updateDto.getDirectClgMember() != null) {
            bankInfo.setDirectClgMember(updateDto.getDirectClgMember());
        }
        if (updateDto.getDirectMemberIftas() != null) {
            bankInfo.setDirectMemberIftas(updateDto.getDirectMemberIftas());
        }
        if (updateDto.getMicr1() != null) {
            bankInfo.setMicr1(updateDto.getMicr1().trim());
        }
        if (updateDto.getMicr2() != null) {
            bankInfo.setMicr2(updateDto.getMicr2().trim());
        }
        if (updateDto.getMicr3() != null) {
            bankInfo.setMicr3(updateDto.getMicr3().trim());
        }
        if (updateDto.getMicrNumber() != null) {
            bankInfo.setMicrNumber(updateDto.getMicrNumber().trim());
        }
        if (updateDto.getIfscCode() != null) {
            bankInfo.setIfscCode(updateDto.getIfscCode().trim().toUpperCase());
        }
        if (updateDto.getNoOfBranches() != null) {
            bankInfo.setNoOfBranches(updateDto.getNoOfBranches());
        }
        if (updateDto.getSponsorBankClg() != null) {
            bankInfo.setSponsorBankClg(updateDto.getSponsorBankClg().trim());
        }
        if (updateDto.getSponsorBankIftas() != null) {
            bankInfo.setSponsorBankIftas(updateDto.getSponsorBankIftas().trim());
        }
        if (updateDto.getAddressType() != null) {
            bankInfo.setAddressType(updateDto.getAddressType().trim());
        }
        if (updateDto.getUnitGala() != null) {
            bankInfo.setUnitGala(updateDto.getUnitGala().trim());
        }
        if (updateDto.getStreetRoad() != null) {
            bankInfo.setStreetRoad(updateDto.getStreetRoad().trim());
        }
        if (updateDto.getLandmark() != null) {
            bankInfo.setLandmark(updateDto.getLandmark().trim());
        }
        if (updateDto.getCity() != null) {
            bankInfo.setCity(updateDto.getCity().trim());
        }
        if (updateDto.getState() != null) {
            bankInfo.setState(updateDto.getState().trim());
        }
        if (updateDto.getCountry() != null) {
            bankInfo.setCountry(updateDto.getCountry().trim());
        }
        if (updateDto.getPincode() != null) {
            bankInfo.setPincode(updateDto.getPincode().trim());
        }
        if (updateDto.getAddress() != null) {
            bankInfo.setAddress(updateDto.getAddress().trim());
        }

        BankInformation saved = bankInformationRepository.save(bankInfo);
        logActivity(saved.getId(), "BANK_UPDATED", "Bank registration details updated", "SUCCESS", null);
        return toBankDto(saved);
    }

    /**
     * Retrieves the initial administrator user details for a bank.
     */
    public InitialUserDto getBankInitialUser(Long bankId) {
        InitialAdminSetup adminSetup = initialAdminSetupRepository.findByBankId(bankId)
                .orElseThrow(() -> new BankSetupException("Initial admin setup record not found for bank ID: " + bankId));

        DatabaseConfiguration dbConfig = databaseConfigurationRepository.findByBankId(bankId)
                .orElseThrow(() -> new BankSetupException("Database configuration not found for bank ID: " + bankId));

        HikariDataSource bankDataSource = null;
        try {
            bankDataSource = createBankDataSource(dbConfig.getDatabaseName());
            InitialUserDto user = initialUserCreationService.getInitialUser(bankDataSource, adminSetup.getUserId());
            if (user == null) {
                // Fallback to metadata in Master DB
                return InitialUserDto.builder()
                        .username(adminSetup.getUsername())
                        .email(adminSetup.getAdminEmail())
                        .firstName(adminSetup.getAdminName())
                        .status("ACTIVE")
                        .userRole("SUPER_ADMIN")
                        .systemRole("1")
                        .build();
            }
            return user;
        } finally {
            if (bankDataSource != null && !bankDataSource.isClosed()) {
                bankDataSource.close();
            }
        }
    }

    /**
     * Maps BankDto properties to BankInformation entity.
     */
    public void mapDtoToEntity(BankDto dto, BankInformation entity) {
        entity.setBankName(dto.getEffectiveBankName());
        entity.setBankCode(dto.getEffectiveBankCode());
        entity.setBankEmail(dto.getEffectiveEmail());
        entity.setContactNumber(dto.getEffectiveContactNumber());
        entity.setAddress(dto.getEffectiveAddress());

        entity.setLegalName(dto.getLegalName());
        entity.setBankType(dto.getBankType());
        entity.setPan(dto.getPan() != null ? dto.getPan().trim().toUpperCase() : null);
        entity.setGstNo(dto.getGstNo() != null ? dto.getGstNo().trim().toUpperCase() : null);
        entity.setLicenseNo(dto.getLicenseNo());
        entity.setWebsite(dto.getWebsite() != null ? dto.getWebsite() : "");
        entity.setLogoUrl(dto.getLogoUrl() != null ? dto.getLogoUrl() : "");
        entity.setStatus(dto.getStatus() != null && !dto.getStatus().trim().isEmpty() ? dto.getStatus().trim() : "ACTIVE");
        entity.setRegulatoryStatus(dto.getRegulatoryStatus() != null && !dto.getRegulatoryStatus().trim().isEmpty() ? dto.getRegulatoryStatus().trim() : "ACTIVE");
        entity.setDirectClgMember(Boolean.TRUE.equals(dto.getDirectClgMember()));
        entity.setDirectMemberIftas(Boolean.TRUE.equals(dto.getDirectMemberIftas()));

        entity.setMicr1(dto.getMicr1());
        entity.setMicr2(dto.getMicr2());
        entity.setMicr3(dto.getMicr3());
        entity.setMicrNumber(dto.getMicrNumber());
        entity.setIfscCode(dto.getIfscCode() != null ? dto.getIfscCode().trim().toUpperCase() : null);
        entity.setNoOfBranches(dto.getNoOfBranches());

        entity.setSponsorBankClg(dto.getSponsorBankClg() != null ? dto.getSponsorBankClg() : "");
        entity.setSponsorBankIftas(dto.getSponsorBankIftas() != null ? dto.getSponsorBankIftas() : "");

        entity.setAddressType(dto.getAddressType() != null && !dto.getAddressType().trim().isEmpty() ? dto.getAddressType().trim() : "Registered Office");
        entity.setUnitGala(dto.getUnitGala() != null ? dto.getUnitGala() : "");
        entity.setStreetRoad(dto.getStreetRoad() != null ? dto.getStreetRoad() : "");
        entity.setLandmark(dto.getLandmark() != null ? dto.getLandmark() : "");
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setCountry(dto.getCountry() != null && !dto.getCountry().trim().isEmpty() ? dto.getCountry().trim() : "India");
        entity.setPincode(dto.getPincode());
    }

    /**
     * Converts BankInformation entity into BankDto matching the final JSON structure.
     */
    public BankDto toBankDto(BankInformation entity) {
        if (entity == null) {
            return null;
        }
        return BankDto.builder()
                .name(entity.getBankName())
                .bankCode(entity.getBankCode())
                .bankName(entity.getBankName())
                .legalName(entity.getLegalName() != null ? entity.getLegalName() : entity.getBankName())
                .bankType(entity.getBankType())
                .contactEmail(entity.getBankEmail())
                .contactPhone(entity.getContactNumber())
                .pan(entity.getPan())
                .gstNo(entity.getGstNo())
                .licenseNo(entity.getLicenseNo())
                .website(entity.getWebsite() != null ? entity.getWebsite() : "")
                .logoUrl(entity.getLogoUrl() != null ? entity.getLogoUrl() : "")
                .status(entity.getStatus() != null ? entity.getStatus() : "ACTIVE")
                .regulatoryStatus(entity.getRegulatoryStatus() != null ? entity.getRegulatoryStatus() : "ACTIVE")
                .directClgMember(Boolean.TRUE.equals(entity.getDirectClgMember()))
                .directMemberIftas(Boolean.TRUE.equals(entity.getDirectMemberIftas()))
                .micr1(entity.getMicr1())
                .micr2(entity.getMicr2())
                .micr3(entity.getMicr3())
                .micrNumber(entity.getMicrNumber())
                .ifscCode(entity.getIfscCode())
                .noOfBranches(entity.getNoOfBranches())
                .sponsorBankClg(entity.getSponsorBankClg() != null ? entity.getSponsorBankClg() : "")
                .sponsorBankIftas(entity.getSponsorBankIftas() != null ? entity.getSponsorBankIftas() : "")
                .addressType(entity.getAddressType() != null ? entity.getAddressType() : "Registered Office")
                .unitGala(entity.getUnitGala() != null ? entity.getUnitGala() : "")
                .streetRoad(entity.getStreetRoad() != null ? entity.getStreetRoad() : "")
                .landmark(entity.getLandmark() != null ? entity.getLandmark() : "")
                .city(entity.getCity())
                .state(entity.getState())
                .country(entity.getCountry() != null ? entity.getCountry() : "India")
                .pincode(entity.getPincode())
                .address(entity.getAddress())
                .build();
    }

    private void validateRequest(BankSetupRequestDto requestDto) {
        if (requestDto == null) {
            throw new IllegalArgumentException("Setup request body cannot be null.");
        }
        if (requestDto.getBank() == null) {
            throw new IllegalArgumentException("Bank information ('bank') is required.");
        }
        if (requestDto.getBank().getEffectiveBankCode() == null) {
            throw new IllegalArgumentException("Bank code ('bank_code') is required.");
        }
        if (requestDto.getInitialUser() == null) {
            throw new IllegalArgumentException("Initial user information ('initial_user') is required.");
        }
        if (requestDto.getInitialUser().getUsername() == null || requestDto.getInitialUser().getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Initial user username is required.");
        }
        validateOptionalFields(requestDto.getBank());
    }

    private void validateOptionalFields(BankDto bankDto) {
        if (bankDto == null) return;
        if (bankDto.getPan() != null && !bankDto.getPan().trim().isEmpty()) {
            String pan = bankDto.getPan().trim().toUpperCase();
            if (!PAN_PATTERN.matcher(pan).matches()) {
                throw new IllegalArgumentException("Invalid PAN format: '" + bankDto.getPan() + "'. Expected format: 5 letters, 4 digits, 1 letter (e.g. ABCDE1234F).");
            }
        }
        if (bankDto.getGstNo() != null && !bankDto.getGstNo().trim().isEmpty()) {
            String gst = bankDto.getGstNo().trim().toUpperCase();
            if (!GST_PATTERN.matcher(gst).matches()) {
                throw new IllegalArgumentException("Invalid GST format: '" + bankDto.getGstNo() + "'. Expected format: 15-character alphanumeric GSTIN.");
            }
        }
        if (bankDto.getIfscCode() != null && !bankDto.getIfscCode().trim().isEmpty()) {
            String ifsc = bankDto.getIfscCode().trim().toUpperCase();
            if (!IFSC_PATTERN.matcher(ifsc).matches()) {
                throw new IllegalArgumentException("Invalid IFSC code format: '" + bankDto.getIfscCode() + "'. Expected format: 4 letters, 0, 6 alphanumeric (e.g. SBIN0001234).");
            }
        }
    }

    private InitialUserDto sanitizeUserDto(InitialUserDto userDto) {
        if (userDto == null) return null;
        return InitialUserDto.builder()
                .empId(userDto.getEffectiveEmpNo())
                .username(userDto.getEffectiveUsername())
                .title(userDto.getTitle() != null ? userDto.getTitle() : "")
                .firstName(userDto.getEffectiveFirstName())
                .middleName(userDto.getMiddleName() != null ? userDto.getMiddleName() : "")
                .lastName(userDto.getEffectiveLastName())
                .dob(userDto.getDob())
                .email(userDto.getEffectiveEmail())
                .mobile(userDto.getMobile())
                .gender(userDto.getGender() != null ? userDto.getGender() : "MALE")
                .designation(userDto.getDesignation() != null ? userDto.getDesignation() : "Chief Technology Officer")
                .userRole(userDto.getEffectiveUserRole())
                .systemRole(userDto.getSystemRole() != null ? userDto.getSystemRole() : "1")
                .status(userDto.getStatus() != null ? userDto.getStatus() : "ACTIVE")
                .loginBranch(userDto.getLoginBranch() != null ? userDto.getLoginBranch() : Long.valueOf(0L))
                .branchName(userDto.getBranchName())
                .branchCode(userDto.getBranchCode())
                .loginTime(userDto.getLoginTime() != null ? userDto.getLoginTime() : "")
                .logoutTime(userDto.getLogoutTime() != null ? userDto.getLogoutTime() : "")
                .sessionTimeout(userDto.getSessionTimeout() != null ? userDto.getSessionTimeout() : Integer.valueOf(30))
                .noOfBadLogins(userDto.getNoOfBadLogins() != null ? userDto.getNoOfBadLogins() : Integer.valueOf(0))
                .build();
    }

    private HikariDataSource createBankDataSource(String databaseName) {
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s",
                databaseProvisioningService.getDbHost(),
                databaseProvisioningService.getDbPort(),
                databaseName);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(databaseProvisioningService.getDbUsername());
        config.setPassword(databaseProvisioningService.getDbPassword());
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(120000);
        config.setPoolName("DynamicPool-" + databaseName);

        return new HikariDataSource(config);
    }

    private void logActivity(Long bankId, String activity, String description, String status, String errorMessage) {
        try {
            SetupActivityLog log = SetupActivityLog.builder()
                    .bankId(bankId)
                    .activity(activity)
                    .description(description)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();
            setupActivityLogRepository.save(log);
        } catch (Exception ex) {
            logger.warn("Failed to write setup activity log: {}", ex.getMessage());
        }
    }

    /**
     * Ensures the initial branch exists in b001 within the target bank database.
     * 1. Reads branch details from setup request (or defaults from bank registration).
     * 2. Checks if branch already exists by ID or branch code.
     * 3. Inserts into b001 if absent and returns the generated/existing b001.id.
     */
    public Long ensureBranchExists(
            DataSource bankDataSource,
            BranchDto branchDto,
            BankDto bankDto,
            InitialUserDto userDto) {

        String branchName = "Main Branch";
        if (branchDto != null && branchDto.getBranchName() != null && !branchDto.getBranchName().trim().isEmpty()) {
            branchName = branchDto.getBranchName().trim();
        } else if (userDto != null && userDto.getBranchName() != null && !userDto.getBranchName().trim().isEmpty()) {
            branchName = userDto.getBranchName().trim();
        } else if (bankDto != null && bankDto.getBranchName() != null && !bankDto.getBranchName().trim().isEmpty()) {
            branchName = bankDto.getBranchName().trim();
        } else if (bankDto != null && bankDto.getEffectiveBankName() != null && !bankDto.getEffectiveBankName().trim().isEmpty()) {
            branchName = bankDto.getEffectiveBankName().trim() + " Main Branch";
        }
        if (branchName.length() > 150) {
            branchName = branchName.substring(0, 150);
        }

        String branchCode = "BR001";
        if (branchDto != null && branchDto.getBranchCode() != null && !branchDto.getBranchCode().trim().isEmpty()) {
            branchCode = branchDto.getBranchCode().trim().toUpperCase();
        } else if (userDto != null && userDto.getBranchCode() != null && !userDto.getBranchCode().trim().isEmpty()) {
            branchCode = userDto.getBranchCode().trim().toUpperCase();
        } else if (bankDto != null && bankDto.getBranchCode() != null && !bankDto.getBranchCode().trim().isEmpty()) {
            branchCode = bankDto.getBranchCode().trim().toUpperCase();
        } else if (bankDto != null && bankDto.getEffectiveBankCode() != null && !bankDto.getEffectiveBankCode().trim().isEmpty()) {
            String bCode = bankDto.getEffectiveBankCode().trim().toUpperCase();
            branchCode = bCode.length() <= 16 ? bCode + "001" : bCode.substring(0, 20);
        }
        if (branchCode.length() > 20) {
            branchCode = branchCode.substring(0, 20);
        }

        String address = (branchDto != null && branchDto.getAddress() != null && !branchDto.getAddress().trim().isEmpty())
                ? branchDto.getAddress().trim()
                : (bankDto != null ? bankDto.getEffectiveAddress() : null);
        if (address != null && address.length() > 255) {
            address = address.substring(0, 255);
        }

        String city = (branchDto != null && branchDto.getCity() != null && !branchDto.getCity().trim().isEmpty())
                ? branchDto.getCity().trim()
                : (bankDto != null ? bankDto.getCity() : null);
        if (city != null && city.length() > 100) {
            city = city.substring(0, 100);
        }

        String state = (branchDto != null && branchDto.getState() != null && !branchDto.getState().trim().isEmpty())
                ? branchDto.getState().trim()
                : (bankDto != null ? bankDto.getState() : null);
        if (state != null && state.length() > 100) {
            state = state.substring(0, 100);
        }

        String pincode = (branchDto != null && branchDto.getPincode() != null && !branchDto.getPincode().trim().isEmpty())
                ? branchDto.getPincode().trim()
                : (bankDto != null ? bankDto.getPincode() : null);
        if (pincode != null && pincode.length() > 10) {
            pincode = pincode.substring(0, 10);
        }

        String status = (branchDto != null && branchDto.getStatus() != null && !branchDto.getStatus().trim().isEmpty())
                ? branchDto.getStatus().trim()
                : (bankDto != null && bankDto.getStatus() != null && !bankDto.getStatus().trim().isEmpty() ? bankDto.getStatus().trim() : "ACTIVE");
        if (status.length() > 20) {
            status = status.substring(0, 20);
        }

        try (Connection conn = bankDataSource.getConnection()) {
            // 1. Check if userDto specifies an existing branch ID
            if (userDto != null && userDto.getLoginBranch() != null && userDto.getLoginBranch() > 0) {
                String checkById = "SELECT id FROM b001 WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkById)) {
                    stmt.setLong(1, userDto.getLoginBranch());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong("id");
                        }
                    }
                }
            }

            // 2. Check if branchDto specifies an existing branch ID
            if (branchDto != null && branchDto.getId() != null && branchDto.getId() > 0) {
                String checkById = "SELECT id FROM b001 WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkById)) {
                    stmt.setLong(1, branchDto.getId());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong("id");
                        }
                    }
                }
            }

            // 3. Check if a branch with this code already exists
            String checkByCode = "SELECT id FROM b001 WHERE UPPER(code) = UPPER(?)";
            try (PreparedStatement stmt = conn.prepareStatement(checkByCode)) {
                stmt.setString(1, branchCode);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("id");
                    }
                }
            }

            // 4. Insert into b001
            String insertSql = "INSERT INTO b001 (name, code, address, city, state, pincode, status, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, branchName);
                stmt.setString(2, branchCode);
                stmt.setString(3, address);
                stmt.setString(4, city);
                stmt.setString(5, state);
                stmt.setString(6, pincode);
                stmt.setString(7, status);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long genId = rs.getLong(1);
                        logger.info("Inserted new branch into b001 with ID: {}, Code: '{}'", genId, branchCode);
                        return genId;
                    }
                }
            } catch (SQLException ex) {
                logger.warn("Insert into b001 encountered duplicate or error: {}. Querying existing record.", ex.getMessage());
                try (PreparedStatement stmt = conn.prepareStatement(checkByCode)) {
                    stmt.setString(1, branchCode);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong("id");
                        }
                    }
                }
            }

            // 5. Fallback: check if any branch exists in b001
            String queryAny = "SELECT id FROM b001 ORDER BY id ASC LIMIT 1";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(queryAny)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

            throw new BankSetupException("Unable to resolve or create branch in b001.");
        } catch (SQLException e) {
            throw new BankSetupException("Database error verifying/inserting branch in b001: " + e.getMessage(), e);
        }
    }

    /**
     * Enriches a BankDto with branch_name and branch_code by querying the
     * first branch (order by id) from table b001 in the bank's own database.
     * If the bank database or branch table is not yet provisioned, the fields
     * are left null gracefully.
     */
    private void enrichWithBranchData(BankDto dto, Long bankId) {
        if (dto == null || bankId == null) return;

        DatabaseConfiguration dbConfig = databaseConfigurationRepository.findByBankId(bankId).orElse(null);
        if (dbConfig == null || dbConfig.getDatabaseName() == null || !Boolean.TRUE.equals(dbConfig.getIsConnected())) {
            // Bank database not yet provisioned — leave branch fields null
            return;
        }

        HikariDataSource bankDs = null;
        try {
            bankDs = createBankDataSource(dbConfig.getDatabaseName());
            String sql = "SELECT name, code FROM b001 ORDER BY id ASC LIMIT 1";
            try (Connection conn = bankDs.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    dto.setBranchName(rs.getString("name"));
                    dto.setBranchCode(rs.getString("code"));
                }
            }
        } catch (Exception e) {
            logger.warn("Could not fetch branch data for bank ID {}: {}", bankId, e.getMessage());
        } finally {
            if (bankDs != null && !bankDs.isClosed()) {
                try { bankDs.close(); } catch (Exception ignored) {}
            }
        }
    }

    private BranchDto getBranchById(DataSource bankDataSource, Long branchId) {
        if (branchId == null || branchId <= 0) {
            return null;
        }
        String sql = "SELECT id, name, code, address, city, state, pincode, status FROM b001 WHERE id = ?";
        try (Connection conn = bankDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return BranchDto.builder()
                            .id(rs.getLong("id"))
                            .branchName(rs.getString("name"))
                            .branchCode(rs.getString("code"))
                            .address(rs.getString("address"))
                            .city(rs.getString("city"))
                            .state(rs.getString("state"))
                            .pincode(rs.getString("pincode"))
                            .status(rs.getString("status"))
                            .build();
                }
            }
        } catch (SQLException e) {
            logger.warn("Could not retrieve branch details for branch ID {}: {}", branchId, e.getMessage());
        }
        return null;
    }
}
