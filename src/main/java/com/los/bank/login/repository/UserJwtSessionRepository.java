package com.los.bank.login.repository;

import com.los.bank.login.entity.UserJwtSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJwtSessionRepository extends JpaRepository<UserJwtSession, String> {

    Optional<UserJwtSession> findByJtiAndInvalidatedFalse(String jti);
}
