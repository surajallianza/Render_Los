package com.los.bank.setup.service;

import com.los.bank.setup.dto.BankDto;
import com.los.bank.setup.dto.InitialUserDto;
import com.los.bank.setup.exception.BankSetupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Service responsible for creating the initial super admin user in the newly
 * provisioned
 * and migrated bank database.
 * Resolves incoming user_role to database role_id, validates credentials,
 * encrypts password with BCrypt,
 * ensures branch exists in b001, and inserts records across u001, u002, u003,
 * and u004 tables.
 */
@Service
public class InitialUserCreationService {

    private static final Logger logger = LoggerFactory.getLogger(InitialUserCreationService.class);

    private final PasswordEncoder passwordEncoder;

    public InitialUserCreationService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Backward-compatible overload without BankDto.
     *
     * @param bankDataSource DataSource pointing to the target bank database.
     * @param userDto        User creation information.
     * @return Generated user ID (u001.id).
     */
    public Long createInitialUser(DataSource bankDataSource, InitialUserDto userDto) {
        return createInitialUser(bankDataSource, userDto, null);
    }

    /**
     * Executes initial user creation within the target bank database.
     * Ensures branch exists in b001, seeds u001, u002, u003, and stores u004
     * mapping.
     *
     * @param bankDataSource DataSource pointing to the target bank database.
     * @param userDto        User creation information.
     * @param bankDto        Bank setup information containing branch details.
     * @return Generated user ID (u001.id).
     */
    public Long createInitialUser(DataSource bankDataSource, InitialUserDto userDto, BankDto bankDto) {
        if (userDto == null) {
            throw new IllegalArgumentException("Initial user information must be provided.");
        }

        String username = userDto.getEffectiveUsername();
        String rawPassword = userDto.getPassword();
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            rawPassword = "Admin@" + username; // Safe default password if omitted in test requests
        }
        if (rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must not exceed 72 bytes (in UTF-8 encoding)");
        }

        String email = userDto.getEffectiveEmail();
        String empNo = userDto.getEffectiveEmpNo();
        String userRole = userDto.getEffectiveUserRole();

