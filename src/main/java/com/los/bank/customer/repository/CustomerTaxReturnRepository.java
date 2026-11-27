package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerTaxReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerTaxReturnRepository extends JpaRepository<CustomerTaxReturn, Long> {
    List<CustomerTaxReturn> findByCustomerIdOrderByFinancialYearDesc(Long customerId);
    void deleteByCustomerId(Long customerId);
}
