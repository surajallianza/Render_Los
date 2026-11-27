package com.los.bank.setup.repository;

import com.los.bank.setup.entity.BankInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankInformationRepository extends JpaRepository<BankInformation, Long> {
    Optional<BankInformation> findByBankCode(String bankCode);
    boolean existsByBankCode(String bankCode);
    Optional<BankInformation> findByBankNameIgnoreCase(String bankName);
    boolean existsByBankNameIgnoreCase(String bankName);
}

