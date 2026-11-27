package com.los.bank.setup.service;

import com.los.bank.branch.exception.BranchException;
import com.los.bank.setup.dto.OrganizationAccountDto;
import com.los.bank.setup.dto.OrganizationDto;
import com.los.bank.setup.entity.BankInformation;
import com.los.bank.setup.repository.BankInformationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for Organization / Bank Master administration.
 * Provides loading and updating of bank details and associated bank accounts
 * in the Master Database (bank_setup).
 */
@Service
public class OrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationService.class);

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    private static final Pattern GST_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern MICR_PATTERN = Pattern.compile("^[0-9]{9}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^[0-9]{6}$");

    private final BankInformationRepository bankInformationRepository;
    private final DataSource setupDataSource;

    public OrganizationService(
            BankInformationRepository bankInformationRepository,
            @Qualifier("setupDataSource") DataSource setupDataSource) {
        this.bankInformationRepository = bankInformationRepository;
        this.setupDataSource = setupDataSource;
    }

    /**
     * Loads organization / bank details by organization ID or Bank Code.
     */
    public OrganizationDto getOrganization(String organizationIdStr) {
        BankInformation bank = findBankEntity(organizationIdStr);
        if (bank == null) {
            throw new BranchException("Organization not found with identifier: " + organizationIdStr, HttpStatus.NOT_FOUND);
        }

        OrganizationDto dto = mapEntityToDto(bank);

        // Load extended limits and accounts from setup database if tables/columns exist
        loadExtendedDetailsAndAccounts(bank.getId(), dto);

        return dto;
    }

    /**
     * Updates organization / bank details.
     */
    public OrganizationDto updateOrganization(String organizationIdStr, OrganizationDto updateDto) {
        if (updateDto == null) {
            throw new BranchException("Request body cannot be null.", HttpStatus.BAD_REQUEST);
        }

        BankInformation bank = findBankEntity(organizationIdStr);
        if (bank == null) {
            throw new BranchException("Organization not found with identifier: " + organizationIdStr, HttpStatus.NOT_FOUND);
        }

        validateUpdateRequest(updateDto);

        // 1. Update basic entity fields
        if (updateDto.getBankName() != null && !updateDto.getBankName().trim().isEmpty()) {
            bank.setBankName(updateDto.getBankName().trim());
        }
        if (updateDto.getBankCode() != null && !updateDto.getBankCode().trim().isEmpty()) {
            bank.setBankCode(updateDto.getBankCode().trim().toUpperCase());
        }
        if (updateDto.getBankType() != null && !updateDto.getBankType().trim().isEmpty()) {
            bank.setBankType(updateDto.getBankType().trim());
        }
        if (updateDto.getRegistrationLicenseNumber() != null && !updateDto.getRegistrationLicenseNumber().trim().isEmpty()) {
            bank.setLicenseNo(updateDto.getRegistrationLicenseNumber().trim());
        }
        if (updateDto.getPan() != null && !updateDto.getPan().trim().isEmpty()) {
            bank.setPan(updateDto.getPan().trim().toUpperCase());
        }
        if (updateDto.getGstNumber() != null && !updateDto.getGstNumber().trim().isEmpty()) {
            bank.setGstNo(updateDto.getGstNumber().trim().toUpperCase());
        }
        if (updateDto.getNumberOfBranches() != null && !updateDto.getNumberOfBranches().trim().isEmpty()) {
            try {
                bank.setNoOfBranches(Integer.parseInt(updateDto.getNumberOfBranches().trim()));
            } catch (NumberFormatException ignored) {}
        }
        if (updateDto.getAddressType() != null && !updateDto.getAddressType().trim().isEmpty()) {
            bank.setAddressType(updateDto.getAddressType().trim());
        }
        if (updateDto.getAddress() != null && !updateDto.getAddress().trim().isEmpty()) {
            bank.setAddress(updateDto.getAddress().trim());
        }
        if (updateDto.getAddressLine2() != null && !updateDto.getAddressLine2().trim().isEmpty()) {
            bank.setStreetRoad(updateDto.getAddressLine2().trim());
        }
        if (updateDto.getLandmark() != null && !updateDto.getLandmark().trim().isEmpty()) {
            bank.setLandmark(updateDto.getLandmark().trim());
        }
        if (updateDto.getCity() != null && !updateDto.getCity().trim().isEmpty()) {
            bank.setCity(updateDto.getCity().trim());
        }
        if (updateDto.getState() != null && !updateDto.getState().trim().isEmpty()) {
            bank.setState(updateDto.getState().trim());
        }
        if (updateDto.getPincode() != null && !updateDto.getPincode().trim().isEmpty()) {
            bank.setPincode(updateDto.getPincode().trim());
        }

        // If accounts are provided, also update top-level IFSC and MICR on bank_information for compatibility
        if (updateDto.getAccounts() != null && !updateDto.getAccounts().isEmpty()) {
            OrganizationAccountDto firstAcc = updateDto.getAccounts().get(0);
            if (firstAcc.getIfscCode() != null && !firstAcc.getIfscCode().trim().isEmpty()) {
                bank.setIfscCode(firstAcc.getIfscCode().trim().toUpperCase());
            }
            if (firstAcc.getMicrCode() != null && !firstAcc.getMicrCode().trim().isEmpty()) {
                bank.setMicrNumber(firstAcc.getMicrCode().trim());
            }
        }

        BankInformation saved = bankInformationRepository.save(bank);
        logger.info("Updated BankInformation entity with ID: {}", saved.getId());

        // 2. Persist extended columns and accounts into master database if tables exist
        saveExtendedDetailsAndAccounts(saved.getId(), updateDto);

        // 3. Return updated DTO
        return getOrganization(saved.getId().toString());
    }

    private BankInformation findBankEntity(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        String trimmed = identifier.trim();

        // Try numeric ID
        try {
            Long id = Long.parseLong(trimmed);
            BankInformation byId = bankInformationRepository.findById(id).orElse(null);
            if (byId != null) {
                return byId;
            }
        } catch (NumberFormatException ignored) {}

        // Try bank_code
        return bankInformationRepository.findByBankCode(trimmed.toUpperCase())
                .or(() -> bankInformationRepository.findByBankNameIgnoreCase(trimmed))
                .orElse(null);
    }

    private OrganizationDto mapEntityToDto(BankInformation bank) {
        return OrganizationDto.builder()
                .id(bank.getId())
                .bankCode(bank.getBankCode())
                .bankName(bank.getBankName())
                .bankType(bank.getBankType())
                .registrationLicenseNumber(bank.getLicenseNo())
                .pan(bank.getPan())
                .gstNumber(bank.getGstNo())
                .numberOfBranches(bank.getNoOfBranches() != null ? bank.getNoOfBranches().toString() : "0")
                .addressType(bank.getAddressType() != null ? bank.getAddressType() : "Registered Office")
                .address(bank.getAddress() != null ? bank.getAddress() : "")
                .addressLine2(bank.getStreetRoad() != null ? bank.getStreetRoad() : "")
                .landmark(bank.getLandmark() != null ? bank.getLandmark() : "")
                .city(bank.getCity() != null ? bank.getCity() : "")
                .state(bank.getState() != null ? bank.getState() : "")
                .pincode(bank.getPincode() != null ? bank.getPincode() : "")
                .singlePartyLimit("0")
                .groupExposureLimit("0")
                .sanctionMargin("0")
                .penaltyCharge("0")
                .penaltyRate("0")
                .loanApprovalAuthority("Branch Manager")
                .accounts(new ArrayList<>())
                .build();
    }

    private void loadExtendedDetailsAndAccounts(Long bankId, OrganizationDto dto) {
        try (Connection conn = setupDataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            // 1. Check if extended columns exist on bank_information
            boolean hasSingleParty = doesColumnExist(meta, "bank_information", "single_party_limit");
            if (hasSingleParty) {
                String selectLimitsSql = "SELECT single_party_limit, group_exposure_limit, sanction_margin, " +
                        "penalty_charge, penalty_rate, loan_approval_authority, address_line2 " +
                        "FROM bank_information WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(selectLimitsSql)) {
                    stmt.setLong(1, bankId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            BigDecimal spl = rs.getBigDecimal("single_party_limit");
                            BigDecimal gel = rs.getBigDecimal("group_exposure_limit");
                            BigDecimal sm = rs.getBigDecimal("sanction_margin");
                            BigDecimal pc = rs.getBigDecimal("penalty_charge");
                            BigDecimal pr = rs.getBigDecimal("penalty_rate");
                            String laa = rs.getString("loan_approval_authority");
                            String al2 = rs.getString("address_line2");

                            if (spl != null) dto.setSinglePartyLimit(spl.toPlainString());
                            if (gel != null) dto.setGroupExposureLimit(gel.toPlainString());
                            if (sm != null) dto.setSanctionMargin(sm.toPlainString());
                            if (pc != null) dto.setPenaltyCharge(pc.toPlainString());
                            if (pr != null) dto.setPenaltyRate(pr.toPlainString());
                            if (laa != null) dto.setLoanApprovalAuthority(laa);
                            if (al2 != null && !al2.trim().isEmpty()) dto.setAddressLine2(al2);
                        }
                    }
                } catch (SQLException ex) {
                    logger.debug("Extended columns not loaded: {}", ex.getMessage());
                }
            }

            // 2. Check if bank_accounts table exists
            boolean hasAccountsTable = doesTableExist(meta, "bank_accounts");
            List<OrganizationAccountDto> accounts = new ArrayList<>();
            if (hasAccountsTable) {
                String selectAccountsSql = "SELECT particulars, bank_name, account_type, account_number, micr_code, ifsc_code " +
                        "FROM bank_accounts WHERE bank_id = ? ORDER BY id ASC";
                try (PreparedStatement stmt = conn.prepareStatement(selectAccountsSql)) {
                    stmt.setLong(1, bankId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            accounts.add(OrganizationAccountDto.builder()
                                    .particulars(rs.getString("particulars"))
                                    .bankName(rs.getString("bank_name"))
                                    .accountType(rs.getString("account_type"))
                                    .accountNumber(rs.getString("account_number"))
                                    .micrCode(rs.getString("micr_code"))
                                    .ifscCode(rs.getString("ifsc_code"))
                                    .build());
                        }
                    }
                } catch (SQLException ex) {
                    logger.debug("Accounts table not queried: {}", ex.getMessage());
                }
            }

            // Fallback: If accounts collection is empty, synthesize primary account from bank_information if present
            if (accounts.isEmpty()) {
                BankInformation bank = bankInformationRepository.findById(bankId).orElse(null);
                if (bank != null && ((bank.getIfscCode() != null && !bank.getIfscCode().trim().isEmpty())
                        || (bank.getMicrNumber() != null && !bank.getMicrNumber().trim().isEmpty()))) {
                    accounts.add(OrganizationAccountDto.builder()
                            .particulars("Primary Collection Account")
                            .bankName(bank.getBankName())
                            .accountType("Current")
                            .accountNumber("N/A")
                            .micrCode(bank.getMicrNumber() != null ? bank.getMicrNumber() : "")
                            .ifscCode(bank.getIfscCode() != null ? bank.getIfscCode() : "")
                            .build());
                }
            }

            dto.setAccounts(accounts);

        } catch (SQLException e) {
            logger.warn("Could not load extended details or accounts: {}", e.getMessage());
        }
    }

    private void saveExtendedDetailsAndAccounts(Long bankId, OrganizationDto dto) {
        try (Connection conn = setupDataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            // 1. Update extended columns if present in table
            if (doesColumnExist(meta, "bank_information", "single_party_limit")) {
                String updateExtendedSql = "UPDATE bank_information SET " +
                        "single_party_limit = ?, group_exposure_limit = ?, sanction_margin = ?, " +
                        "penalty_charge = ?, penalty_rate = ?, loan_approval_authority = ?, address_line2 = ? " +
                        "WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateExtendedSql)) {
                    stmt.setBigDecimal(1, parseBigDecimal(dto.getSinglePartyLimit()));
                    stmt.setBigDecimal(2, parseBigDecimal(dto.getGroupExposureLimit()));
                    stmt.setBigDecimal(3, parseBigDecimal(dto.getSanctionMargin()));
                    stmt.setBigDecimal(4, parseBigDecimal(dto.getPenaltyCharge()));
                    stmt.setBigDecimal(5, parseBigDecimal(dto.getPenaltyRate()));
                    stmt.setString(6, dto.getLoanApprovalAuthority());
                    stmt.setString(7, dto.getAddressLine2());
                    stmt.setLong(8, bankId);
                    stmt.executeUpdate();
                } catch (SQLException ex) {
                    logger.warn("Could not update extended limit columns: {}", ex.getMessage());
                }
            }

            // 2. Update bank_accounts table if present
            if (doesTableExist(meta, "bank_accounts") && dto.getAccounts() != null) {
                // Delete previous accounts for this bank to handle sync
                String deleteSql = "DELETE FROM bank_accounts WHERE bank_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                    stmt.setLong(1, bankId);
                    stmt.executeUpdate();
                }

                // Insert updated accounts
                String insertSql = "INSERT INTO bank_accounts (bank_id, particulars, bank_name, account_type, account_number, micr_code, ifsc_code, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    for (OrganizationAccountDto acc : dto.getAccounts()) {
                        stmt.setLong(1, bankId);
                        stmt.setString(2, acc.getParticulars() != null ? acc.getParticulars() : "Collection Account");
                        stmt.setString(3, acc.getBankName() != null ? acc.getBankName() : dto.getBankName());
                        stmt.setString(4, acc.getAccountType() != null ? acc.getAccountType() : "Current");
                        stmt.setString(5, acc.getAccountNumber() != null ? acc.getAccountNumber() : "");
                        stmt.setString(6, acc.getMicrCode());
                        stmt.setString(7, acc.getIfscCode());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                } catch (SQLException ex) {
                    logger.warn("Could not insert into bank_accounts table: {}", ex.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.warn("Could not save extended details or accounts: {}", e.getMessage());
        }
    }

    private void validateUpdateRequest(OrganizationDto dto) {
        if (dto.getPan() != null && !dto.getPan().trim().isEmpty()) {
            if (!PAN_PATTERN.matcher(dto.getPan().trim().toUpperCase()).matches()) {
                throw new BranchException("Please enter a valid PAN number.", HttpStatus.BAD_REQUEST);
            }
        }
        if (dto.getGstNumber() != null && !dto.getGstNumber().trim().isEmpty()) {
            if (!GST_PATTERN.matcher(dto.getGstNumber().trim().toUpperCase()).matches()) {
                throw new BranchException("Please enter a valid GST number.", HttpStatus.BAD_REQUEST);
            }
        }
        if (dto.getPincode() != null && !dto.getPincode().trim().isEmpty()) {
            if (!PIN_PATTERN.matcher(dto.getPincode().trim()).matches()) {
                throw new BranchException("Please enter a valid 6-digit PIN code.", HttpStatus.BAD_REQUEST);
            }
        }
        if (dto.getAccounts() != null) {
            for (OrganizationAccountDto acc : dto.getAccounts()) {
                if (acc.getIfscCode() != null && !acc.getIfscCode().trim().isEmpty()) {
                    if (!IFSC_PATTERN.matcher(acc.getIfscCode().trim().toUpperCase()).matches()) {
                        throw new BranchException("Please enter a valid 11-character IFSC code: '" + acc.getIfscCode() + "'.", HttpStatus.BAD_REQUEST);
                    }
                }
                if (acc.getMicrCode() != null && !acc.getMicrCode().trim().isEmpty()) {
                    if (!MICR_PATTERN.matcher(acc.getMicrCode().trim()).matches()) {
                        throw new BranchException("Please enter a valid 9-digit MICR code: '" + acc.getMicrCode() + "'.", HttpStatus.BAD_REQUEST);
                    }
                }
            }
        }
    }

    private boolean doesTableExist(DatabaseMetaData meta, String tableName) {
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE", "VIEW"})) {
            if (rs.next()) return true;
        } catch (SQLException e) {
        }
        try (ResultSet rs = meta.getTables(null, "public", tableName, new String[]{"TABLE", "VIEW"})) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean doesColumnExist(DatabaseMetaData meta, String tableName, String columnName) {
        try (ResultSet rs = meta.getColumns(null, "public", tableName, columnName)) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private BigDecimal parseBigDecimal(String val) {
        if (val == null || val.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(val.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
