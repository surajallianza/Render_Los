package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerPreviousEmployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerPreviousEmploymentRepository extends JpaRepository<CustomerPreviousEmployment, Long> {
    List<CustomerPreviousEmployment> findByCustomerId(Long customerId);
    void deleteByCustomerId(Long customerId);
}
