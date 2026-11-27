package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerIncome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerIncomeRepository extends JpaRepository<CustomerIncome, Long> {
    Optional<CustomerIncome> findByCustomerId(Long customerId);
}
