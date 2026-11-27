package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerEmployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerEmploymentRepository extends JpaRepository<CustomerEmployment, Long> {
    Optional<CustomerEmployment> findByCustomerId(Long customerId);
}
