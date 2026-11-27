package com.los.bank.Otp.service;

import com.los.bank.Otp.entity.UserOtp;
import com.los.bank.Otp.repository.UserOtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserOtpRepository userOtpRepository;
    private final PasswordEncoder passwordEncoder;

    public OtpServiceImpl(UserOtpRepository userOtpRepository, PasswordEncoder passwordEncoder) {
        this.userOtpRepository = userOtpRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional("userTransactionManager")
    public String generateLoginOtp(Long userId) {
        // Generate secure 6-digit numeric OTP (100000 - 999999)
        int otpNumber = 100000 + SECURE_RANDOM.nextInt(900000);
        String plainOtp = String.valueOf(otpNumber);

        String otpHash = passwordEncoder.encode(plainOtp);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        UserOtp userOtp = UserOtp.builder()
                .userId(userId)
                .otpHash(otpHash)
                .used(false)
                .expiresAt(expiresAt)
                .build();

        userOtpRepository.save(userOtp);

        logger.info("[2FA OTP Generated] For UserId: {}, OTP: {} (Expires at: {})", userId, plainOtp, expiresAt);
        return plainOtp;
    }

    @Override
    @Transactional("userTransactionManager")
    public boolean verifyLoginOtp(Long userId, String plainOtp) {
        if (plainOtp == null || plainOtp.trim().isEmpty()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<UserOtp> otpOpt = userOtpRepository
                .findTopByUserIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(userId, now);

        if (otpOpt.isEmpty()) {
            logger.warn("[2FA OTP Verification Failed] No valid, unexpired OTP found for UserId: {}", userId);
            return false;
        }

        UserOtp userOtp = otpOpt.get();
        boolean matches = passwordEncoder.matches(plainOtp.trim(), userOtp.getOtpHash());

        if (matches) {
            userOtp.setUsed(true);
            userOtpRepository.save(userOtp);
            logger.info("[2FA OTP Verification Succeeded] OTP verified and invalidated for UserId: {}", userId);
            return true;
        }

        logger.warn("[2FA OTP Verification Failed] OTP mismatch for UserId: {}", userId);
        return false;
    }
}