        try (Connection conn = bankDataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Verify required user and branch tables exist
                verifyBankTablesExist(conn);

                // 2. Ensure roles exist and resolve user_role to role_id
                int roleId = resolveOrSeedRoleId(conn, userRole);
                logger.info("Resolved user_role '{}' to bank database role_id: {}", userRole, roleId);

                // 3. Check if user already exists (retry safety)
                Long existingUserId = findExistingUserId(conn, username, email);
                if (existingUserId != null) {
                    logger.info(
                            "Initial user '{}' already exists in bank database (id={}). Skipping duplicate insertion.",
                            username, existingUserId);
                    conn.commit();
                    return existingUserId;
                }

                // 4. Hash password with BCrypt
                String hashedPassword = passwordEncoder.encode(rawPassword);

                // 5. Insert into u001 (core credentials)
                long userId;
                String insertU001 = "INSERT INTO u001 (emp_no, username, password_hash, status, is_active, no_of_bad_logins) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
                try (PreparedStatement stmt = conn.prepareStatement(insertU001)) {
                    stmt.setString(1, empNo);
                    stmt.setString(2, username);
                    stmt.setString(3, hashedPassword);
                    stmt.setString(4, userDto.getStatus() != null ? userDto.getStatus() : "ACTIVE");
                    stmt.setBoolean(5, true);
                    stmt.setInt(6, userDto.getNoOfBadLogins() != null ? userDto.getNoOfBadLogins() : 0);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            userId = rs.getLong(1);
                        } else {
                            throw new SQLException("Failed to obtain generated ID from u001.");
                        }
                    }
                }

                // 6. Insert into u002 (profile details & role_id mapping)
                boolean hasTitleCol = doesColumnExist(conn, "u002", "title");
                if (hasTitleCol) {
                    String insertU002 = "INSERT INTO u002 (user_id, title, first_name, middle_name, last_name, dob, gender, email, mobile, designation, role_id) "
                            +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertU002)) {
                        stmt.setLong(1, userId);
                        stmt.setString(2, userDto.getTitle());
                        stmt.setString(3, userDto.getEffectiveFirstName());
                        stmt.setString(4, userDto.getMiddleName());
                        stmt.setString(5, userDto.getEffectiveLastName());
                        stmt.setDate(6, parseSqlDate(userDto.getDob()));
                        stmt.setString(7, userDto.getGender() != null ? userDto.getGender() : "MALE");
                        stmt.setString(8, email);
                        stmt.setString(9, userDto.getMobile());
                        stmt.setString(10,
                                userDto.getDesignation() != null ? userDto.getDesignation() : "Super Administrator");
                        stmt.setInt(11, roleId);
                        stmt.executeUpdate();
                    }
                } else {
                    String insertU002 = "INSERT INTO u002 (user_id, first_name, middle_name, last_name, dob, gender, email, mobile, designation, role_id) "
                            +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertU002)) {
                        stmt.setLong(1, userId);
                        stmt.setString(2, userDto.getEffectiveFirstName());
                        stmt.setString(3, userDto.getMiddleName());
                        stmt.setString(4, userDto.getEffectiveLastName());
                        stmt.setDate(5, parseSqlDate(userDto.getDob()));
                        stmt.setString(6, userDto.getGender() != null ? userDto.getGender() : "MALE");
                        stmt.setString(7, email);
                        stmt.setString(8, userDto.getMobile());
                        stmt.setString(9,
                                userDto.getDesignation() != null ? userDto.getDesignation() : "Super Administrator");
                        stmt.setInt(10, roleId);
                        stmt.executeUpdate();
                    }
                }

                // 7. Ensure branch exists in b001, retrieve actual generated b001.id, and
                // assign to userDto.loginBranch
                Long validBranchId = ensureBranchExists(conn, bankDto, userDto.getLoginBranch());
                if (validBranchId != null) {
                    userDto.setLoginBranch(validBranchId);
                }

                // 8. Insert into u003 (system role / branch constraints)
                int sessionTimeout = userDto.getSessionTimeout() != null && userDto.getSessionTimeout() > 0
                        ? userDto.getSessionTimeout()
                        : 30;
                boolean hasSystemRoleCol = doesColumnExist(conn, "u003", "system_role");

                if (hasSystemRoleCol) {
                    int systemRoleInt = parseSystemRole(userDto.getSystemRole());
                    String insertU003 = "INSERT INTO u003 (user_id, system_role, login_branch_id, login_time, logout_time, inactive_session_timeout) "
                            +
                            "VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertU003)) {
                        stmt.setLong(1, userId);
                        stmt.setInt(2, systemRoleInt);
                        if (validBranchId != null) {
                            stmt.setLong(3, validBranchId);
                        } else {
                            stmt.setNull(3, java.sql.Types.BIGINT);
                        }
                        stmt.setTime(4, parseSqlTime(userDto.getLoginTime()));
                        stmt.setTime(5, parseSqlTime(userDto.getLogoutTime()));
                        stmt.setInt(6, sessionTimeout);
                        stmt.executeUpdate();
                    }
                } else {
                    String insertU003 = "INSERT INTO u003 (user_id, multi_branch_access, login_branch_id, login_on_holidays, login_time, logout_time, inactive_session_timeout) "
                            +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertU003)) {
                        stmt.setLong(1, userId);
                        stmt.setBoolean(2, true);
                        if (validBranchId != null) {
                            stmt.setLong(3, validBranchId);
                        } else {
                            stmt.setNull(3, java.sql.Types.BIGINT);
                        }
                        stmt.setBoolean(4, false);
                        stmt.setTime(5, parseSqlTime(userDto.getLoginTime()));
                        stmt.setTime(6, parseSqlTime(userDto.getLogoutTime()));
                        stmt.setInt(7, sessionTimeout);
                        stmt.executeUpdate();
                    }
                }

                // 9. Store user-to-branch mapping in u004 using actual u001.id and b001.id
                if (validBranchId != null) {
                    insertUserBranchMapping(conn, userId, validBranchId, userDto.getTitle());
                }

                conn.commit();
                logger.info(
                        "Initial user '{}' (ID: {}) created successfully with role_id: {}, branch_id: {} in bank database.",
                        username, userId, roleId, validBranchId);
                return userId;

            } catch (Exception e) {
                conn.rollback();
                logger.error("Error creating initial bank user: {}", e.getMessage(), e);
                throw new BankSetupException("Initial user creation failed: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            String errorMsg = "Database error connecting to bank database for initial user creation: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new BankSetupException(errorMsg, e);
        }
    }

    /**
     * Retrieves initial user details from the bank database without exposing
     * password.
     */
    public InitialUserDto getInitialUser(DataSource bankDataSource, Long userId) {
        if (userId == null) {
            return null;
        }
        try (Connection conn = bankDataSource.getConnection()) {
            boolean hasTitleCol = doesColumnExist(conn, "u002", "title");
            String titleSelect = hasTitleCol ? "COALESCE(u002.title, u004.title, '') AS title"
                    : "COALESCE(u004.title, '') AS title";
            boolean hasSystemRoleCol = doesColumnExist(conn, "u003", "system_role");
            String systemRoleSelect = hasSystemRoleCol ? "u003.system_role::text" : "'1' AS system_role";

            String sql = "SELECT u001.emp_no, u001.username, " + titleSelect + ", u002.first_name, u002.middle_name, " +
                    "u002.last_name, u002.dob, u002.email, u002.mobile, u002.gender, u002.designation, " +
                    "r001.name AS user_role, " + systemRoleSelect + ", u001.status, u003.login_branch_id, " +
                    "u003.login_time, u003.logout_time, u003.inactive_session_timeout, u001.no_of_bad_logins " +
                    "FROM u001 " +
                    "JOIN u002 ON u002.user_id = u001.id " +
                    "LEFT JOIN r001 ON r001.id = u002.role_id " +
                    "LEFT JOIN u003 ON u003.user_id = u001.id " +
                    "LEFT JOIN u004 ON u004.user_id = u001.id " +
                    "WHERE u001.id = ? " +
                    "LIMIT 1";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return InitialUserDto.builder()
                                .empId(rs.getString("emp_no"))
                                .username(rs.getString("username"))
                                .title(rs.getString("title"))
                                .firstName(rs.getString("first_name"))
                                .middleName(rs.getString("middle_name"))
                                .lastName(rs.getString("last_name"))
                                .dob(rs.getDate("dob") != null ? rs.getDate("dob").toString() : null)
                                .email(rs.getString("email"))
                                .mobile(rs.getString("mobile"))
                                .gender(rs.getString("gender"))
                                .designation(rs.getString("designation"))
                                .userRole(rs.getString("user_role"))
                                .systemRole(rs.getString("system_role"))
                                .status(rs.getString("status"))
                                .loginBranch(
                                        rs.getObject("login_branch_id") != null ? rs.getLong("login_branch_id") : 0L)
                                .loginTime(rs.getTime("login_time") != null ? rs.getTime("login_time").toString() : "")
                                .logoutTime(
                                        rs.getTime("logout_time") != null ? rs.getTime("logout_time").toString() : "")
                                .sessionTimeout(rs.getObject("inactive_session_timeout") != null
                                        ? rs.getInt("inactive_session_timeout")
                                        : 30)
                                .noOfBadLogins(
                                        rs.getObject("no_of_bad_logins") != null ? rs.getInt("no_of_bad_logins") : 0)
                                .build();
                    }
                }
            }
        } catch (SQLException e) {
            logger.warn("Could not retrieve initial user details: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Resolves requested user_role to r001 primary key. Seeds default roles if
     * absent.
     */
    private int resolveOrSeedRoleId(Connection conn, String userRole) throws SQLException {
        // First check if role exists
        Integer roleId = findRoleIdByName(conn, userRole);
        if (roleId != null) {
            return roleId;
        }

        // Check if r001 is completely empty or missing required roles, and seed
        // baseline roles
        logger.info("Role '{}' not found in r001. Ensuring baseline roles exist...", userRole);
        seedDefaultRolesIfEmpty(conn);

        // Try finding role again
        roleId = findRoleIdByName(conn, userRole);
        if (roleId != null) {
            return roleId;
        }

        // If specific role still not found and role is SUPER_ADMIN variant, create it
        if ("SUPER_ADMIN".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)) {
            String insertRole = "INSERT INTO r001 (name, panel, description) VALUES ('SUPER_ADMIN', 'ADMIN', 'Super Administrator Role') RETURNING id";
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(insertRole)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new BankSetupException(String
                .format("Requested user_role '%s' does not exist in bank database role repository (r001).", userRole));
    }

    private Integer findRoleIdByName(Connection conn, String roleName) throws SQLException {
        String query = "SELECT id FROM r001 WHERE UPPER(name) = UPPER(?) LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, roleName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

    private void seedDefaultRolesIfEmpty(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM r001";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                logger.info("r001 role table is empty. Seeding default roles: SUPER_ADMIN, MAKER, CHECKER");
                String seedSql = "INSERT INTO r001 (name, panel, description) VALUES " +
                        "('SUPER_ADMIN', 'ADMIN', 'Super Administrator Role'), " +
                        "('MAKER', 'OPERATIONS', 'Maker Role'), " +
                        "('CHECKER', 'OPERATIONS', 'Checker Role')";
                stmt.executeUpdate(seedSql);
            }
        }
    }

    private Long findExistingUserId(Connection conn, String username, String email) throws SQLException {
        String checkU001 = "SELECT id FROM u001 WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkU001)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }

        String checkU002 = "SELECT user_id FROM u002 WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkU002)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("user_id");
                }
            }
        }
        return null;
    }

    private boolean doesColumnExist(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? AND column_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            stmt.setString(2, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Long resolveValidBranchId(Connection conn, Long branchId) {
        if (branchId == null || branchId <= 0) {
            return null;
        }
        String checkBranch = "SELECT id FROM b001 WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(checkBranch)) {
            stmt.setLong(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (SQLException e) {
            logger.warn("Could not verify branch ID {}: {}", branchId, e.getMessage());
        }
        return null;
    }

    private void verifyBankTablesExist(Connection conn) throws SQLException {
        String[] requiredTables = { "b001", "u001", "u002", "u003", "u004", "r001" };
        for (String table : requiredTables) {
            String check = "SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(check)) {
                stmt.setString(1, table);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new BankSetupException(
                                String.format("Required bank table '%s' was not created by Flyway migration.", table));
                    }
                }
            }
        }
    }

    /**
     * Ensures an initial branch exists in b001.
     * 1. If requestedBranchId is provided, check if it already exists in b001.
     * 2. Else check if a branch with this code already exists in b001.
     * 3. Else check if any branch exists at all (e.g. pre-seeded or existing).
     * 4. Otherwise, insert a new main branch into b001 using BankDto details.
     */
    public Long ensureBranchExists(Connection conn, BankDto bankDto, Long requestedBranchId) throws SQLException {
        // 1. If requestedBranchId is provided, check if it already exists in b001
        if (requestedBranchId != null && requestedBranchId > 0) {
            String checkById = "SELECT id FROM b001 WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkById)) {
                stmt.setLong(1, requestedBranchId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long foundId = rs.getLong("id");
                        logger.info("Found existing branch by ID: {}", foundId);
                        return foundId;
                    }
                }
            }
        }

        // Branch code based on bank code (max 20 chars)
        String rawBankCode = bankDto != null && bankDto.getEffectiveBankCode() != null
                ? bankDto.getEffectiveBankCode()
                : "BR";
        String branchCode = (rawBankCode + "001").trim();
        if (branchCode.length() > 20) {
            branchCode = branchCode.substring(0, 20);
        }

        // 2. Check if a branch with this code already exists in b001
        String checkByCode = "SELECT id FROM b001 WHERE UPPER(code) = UPPER(?)";
        try (PreparedStatement stmt = conn.prepareStatement(checkByCode)) {
            stmt.setString(1, branchCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long foundId = rs.getLong("id");
                    logger.info("Found existing branch by code '{}': ID {}", branchCode, foundId);
                    return foundId;
                }
            }
        }

        // 3. Check if any branch exists at all (e.g. pre-seeded or existing)
        String checkAny = "SELECT id FROM b001 ORDER BY id ASC LIMIT 1";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(checkAny)) {
            if (rs.next()) {
                long foundId = rs.getLong("id");
                logger.info("Reusing existing branch ID: {}", foundId);
                return foundId;
            }
        }

        // 4. Create new main branch in b001
        String branchName = bankDto != null && bankDto.getEffectiveBankName() != null
                ? (bankDto.getEffectiveBankName() + " - Main Branch")
                : "Main Branch";
        if (branchName.length() > 150) {
            branchName = branchName.substring(0, 150);
        }

        String address = bankDto != null ? bankDto.getEffectiveAddress() : null;
        if (address != null && address.length() > 255) {
            address = address.substring(0, 255);
        }

        String city = bankDto != null ? bankDto.getCity() : null;
        if (city != null && city.length() > 100) {
            city = city.substring(0, 100);
        }

        String state = bankDto != null ? bankDto.getState() : null;
        if (state != null && state.length() > 100) {
            state = state.substring(0, 100);
        }

        String pincode = bankDto != null ? bankDto.getPincode() : null;
        if (pincode != null && pincode.length() > 10) {
            pincode = pincode.substring(0, 10);
        }

        String status = bankDto != null && bankDto.getStatus() != null
                ? bankDto.getStatus()
                : "ACTIVE";
        if (status.length() > 20) {
            status = status.substring(0, 20);
        }

        String insertBranch = "INSERT INTO b001 (name, code, address, city, state, pincode, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = conn.prepareStatement(insertBranch)) {
            stmt.setString(1, branchName);
            stmt.setString(2, branchCode);
            stmt.setString(3, address);
            stmt.setString(4, city);
            stmt.setString(5, state);
            stmt.setString(6, pincode);
            stmt.setString(7, status);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long generatedBranchId = rs.getLong(1);
                    logger.info("Created initial branch in b001: '{}' (code: '{}', id: {})", branchName, branchCode,
                            generatedBranchId);
                    return generatedBranchId;
                }
            }
        }

        throw new SQLException("Failed to create or retrieve initial branch in b001.");
    }

    /**
     * Stores user-to-branch mapping in u004 using actual u001.id, b001.id, and
     * title.
     * Uses ON CONFLICT for retry safety.
     */
    public void insertUserBranchMapping(Connection conn, long userId, long branchId, String title) throws SQLException {
        String safeTitle = (title != null && !title.trim().isEmpty()) ? title.trim() : "Mr.";
        if (safeTitle.length() > 20) {
            safeTitle = safeTitle.substring(0, 20);
        }

        String insertU004 = "INSERT INTO u004 (user_id, branch_id, title) VALUES (?, ?, ?) " +
                "ON CONFLICT (user_id, branch_id) DO UPDATE SET title = EXCLUDED.title";
        try (PreparedStatement stmt = conn.prepareStatement(insertU004)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, branchId);
            stmt.setString(3, safeTitle);
            stmt.executeUpdate();
            logger.info("Stored user-to-branch mapping in u004: user_id={}, branch_id={}, title='{}'", userId, branchId,
                    safeTitle);
        }
    }

    private int parseSystemRole(String systemRole) {
        if (systemRole == null || systemRole.trim().isEmpty()) {
            return 1; // Default to 1 (Super Admin system role)
        }
        try {
            return Integer.parseInt(systemRole.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private Date parseSqlDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            LocalDate localDate = LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            return Date.valueOf(localDate);
        } catch (Exception e) {
            logger.warn("Unable to parse date string '{}'. Using null.", dateStr);
            return null;
        }
    }

    private Time parseSqlTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        try {
            String trimmed = timeStr.trim();
            if (trimmed.length() == 5) {
                trimmed = trimmed + ":00";
            }
            LocalTime localTime = LocalTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_TIME);
            return Time.valueOf(localTime);
        } catch (Exception e) {
            logger.warn("Unable to parse time string '{}'. Using null.", timeStr);
            return null;
        }
    }
}
