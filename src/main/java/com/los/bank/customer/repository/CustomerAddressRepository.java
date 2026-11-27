package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    Optional<CustomerAddress> findByCustomerId(Long customerId);
}
