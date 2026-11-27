package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerBankAccountRepository extends JpaRepository<CustomerBankAccount, Long> {
    Optional<CustomerBankAccount> findByCustomerId(Long customerId);
}
