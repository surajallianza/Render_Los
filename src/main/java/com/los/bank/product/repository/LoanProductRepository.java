package com.los.bank.product.repository;

import com.los.bank.product.entity.LoanProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for LoanProduct entity operations against the Bank database.
 */
@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    Optional<LoanProduct> findByCode(String code);

    Optional<LoanProduct> findByCodeIgnoreCase(String code);

    boolean existsByCode(String code);

    @Query("SELECT COUNT(p) > 0 FROM LoanProduct p WHERE LOWER(p.code) = LOWER(:code) AND p.id <> :id")
    boolean existsByCodeAndIdNot(@Param("code") String code, @Param("id") Long id);

    @Query("SELECT DISTINCT p FROM LoanProduct p LEFT JOIN FETCH p.delegationOfPower WHERE p.id = :id")
    Optional<LoanProduct> findByIdWithDelegations(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM LoanProduct p LEFT JOIN FETCH p.delegationOfPower WHERE LOWER(p.code) = LOWER(:code)")
    Optional<LoanProduct> findByCodeWithDelegations(@Param("code") String code);

    @Query("SELECT p FROM LoanProduct p WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<LoanProduct> searchProducts(@Param("search") String search, Pageable pageable);
}
