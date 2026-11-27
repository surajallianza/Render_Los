package com.los.bank.customer.repository;

import com.los.bank.customer.entity.CustomerProfileView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileViewRepository extends JpaRepository<CustomerProfileView, Long>, JpaSpecificationExecutor<CustomerProfileView> {
    Optional<CustomerProfileView> findByCustomerCode(String customerCode);
}
