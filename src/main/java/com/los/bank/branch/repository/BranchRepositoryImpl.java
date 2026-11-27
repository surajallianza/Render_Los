package com.los.bank.branch.repository;

import com.los.bank.branch.entity.Branch;
import com.los.bank.branch.service.DynamicBankDataSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of BranchRepository that operates directly on the dynamic
 * bank database resolved by DynamicBankDataSourceService.
 */
@Repository
public class BranchRepositoryImpl implements BranchRepository {

    private static final Logger logger = LoggerFactory.getLogger(BranchRepositoryImpl.class);

    private final DynamicBankDataSourceService dynamicBankDataSourceService;

    public BranchRepositoryImpl(DynamicBankDataSourceService dynamicBankDataSourceService) {
        this.dynamicBankDataSourceService = dynamicBankDataSourceService;
    }

    private static final String SELECT_COLUMNS =
            "id, name, code, bank_name, rbi_license_no, registration_no, incorporation_date, " +
            "working_from, working_to, address, address1, address2, country, city, state, " +
            "pincode, primary_contact_number, secondary_contact_number, email_id, status, " +
            "created_at, updated_at";

    @Override
    public Optional<Branch> findById(Long id) {
        return findById(id, null);
    }

    @Override
    public Optional<Branch> findById(Long id, String bankName) {
        if (id == null) {
            return Optional.empty();
        }
        DataSource ds = dynamicBankDataSourceService.getDataSourceForBank(bankName);
        String sql = "SELECT " + SELECT_COLUMNS + " FROM b001 WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBranch(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding branch by ID {}: {}", id, e.getMessage());
            throw new RuntimeException("Database error finding branch by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Branch> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        DataSource ds = dynamicBankDataSourceService.getDataSourceForBank(null);
        String sql = "SELECT " + SELECT_COLUMNS + " FROM b001 WHERE code = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBranch(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding branch by code '{}': {}", code, e.getMessage());
            throw new RuntimeException("Database error finding branch by code: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Branch> findByCodeIgnoreCase(String code) {
        if (code == null) {
            return Optional.empty();
        }
        DataSource ds = dynamicBankDataSourceService.getDataSourceForBank(null);
        String sql = "SELECT " + SELECT_COLUMNS + " FROM b001 WHERE UPPER(code) = UPPER(?) LIMIT 1";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBranch(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding branch by code ignore case '{}': {}", code, e.getMessage());
            throw new RuntimeException("Database error finding branch by code: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByCode(String code) {
        if (code == null) {
            return false;
        }
        DataSource ds = dynamicBankDataSourceService.getDataSourceForBank(null);
        String sql = "SELECT 1 FROM b001 WHERE code = ? LIMIT 1";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking branch code existence '{}': {}", code, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean existsByCodeIgnoreCase(String code) {
        return existsByCodeIgnoreCase(code, null);
    }

    @Override
    public boolean existsByCodeIgnoreCase(String code, String bankName) {
        if (code == null) {
            return false;
        }
        DataSource ds = dynamicBankDataSourceService.getDataSourceForBank(bankName);
        String sql = "SELECT 1 FROM b001 WHERE UPPER(code) = UPPER(?) LIMIT 1";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking branch code existence ignore case '{}': {}", code, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id, String bankName) {
        if (code == null || id == null) {
            return false;
        }
        DataSource ds = dynamicBankDataSourceService.getDataSourceForBank(bankName);
        String sql = "SELECT 1 FROM b001 WHERE UPPER(code) = UPPER(?) AND id <> ? LIMIT 1";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code.trim());
            stmt.setLong(2, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking branch code existence for code '{}' excluding id {}: {}", code, id, e.getMessage());
            return false;
        }
    }

    @Override
    public List<Branch> findAll() {
        return findAll(null);
    }

    @Override
    public List<Branch> findAll(String bankName) {
        List<Branch> list = new ArrayList<>();
        DataSource ds = dynamicBankDataSourceService.getDataSourceForBank(bankName);
        String sql = "SELECT " + SELECT_COLUMNS + " FROM b001 ORDER BY id ASC";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToBranch(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all branches: {}", e.getMessage());
            throw new RuntimeException("Database error fetching branches: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Branch save(Branch branch) {
        if (branch == null) {
            return null;
        }
        DataSource ds = dynamicBankDataSourceService.getDataSourceForBank(branch.getBankName());

        if (branch.getId() == null) {
            // INSERT
            String insertSql = "INSERT INTO b001 (" +
                    "name, code, bank_name, rbi_license_no, registration_no, incorporation_date, " +
                    "working_from, working_to, address, address1, address2, country, city, state, " +
                    "pincode, primary_contact_number, secondary_contact_number, email_id, status, " +
                    "created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                    "RETURNING id";

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertSql)) {

                stmt.setString(1, branch.getName());
                stmt.setString(2, branch.getCode());
                stmt.setString(3, branch.getBankName());
                stmt.setString(4, branch.getRbiLicenseNo());
                stmt.setString(5, branch.getRegistrationNo());
                stmt.setDate(6, branch.getIncorporationDate() != null ? Date.valueOf(branch.getIncorporationDate()) : null);
                stmt.setTime(7, branch.getWorkingFrom() != null ? Time.valueOf(branch.getWorkingFrom()) : null);
                stmt.setTime(8, branch.getWorkingTo() != null ? Time.valueOf(branch.getWorkingTo()) : null);
                stmt.setString(9, branch.getAddress());
                stmt.setString(10, branch.getAddress1());
                stmt.setString(11, branch.getAddress2());
                stmt.setString(12, branch.getCountry() != null ? branch.getCountry() : "India");
                stmt.setString(13, branch.getCity());
                stmt.setString(14, branch.getState());
                stmt.setString(15, branch.getPincode());
                stmt.setString(16, branch.getPrimaryContactNumber());
                stmt.setString(17, branch.getSecondaryContactNumber());
                stmt.setString(18, branch.getEmailId());
                stmt.setString(19, branch.getStatus() != null ? branch.getStatus() : "ACTIVE");

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        branch.setId(rs.getLong(1));
                    }
                }
                logger.info("Inserted new branch into b001 with ID: {}, Code: '{}'", branch.getId(), branch.getCode());
                return branch;
            } catch (SQLException e) {
                logger.error("Failed to insert branch into b001: {}", e.getMessage());
                throw new RuntimeException("Database error saving branch: " + e.getMessage(), e);
            }
        } else {
            // UPDATE
            String updateSql = "UPDATE b001 SET " +
                    "name = ?, code = ?, bank_name = ?, rbi_license_no = ?, registration_no = ?, " +
                    "incorporation_date = ?, working_from = ?, working_to = ?, address = ?, " +
                    "address1 = ?, address2 = ?, country = ?, city = ?, state = ?, pincode = ?, " +
                    "primary_contact_number = ?, secondary_contact_number = ?, email_id = ?, " +
                    "status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateSql)) {

                stmt.setString(1, branch.getName());
                stmt.setString(2, branch.getCode());
                stmt.setString(3, branch.getBankName());
                stmt.setString(4, branch.getRbiLicenseNo());
                stmt.setString(5, branch.getRegistrationNo());
                stmt.setDate(6, branch.getIncorporationDate() != null ? Date.valueOf(branch.getIncorporationDate()) : null);
                stmt.setTime(7, branch.getWorkingFrom() != null ? Time.valueOf(branch.getWorkingFrom()) : null);
                stmt.setTime(8, branch.getWorkingTo() != null ? Time.valueOf(branch.getWorkingTo()) : null);
                stmt.setString(9, branch.getAddress());
                stmt.setString(10, branch.getAddress1());
                stmt.setString(11, branch.getAddress2());
                stmt.setString(12, branch.getCountry());
                stmt.setString(13, branch.getCity());
                stmt.setString(14, branch.getState());
                stmt.setString(15, branch.getPincode());
                stmt.setString(16, branch.getPrimaryContactNumber());
                stmt.setString(17, branch.getSecondaryContactNumber());
                stmt.setString(18, branch.getEmailId());
                stmt.setString(19, branch.getStatus());
                stmt.setLong(20, branch.getId());

                stmt.executeUpdate();
                return branch;
            } catch (SQLException e) {
                logger.error("Failed to update branch ID {}: {}", branch.getId(), e.getMessage());
                throw new RuntimeException("Database error updating branch: " + e.getMessage(), e);
            }
        }
    }

    private Branch mapResultSetToBranch(ResultSet rs) throws SQLException {
        Date incDate = rs.getDate("incorporation_date");
        Time workFrom = rs.getTime("working_from");
        Time workTo = rs.getTime("working_to");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        return Branch.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .code(rs.getString("code"))
                .bankName(rs.getString("bank_name"))
                .rbiLicenseNo(rs.getString("rbi_license_no"))
                .registrationNo(rs.getString("registration_no"))
                .incorporationDate(incDate != null ? incDate.toLocalDate() : null)
                .workingFrom(workFrom != null ? workFrom.toLocalTime() : null)
                .workingTo(workTo != null ? workTo.toLocalTime() : null)
                .address(rs.getString("address"))
                .address1(rs.getString("address1"))
                .address2(rs.getString("address2"))
                .country(rs.getString("country"))
                .city(rs.getString("city"))
                .state(rs.getString("state"))
                .pincode(rs.getString("pincode"))
                .primaryContactNumber(rs.getString("primary_contact_number"))
                .secondaryContactNumber(rs.getString("secondary_contact_number"))
                .emailId(rs.getString("email_id"))
                .status(rs.getString("status"))
                .createdAt(createdAt != null ? createdAt.toLocalDateTime() : null)
                .updatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null)
                .build();
    }
}
