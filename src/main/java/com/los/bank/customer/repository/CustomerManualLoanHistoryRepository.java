package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerManualLoanHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerManualLoanHistoryRepository extends JpaRepository<CustomerManualLoanHistory, Long> {
    List<CustomerManualLoanHistory> findByCustomerId(Long customerId);
    void deleteByCustomerId(Long customerId);
}
