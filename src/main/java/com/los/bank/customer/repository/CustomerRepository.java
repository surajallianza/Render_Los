package com.los.bank.customer.repository;

import com.los.bank.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerCode(String customerCode);

    boolean existsByCustomerCode(String customerCode);

    @Query(value = "SELECT nextval('seq_customer_code')", nativeQuery = true)
    Long getNextCustomerCodeSequence();
}
