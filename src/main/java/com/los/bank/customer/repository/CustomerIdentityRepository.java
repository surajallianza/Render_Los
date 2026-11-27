package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerIdentityRepository extends JpaRepository<CustomerIdentity, Long> {

    Optional<CustomerIdentity> findByCustomerId(Long customerId);

    boolean existsByPanHash(String panHash);

    boolean existsByPanHashAndCustomerIdNot(String panHash, Long customerId);

    boolean existsByAadhaarHash(String aadhaarHash);

    boolean existsByAadhaarHashAndCustomerIdNot(String aadhaarHash, Long customerId);
}
