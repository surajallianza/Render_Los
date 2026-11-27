package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerFamily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerFamilyRepository extends JpaRepository<CustomerFamily, Long> {
    Optional<CustomerFamily> findByCustomerId(Long customerId);
}
