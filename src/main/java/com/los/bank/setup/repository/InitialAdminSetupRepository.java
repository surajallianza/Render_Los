package com.los.bank.setup.repository;

import com.los.bank.setup.entity.InitialAdminSetup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InitialAdminSetupRepository extends JpaRepository<InitialAdminSetup, Long> {
    Optional<InitialAdminSetup> findByBankId(Long bankId);
}

