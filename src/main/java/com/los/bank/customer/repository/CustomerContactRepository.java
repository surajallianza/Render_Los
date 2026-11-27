package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerContactRepository extends JpaRepository<CustomerContact, Long> {

    Optional<CustomerContact> findByCustomerId(Long customerId);

    Optional<CustomerContact> findByPhone(String phone);

    Optional<CustomerContact> findByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndCustomerIdNot(String phone, Long customerId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndCustomerIdNot(String email, Long customerId);
}
