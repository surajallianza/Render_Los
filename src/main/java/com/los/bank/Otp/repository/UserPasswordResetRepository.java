package com.los.bank.Otp.repository;

import com.los.bank.Otp.entity.UserPasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPasswordResetRepository extends JpaRepository<UserPasswordReset, Long> {

    Optional<UserPasswordReset> findTopByUserIdAndAppliedFalseOrderByCreatedAtDesc(Long userId);
}
