package com.los.bank.product.repository;

import com.los.bank.product.entity.ProductDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for child ProductDelegation entity operations.
 */
@Repository
public interface ProductDelegationRepository extends JpaRepository<ProductDelegation, Long> {

    List<ProductDelegation> findByLoanProductId(Long loanProductId);

    void deleteByLoanProductId(Long loanProductId);
}
