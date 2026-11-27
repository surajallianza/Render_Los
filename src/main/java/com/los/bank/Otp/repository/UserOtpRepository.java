package com.los.bank.Otp.repository;

import com.los.bank.Otp.entity.UserOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserOtpRepository extends JpaRepository<UserOtp, Long> {

    Optional<UserOtp> findTopByUserIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime now);

    Optional<UserOtp> findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);
}
