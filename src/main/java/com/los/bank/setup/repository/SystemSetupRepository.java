package com.los.bank.setup.repository;

import com.los.bank.setup.entity.SystemSetup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSetupRepository extends JpaRepository<SystemSetup, Long> {
    Optional<SystemSetup> findByBankId(Long bankId);
}

