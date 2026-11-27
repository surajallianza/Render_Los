package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerReferenceRepository extends JpaRepository<CustomerReference, Long> {
    List<CustomerReference> findByCustomerIdOrderBySrNoAsc(Long customerId);
    void deleteByCustomerId(Long customerId);
}
