package com.los.bank.login.repository;

import com.los.bank.login.entity.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {

    Optional<UserRefreshToken> findTopByUserIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime now);
}
