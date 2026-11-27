package com.los.bank.user.service;

import com.los.bank.user.context.BankContext;
import com.los.bank.user.dto.UserCreateRequest;
import com.los.bank.user.dto.UserResponse;
import com.los.bank.user.dto.UserUpdateRequest;
import com.los.bank.user.entity.User;
import com.los.bank.user.entity.UserBranchMapping;
import com.los.bank.user.entity.UserProfile;
import com.los.bank.user.entity.UserSecuritySetting;
import com.los.bank.user.entity.UserToken;
import com.los.bank.user.exception.UserDuplicateException;
import com.los.bank.user.exception.UserNotFoundException;
import com.los.bank.user.exception.UserValidationException;
import com.los.bank.user.repository.UserBranchMappingRepository;
import com.los.bank.user.repository.UserProfileRepository;
import com.los.bank.user.repository.UserRepository;
import com.los.bank.user.repository.UserSecuritySettingRepository;
import com.los.bank.user.repository.UserTokenRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSecuritySettingRepository userSecuritySettingRepository;
    private final UserBranchMappingRepository userBranchMappingRepository;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource bankRoutingDataSource;
    private final SecureRandom secureRandom = new SecureRandom();

    @PersistenceContext(unitName = "userBankUnit")
    private EntityManager entityManager;

    public UserServiceImpl(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserSecuritySettingRepository userSecuritySettingRepository,
            UserBranchMappingRepository userBranchMappingRepository,
            UserTokenRepository userTokenRepository,
            PasswordEncoder passwordEncoder,
            @Qualifier("bankRoutingDataSource") DataSource bankRoutingDataSource) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSecuritySettingRepository = userSecuritySettingRepository;
        this.userBranchMappingRepository = userBranchMappingRepository;
        this.userTokenRepository = userTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.bankRoutingDataSource = bankRoutingDataSource;
    }

    /**
     * API 1: GET – Fetch all users
     */
    @Override
    @Transactional(transactionManager = "userTransactionManager", readOnly = true)
    public List<UserResponse> getAllUsers() {
        logAndVerifyDatabaseContext("getAllUsers");
        List<User> users = userRepository.findAllWithDetails();
        return users.stream()
                .map(u -> mapToResponse(u, null))
                .collect(Collectors.toList());
    }

    /**
     * API 2: POST – Create user with password
     */
    @Override
    @Transactional(transactionManager = "userTransactionManager")
    public UserResponse createUser(UserCreateRequest request) {
        logAndVerifyDatabaseContext("createUser (POST /api/users)");
        validateCreateRequest(request);

        // 1. Password Confirmation & Length Validation
        String rawPassword = request.getPassword().trim();
        String confirmPassword = request.getConfirmPassword() != null ? request.getConfirmPassword().trim() : "";
        if (!rawPassword.equals(confirmPassword)) {
            throw new UserValidationException("Password and confirm password do not match");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new UserValidationException("Password must not exceed 72 bytes (in UTF-8 encoding)");
        }

        // 2. Uniqueness Validations in the target bank database
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new UserDuplicateException("Username already exists: " + username);
        }

        String empNo = request.getEmpNo().trim();
        if (userRepository.existsByEmpNo(empNo)) {
            throw new UserDuplicateException("Employee number already exists: " + empNo);
        }

        String email = request.getEmail() != null ? request.getEmail().trim() : null;
        if (email != null && !email.isEmpty() && userProfileRepository.existsByEmail(email)) {
            throw new UserDuplicateException("Email already exists: " + email);
        }

        // 3. BCrypt Password Hashing (Never store plaintext password)
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 4. Construct Core User Entity (u001)
        User user = User.builder()
                .empNo(empNo)
                .username(username)
                .passwordHash(encodedPassword)
                .status(request.getStatus() != null && !request.getStatus().trim().isEmpty()
                        ? request.getStatus().trim()
                        : "ACTIVE")
                .isActive(true)
                .noOfBadLogins(0)
                .build();

        // 5. Construct Profile Entity (u002)
        UserProfile profile = UserProfile.builder()
                .firstName(request.getFirstName() != null ? request.getFirstName().trim() : "")
                .middleName(request.getMiddleName() != null ? request.getMiddleName().trim() : null)
                .lastName(request.getLastName() != null ? request.getLastName().trim() : "")
                .dob(parseDate(request.getDob()))
                .gender(request.getGender() != null ? request.getGender().trim().toUpperCase() : "MALE")
                .email(email)
                .mobile(request.getMobile() != null ? request.getMobile().trim() : null)
                .designation(request.getDesignation() != null ? request.getDesignation().trim() : null)
                .roleId(request.getRoleId() != null ? request.getRoleId() : 1)
                .build();
        user.setProfile(profile);

        // 6. Construct Security Settings (u003)
        UserSecuritySetting securitySetting = UserSecuritySetting.builder()
                .systemRole(request.getSystemRole() != null ? request.getSystemRole() : 1)
                .loginBranchId(request.getLoginBranchId())
                .loginTime(parseTime(request.getLoginTime()))
                .logoutTime(parseTime(request.getLogoutTime()))
                .inactiveSessionTimeout(request.getSessionTimeout() != null && request.getSessionTimeout() > 0
                        ? request.getSessionTimeout()
                        : 30)
                .createdAt(LocalDateTime.now())
                .build();
        user.setSecuritySetting(securitySetting);

        // 7. Persist User, Profile & Security
        User savedUser = userRepository.save(user);

        // 8. Branch Mapping (u004) if login branch provided
        if (request.getLoginBranchId() != null && request.getLoginBranchId() > 0) {
            String title = request.getTitle() != null && !request.getTitle().trim().isEmpty()
                    ? request.getTitle().trim()
                    : "Mr.";
            UserBranchMapping branchMapping = UserBranchMapping.builder()
                    .userId(savedUser.getId())
                    .branchId(request.getLoginBranchId())
                    .title(title)
                    .build();
            userBranchMappingRepository.save(branchMapping);
        }

        // 9. Also Save Token (u007)
        String generatedToken = null;
        if (request.getGenerateToken() == null || Boolean.TRUE.equals(request.getGenerateToken())) {
            generatedToken = generateAndPersistToken(savedUser.getId(), 24);
            logger.info("Generated and saved token for user id: {} in target bank database", savedUser.getId());
        }

        logger.info("Successfully created user: id={}, username='{}' in target bank database",
                savedUser.getId(), savedUser.getUsername());
        return mapToResponse(savedUser, generatedToken);
    }

    /**
     * API 3: PUT – Update user
     * Makes ALL existing user information and fields editable.
     * Allows updating any single field, multiple fields, or all fields together.
     * Preserves existing values when fields are not provided in the update request.
     * Fixes duplicate validation so unchanged values belonging to the current user
     * do NOT generate conflicts.
     * Encodes password if provided; never exposes password in responses.
     */
    @Override
    @Transactional(transactionManager = "userTransactionManager")
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        logAndVerifyDatabaseContext("updateUser (PUT /api/users/" + id + ")");
        if (id == null) {
            throw new UserValidationException("User ID is required for update");
        }
        if (request == null) {
            throw new UserValidationException("Update request payload cannot be empty");
        }

        User user = userRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = UserProfile.builder().userId(user.getId()).user(user).build();
            user.setProfile(profile);
        }

        UserSecuritySetting securitySetting = user.getSecuritySetting();
        if (securitySetting == null) {
            securitySetting = UserSecuritySetting.builder().userId(user.getId()).user(user).build();
            user.setSecuritySetting(securitySetting);
        }

        // =========================================================================
        // 1. Password Update Validation & BCrypt Hashing (Optional)
        // =========================================================================
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            String newPassword = request.getPassword().trim();
            String confirmPassword = request.getConfirmPassword() != null ? request.getConfirmPassword().trim() : "";
            if (!newPassword.equals(confirmPassword)) {
                throw new UserValidationException("New password and confirm password do not match");
            }
            if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new UserValidationException("Password must not exceed 72 bytes (in UTF-8 encoding)");
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            logger.info("Successfully updated password hash via BCrypt for user id: {}", id);
        }

        // =========================================================================
        // 2. Uniqueness Validations (Permit Unchanged Values for Current User)
        // =========================================================================
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String newUsername = request.getUsername().trim();
            // Only validate if username is changing from the current user's existing value
            if (!newUsername.equalsIgnoreCase(user.getUsername())) {
                if (userRepository.existsByUsernameAndIdNot(newUsername, id)) {
                    throw new UserDuplicateException("Username is already taken by another user: " + newUsername);
                }
            }
            user.setUsername(newUsername);
        }

        if (request.getEmpNo() != null && !request.getEmpNo().trim().isEmpty()) {
            String newEmpNo = request.getEmpNo().trim();
            // Only validate if empNo is changing from the current user's existing value
            if (!newEmpNo.equalsIgnoreCase(user.getEmpNo())) {
                if (userRepository.existsByEmpNoAndIdNot(newEmpNo, id)) {
                    throw new UserDuplicateException("Employee number is already taken by another user: " + newEmpNo);
                }
            }
            user.setEmpNo(newEmpNo);
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim();
            String currentEmail = profile.getEmail();
            // Only validate if email is changing from the current user's existing value
            if (currentEmail == null || !newEmail.equalsIgnoreCase(currentEmail.trim())) {
                if (userProfileRepository.existsByEmailAndUserIdNot(newEmail, id)) {
                    throw new UserDuplicateException("Email is already registered to another user: " + newEmail);
                }
            }
            profile.setEmail(newEmail);
        }

        // =========================================================================
        // 3. Update Core User Fields (u001) - Preserve existing if null
        // =========================================================================
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            user.setStatus(request.getStatus().trim());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        if (request.getNoOfBadLogins() != null) {
            user.setNoOfBadLogins(request.getNoOfBadLogins());
        }
        if (request.getLastLoginDate() != null && !request.getLastLoginDate().trim().isEmpty()) {
            user.setLastLoginDate(parseDate(request.getLastLoginDate()));
        }
        if (request.getLastLoginTime() != null && !request.getLastLoginTime().trim().isEmpty()) {
            user.setLastLoginTime(parseTime(request.getLastLoginTime()));
        }

        // =========================================================================
        // 4. Update Profile Fields (u002) - Preserve existing if null
        // =========================================================================
        if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
            profile.setFirstName(request.getFirstName().trim());
        }
        if (request.getMiddleName() != null) {
            profile.setMiddleName(request.getMiddleName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
            profile.setLastName(request.getLastName().trim());
        }
        if (request.getDob() != null && !request.getDob().trim().isEmpty()) {
            profile.setDob(parseDate(request.getDob()));
        }
        if (request.getGender() != null && !request.getGender().trim().isEmpty()) {
            profile.setGender(request.getGender().trim().toUpperCase());
        }
        if (request.getMobile() != null && !request.getMobile().trim().isEmpty()) {
            profile.setMobile(request.getMobile().trim());
        }
        if (request.getDesignation() != null && !request.getDesignation().trim().isEmpty()) {
            profile.setDesignation(request.getDesignation().trim());
        }
        if (request.getRoleId() != null) {
            profile.setRoleId(request.getRoleId());
        }

        // =========================================================================
        // 5. Update Security & Session Settings (u003) - Preserve existing if null
        // =========================================================================
        if (request.getSystemRole() != null) {
            securitySetting.setSystemRole(request.getSystemRole());
        }
        if (request.getLoginBranchId() != null) {
            securitySetting.setLoginBranchId(request.getLoginBranchId());
        }
        if (request.getLoginTime() != null && !request.getLoginTime().trim().isEmpty()) {
            securitySetting.setLoginTime(parseTime(request.getLoginTime()));
        }
        if (request.getLogoutTime() != null && !request.getLogoutTime().trim().isEmpty()) {
            securitySetting.setLogoutTime(parseTime(request.getLogoutTime()));
        }
        if (request.getSessionTimeout() != null && request.getSessionTimeout() > 0) {
            securitySetting.setInactiveSessionTimeout(request.getSessionTimeout());
        }
        if (request.getCreatedBy() != null) {
            securitySetting.setCreatedBy(request.getCreatedBy());
        }
        if (request.getVerifiedBy() != null) {
            securitySetting.setVerifiedBy(request.getVerifiedBy());
        }
        if (request.getVerifiedDate() != null && !request.getVerifiedDate().trim().isEmpty()) {
            securitySetting.setVerifiedDate(parseDateTime(request.getVerifiedDate()));
        }
        if (request.getModifiedBy() != null) {
            securitySetting.setModifiedBy(request.getModifiedBy());
        }
        securitySetting.setUpdatedAt(LocalDateTime.now());

        // =========================================================================
        // 6. Update User-Branch Mapping (u004) if applicable
        // =========================================================================
        Long targetBranchId = request.getLoginBranchId() != null ? request.getLoginBranchId()
                : securitySetting.getLoginBranchId();
        if (targetBranchId != null && targetBranchId > 0) {
            String title = (request.getTitle() != null && !request.getTitle().trim().isEmpty())
                    ? request.getTitle().trim()
                    : "Mr.";
            UserBranchMapping mapping = UserBranchMapping.builder()
                    .userId(user.getId())
                    .branchId(targetBranchId)
                    .title(title)
                    .build();
            userBranchMappingRepository.save(mapping);
        }

        User updatedUser = userRepository.save(user);
        logger.info("Successfully updated user id: {} in target bank database", id);
        return mapToResponse(updatedUser, null);
    }

    /**
     * API 4: GET – Fetch user by ID
     */
    @Override
    @Transactional(transactionManager = "userTransactionManager", readOnly = true)
    public UserResponse getUserById(Long id) {
        logAndVerifyDatabaseContext("getUserById (GET /api/users/" + id + ")");
        if (id == null) {
            throw new UserValidationException("User ID must be provided");
        }
        User user = userRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return mapToResponse(user, null);
    }

    /**
     * Dedicated Token Management method to save token securely in table u007.
     */
    @Override
    @Transactional(transactionManager = "userTransactionManager")
    public UserToken saveToken(Long userId, String tokenValue, int expirationHours) {
        logAndVerifyDatabaseContext("saveToken (POST /api/users/" + userId + "/token)");
        if (userId == null) {
            throw new UserValidationException("User ID must be provided to save token");
        }
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        String rawToken = (tokenValue != null && !tokenValue.trim().isEmpty())
                ? tokenValue.trim()
                : generateSecureRandomToken();

        String tokenHash = hashToken(rawToken);
        int hours = expirationHours > 0 ? expirationHours : 24;

        UserToken tokenEntity = UserToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .used(false)
                .expiresAt(LocalDateTime.now().plusHours(hours))
                .createdAt(LocalDateTime.now())
                .build();

        return userTokenRepository.save(tokenEntity);
    }

    private void logAndVerifyDatabaseContext(String operation) {
        if (bankRoutingDataSource == null) {
            return;
        }
        try (Connection conn = bankRoutingDataSource.getConnection()) {
            if (conn == null) {
                return;
            }
            DatabaseMetaData meta = conn.getMetaData();
            String currentDb = conn.getCatalog();
            String currentSchema = conn.getSchema();
            String url = meta.getURL();

            logger.info("================================================================================");
            logger.info("[ROUTING DIAGNOSTIC] Operation:               {}", operation);
            logger.info("[ROUTING DIAGNOSTIC] BankContext Target:       '{}'", BankContext.getCurrentDatabase());
            logger.info("[ROUTING DIAGNOSTIC] Active Connected DB:      '{}'", currentDb);
            logger.info("[ROUTING DIAGNOSTIC] Active Connected Schema:  '{}'", currentSchema);
            logger.info("[ROUTING DIAGNOSTIC] JDBC Connection URL:      {}", url);
            logger.info("[ROUTING DIAGNOSTIC] Target Table Executed:    {}.{}.u001", currentDb, currentSchema);
            logger.info("================================================================================");

            if ("bank_setup".equalsIgnoreCase(currentDb)) {
                String errorMsg = String.format(
                        "CRITICAL ROUTING VIOLATION: UserRepository query attempted to execute on '%s'! " +
                                "Must route to separate bank database. Check BankContext configuration.",
                        currentDb);
                logger.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
        } catch (SQLException e) {
            logger.error("[ROUTING DIAGNOSTIC] Could not verify database connection context: {}", e.getMessage(), e);
        }
    }

    private String generateAndPersistToken(Long userId, int expirationHours) {
        String rawToken = generateSecureRandomToken();
        String tokenHash = hashToken(rawToken);

        UserToken token = UserToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .used(false)
                .expiresAt(LocalDateTime.now().plusHours(expirationHours))
                .createdAt(LocalDateTime.now())
                .build();
        userTokenRepository.save(token);
        return rawToken;
    }

    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(rawToken.hashCode());
        }
    }

    private void validateCreateRequest(UserCreateRequest request) {
        if (request == null) {
            throw new UserValidationException("User creation request cannot be empty");
        }
        if (request.getEmpNo() == null || request.getEmpNo().trim().isEmpty()) {
            throw new UserValidationException("Employee number (emp_no) is required");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new UserValidationException("Username is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new UserValidationException("Password is required");
        }
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new UserValidationException("Password must not exceed 72 bytes (in UTF-8 encoding)");
        }
        if (request.getConfirmPassword() == null || request.getConfirmPassword().trim().isEmpty()) {
            throw new UserValidationException("Password confirmation (confirm_password) is required");
        }
        if (request.getConfirmPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new UserValidationException("Confirm password must not exceed 72 bytes (in UTF-8 encoding)");
        }
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new UserValidationException("First name (first_name) is required");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new UserValidationException("Last name (last_name) is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new UserValidationException("Email is required");
        }
    }

    private UserResponse mapToResponse(User user, String token) {
        UserProfile profile = user.getProfile();
        UserSecuritySetting security = user.getSecuritySetting();

        String firstName = profile != null ? profile.getFirstName() : "";
        String middleName = profile != null ? profile.getMiddleName() : "";
        String lastName = profile != null ? profile.getLastName() : "";

        StringBuilder fullName = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            fullName.append(firstName);
        }
        if (middleName != null && !middleName.isEmpty()) {
            if (fullName.length() > 0)
                fullName.append(" ");
            fullName.append(middleName);
        }
        if (lastName != null && !lastName.isEmpty()) {
            if (fullName.length() > 0)
                fullName.append(" ");
            fullName.append(lastName);
        }

        // Retrieve title from u004 branch mapping if present
        String title = null;
        try {
            List<UserBranchMapping> mappings = userBranchMappingRepository.findByUserId(user.getId());
            if (!mappings.isEmpty() && mappings.get(0).getTitle() != null) {
                title = mappings.get(0).getTitle();
            }
        } catch (Exception ignored) {
        }

        return UserResponse.builder()
                .id(user.getId())
                .empNo(user.getEmpNo())
                .username(user.getUsername())
                .title(title)
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .noOfBadLogins(user.getNoOfBadLogins())
                .lastLoginDate(user.getLastLoginDate() != null ? user.getLastLoginDate().toString() : null)
                .lastLoginTime(user.getLastLoginTime() != null ? user.getLastLoginTime().toString() : null)
                .firstName(firstName)
                .middleName(middleName)
                .lastName(lastName)
                .fullName(fullName.toString())
                .email(profile != null ? profile.getEmail() : null)
                .mobile(profile != null ? profile.getMobile() : null)
                .dob(profile != null && profile.getDob() != null ? profile.getDob().toString() : null)
                .gender(profile != null ? profile.getGender() : null)
                .designation(profile != null ? profile.getDesignation() : null)
                .roleId(profile != null ? profile.getRoleId() : null)
                .systemRole(security != null ? security.getSystemRole() : null)
                .loginBranchId(security != null ? security.getLoginBranchId() : null)
                .loginTime(
                        security != null && security.getLoginTime() != null ? security.getLoginTime().toString() : null)
                .logoutTime(security != null && security.getLogoutTime() != null ? security.getLogoutTime().toString()
                        : null)
                .sessionTimeout(security != null ? security.getInactiveSessionTimeout() : null)
                .createdBy(security != null ? security.getCreatedBy() : null)
                .verifiedBy(security != null ? security.getVerifiedBy() : null)
                .verifiedDate(
                        security != null && security.getVerifiedDate() != null ? security.getVerifiedDate().toString()
                                : null)
                .modifiedBy(security != null ? security.getModifiedBy() : null)
                .token(token)
                .createdAt(
                        security != null && security.getCreatedAt() != null ? security.getCreatedAt().toString() : null)
                .updatedAt(
                        security != null && security.getUpdatedAt() != null ? security.getUpdatedAt().toString() : null)
                .build();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (Exception ex) {
                logger.warn("Unable to parse date: {}", dateStr);
                return null;
            }
        }
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        try {
            String trimmed = timeStr.trim();
            if (trimmed.length() == 5) { // HH:mm
                return LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("HH:mm"));
            }
            return LocalTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (Exception e) {
            logger.warn("Unable to parse time: {}", timeStr);
            return null;
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                logger.warn("Unable to parse datetime: {}", dateTimeStr);
                return null;
            }
        }
    }
}
