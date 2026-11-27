package com.los.bank.loan.repository;

import com.los.bank.loan.entity.RetailLoanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for RetailLoanApplication (a001-a005) in the Bank database.
 */
@Repository
public interface RetailLoanApplicationRepository extends JpaRepository<RetailLoanApplication, Long> {

    Optional<RetailLoanApplication> findByApplicationNumber(String applicationNumber);

    boolean existsByApplicationNumber(String applicationNumber);

    boolean existsByApplicationNumberAndIdNot(String applicationNumber, Long id);

    @Query("SELECT a FROM RetailLoanApplication a WHERE " +
           "(:status IS NULL OR :status = '' OR LOWER(a.status) = LOWER(:status)) AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(a.productCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<RetailLoanApplication> searchApplications(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);
}
