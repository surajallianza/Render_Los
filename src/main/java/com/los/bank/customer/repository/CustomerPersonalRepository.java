package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerPersonal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerPersonalRepository extends JpaRepository<CustomerPersonal, Long> {

    Optional<CustomerPersonal> findByCustomerId(Long customerId);

    Optional<CustomerPersonal> findByLeadId(String leadId);

    boolean existsByLeadId(String leadId);

    boolean existsByLeadIdAndCustomerIdNot(String leadId, Long customerId);
}
