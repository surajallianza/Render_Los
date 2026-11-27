package com.los.bank.user.repository;

import com.los.bank.user.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    List<UserToken> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE UserToken t SET t.used = true WHERE t.userId = :userId")
    void markAllTokensUsedForUser(@Param("userId") Long userId);
}
