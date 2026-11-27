package com.los.bank.login.service;

import com.los.bank.Otp.service.OtpService;
import com.los.bank.Rbac.entity.Role;
import com.los.bank.Rbac.service.RbacService;
import com.los.bank.login.DTO.*;
import com.los.bank.login.entity.UserJwtSession;
import com.los.bank.login.entity.UserRefreshToken;
import com.los.bank.login.exception.AccountInactiveException;
import com.los.bank.login.exception.InvalidCredentialsException;
import com.los.bank.login.exception.OtpVerificationException;
import com.los.bank.login.repository.UserJwtSessionRepository;
import com.los.bank.login.repository.UserRefreshTokenRepository;
import com.los.bank.user.entity.User;
import com.los.bank.user.entity.UserBranchMapping;
import com.los.bank.user.entity.UserProfile;
import com.los.bank.user.entity.UserSecuritySetting;
import com.los.bank.user.repository.UserBranchMappingRepository;
import com.los.bank.user.repository.UserProfileRepository;
import com.los.bank.user.repository.UserRepository;
import com.los.bank.user.repository.UserSecuritySettingRepository;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoginServiceImpl implements LoginService {

    private static final Logger logger = LoggerFactory.getLogger(LoginServiceImpl.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSecuritySettingRepository userSecuritySettingRepository;
    private final UserBranchMappingRepository userBranchMappingRepository;
    private final OtpService otpService;
    private final RbacService rbacService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final UserJwtSessionRepository userJwtSessionRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginServiceImpl(UserRepository userRepository,
                            UserProfileRepository userProfileRepository,
                            UserSecuritySettingRepository userSecuritySettingRepository,
                            UserBranchMappingRepository userBranchMappingRepository,
                            OtpService otpService,
                            RbacService rbacService,
                            JwtTokenProvider jwtTokenProvider,
                            UserRefreshTokenRepository userRefreshTokenRepository,
                            UserJwtSessionRepository userJwtSessionRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSecuritySettingRepository = userSecuritySettingRepository;
        this.userBranchMappingRepository = userBranchMappingRepository;
        this.otpService = otpService;
        this.rbacService = rbacService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRefreshTokenRepository = userRefreshTokenRepository;
        this.userJwtSessionRepository = userJwtSessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional("userTransactionManager")
    public OtpChallengeResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        logger.info("[Login Attempt] Initiating Step 1 authentication for email: {}", email);

        // 1. Locate user profile by email in u002
        UserProfile profile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("[Login Failed] No user profile associated with email: {}", email);
                    return new InvalidCredentialsException("Invalid email address or password");
                });

        // 2. Fetch credentials from u001
        User user = userRepository.findById(profile.getUserId())
                .orElseThrow(() -> {
                    logger.warn("[Login Failed] User ID {} found in profile but missing in u001", profile.getUserId());
                    return new InvalidCredentialsException("Invalid email address or password");
                });

        // 3. Verify account active status
        if (!Boolean.TRUE.equals(user.getIsActive()) || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            logger.warn("[Login Blocked] Inactive or suspended user ID {}. Status: '{}', IsActive: {}",
                    user.getId(), user.getStatus(), user.getIsActive());
            throw new AccountInactiveException("User account is inactive or suspended. Please contact your system administrator.");
        }

        // 4. Validate password length (UTF-8 byte length max 72 bytes) & verify with BCrypt
        if (request.getPassword() != null && request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new com.los.bank.user.exception.UserValidationException("Password must not exceed 72 bytes (in UTF-8 encoding)");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int currentBadLogins = user.getNoOfBadLogins() != null ? user.getNoOfBadLogins() : 0;
            user.setNoOfBadLogins(currentBadLogins + 1);
            userRepository.save(user);
            logger.warn("[Login Failed] Password mismatch for user ID {}. Bad logins count: {}", user.getId(), user.getNoOfBadLogins());
            throw new InvalidCredentialsException("Invalid email address or password");
        }

        // 5. Credentials verified: Trigger mandatory 2FA OTP
        String plainOtp = otpService.generateLoginOtp(user.getId());
        String tempToken = jwtTokenProvider.generateTempToken(user.getId(), profile.getEmail());

        logger.info("[Login Step 1 Successful] 2FA challenge dispatched for UserId: {}", user.getId());

        return OtpChallengeResponse.builder()
                .twoFactorRequired(true)
                .tempToken(tempToken)
                .maskedEmail(maskEmail(profile.getEmail()))
                .message("Two-factor authentication required. A 6-digit OTP has been generated.")
                .build();
    }

    @Override
    @Transactional("userTransactionManager")
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        logger.info("[2FA Verification] Validating OTP for email: {}", request.getEmail());

        // 1. Parse and validate the temporary token
        Claims claims = jwtTokenProvider.parseAndValidateToken(request.getTempToken());
        if (claims == null || !"2FA_PENDING".equals(claims.get("stage"))) {
            throw new OtpVerificationException("Invalid or expired 2FA verification session. Please initiate login again.");
        }

        Long userId = Long.valueOf(claims.getSubject());

        // 2. Verify OTP against u005
        boolean isOtpValid = otpService.verifyLoginOtp(userId, request.getOtp());
        if (!isOtpValid) {
            throw new OtpVerificationException("Invalid or expired OTP. Please enter the correct verification code.");
        }

        // 3. Load user, profile, security settings, branch mapping
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User profile not found"));
        Optional<UserSecuritySetting> securityOpt = userSecuritySettingRepository.findById(userId);
        List<UserBranchMapping> branchMappings = userBranchMappingRepository.findByUserId(userId);

        // 4. Resolve Dynamic Role from r001
        Integer roleId = profile.getRoleId();
        if (roleId == null && securityOpt.isPresent()) {
            roleId = securityOpt.get().getSystemRole();
        }

        Role role = null;
        if (roleId != null) {
            role = rbacService.getRoleById(roleId).orElse(null);
        }

        String roleName = role != null ? role.getName() : "Unknown";

        // 5. Resolve Dynamic Permissions from r003 & r002
        List<String> permissions = role != null ? rbacService.getPermissionsForRole(role.getId()) : Collections.emptyList();

        // 6. Branch resolution
        Long branchId = null;
        if (!branchMappings.isEmpty()) {
            branchId = branchMappings.get(0).getBranchId();
        } else if (securityOpt.isPresent() && securityOpt.get().getLoginBranchId() != null) {
            branchId = securityOpt.get().getLoginBranchId();
        }

        // 7. Generate JWT Access and Refresh Tokens
        String jti = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.generateAccessToken(
                userId, profile.getEmail(), roleName, roleId, branchId, permissions, jti
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // 8. Track session JTI in u008
        UserJwtSession session = UserJwtSession.builder()
                .jti(jti)
                .userId(userId)
                .lastSeen(LocalDateTime.now())
                .timeoutSecs((int) jwtTokenProvider.getAccessTokenValiditySeconds())
                .invalidated(false)
                .build();
        userJwtSessionRepository.save(session);

        // 9. Store Refresh Token hash in u007 using SHA-256 (arbitrary token length safe)
        UserRefreshToken tokenEntity = UserRefreshToken.builder()
                .userId(userId)
                .tokenHash(hashToken(refreshToken))
                .used(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        userRefreshTokenRepository.save(tokenEntity);

        // 10. Update last login details in u001
        user.setLastLoginDate(LocalDate.now());
        user.setLastLoginTime(LocalTime.now());
        user.setNoOfBadLogins(0);
        userRepository.save(user);

        logger.info("[2FA Verification Successful] Issued tokens for User: {} ({}) with Role: '{}'",
                user.getUsername(), profile.getEmail(), roleName);

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id(user.getId())
                .empNo(user.getEmpNo())
                .username(user.getUsername())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .designation(profile.getDesignation())
                .roleId(roleId)
                .roleName(roleName)
                .branchId(branchId)
                .permissions(permissions)
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValiditySeconds())
                .user(userSummary)
                .build();
    }

    @Override
    @Transactional("userTransactionManager")
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        Claims claims = jwtTokenProvider.parseAndValidateToken(request.getRefreshToken());
        if (claims == null || !"REFRESH".equals(claims.get("type"))) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        Long userId = Long.valueOf(claims.getSubject());
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User profile not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AccountInactiveException("User account is inactive");
        }

        Integer roleId = profile.getRoleId();
        Role role = roleId != null ? rbacService.getRoleById(roleId).orElse(null) : null;
        String roleName = role != null ? role.getName() : "Unknown";
        List<String> permissions = role != null ? rbacService.getPermissionsForRole(role.getId()) : Collections.emptyList();

        String jti = UUID.randomUUID().toString();
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                userId, profile.getEmail(), roleName, roleId, null, permissions, jti
        );
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        UserJwtSession session = UserJwtSession.builder()
                .jti(jti)
                .userId(userId)
                .lastSeen(LocalDateTime.now())
                .timeoutSecs((int) jwtTokenProvider.getAccessTokenValiditySeconds())
                .invalidated(false)
                .build();
        userJwtSessionRepository.save(session);

        UserRefreshToken tokenEntity = UserRefreshToken.builder()
                .userId(userId)
                .tokenHash(hashToken(newRefreshToken))
                .used(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        userRefreshTokenRepository.save(tokenEntity);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValiditySeconds())
                .user(UserSummaryDto.builder()
                        .id(user.getId())
                        .empNo(user.getEmpNo())
                        .username(user.getUsername())
                        .email(profile.getEmail())
                        .firstName(profile.getFirstName())
                        .lastName(profile.getLastName())
                        .roleId(roleId)
                        .roleName(roleName)
                        .permissions(permissions)
                        .build())
                .build();
    }

    @Override
    @Transactional("userTransactionManager")
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Claims claims = jwtTokenProvider.parseAndValidateToken(token);
            if (claims != null && claims.getId() != null) {
                userJwtSessionRepository.findById(claims.getId()).ifPresent(session -> {
                    session.setInvalidated(true);
                    userJwtSessionRepository.save(session);
                    logger.info("[Logout] Invalidated JTI session: {}", claims.getId());
                });
            }
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 2) {
            return username.charAt(0) + "***" + domain;
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + domain;
    }

    private String hashToken(String rawToken) {
        if (rawToken == null) {
            return null;
        }
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
}
