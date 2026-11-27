package com.los.bank.lead.repository;

import com.los.bank.lead.entity.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for Lead entity operations.
 */
@Repository
public interface LeadRepository extends JpaRepository<Lead, String> {

    /**
     * Searches leads with optional status and multi-field keyword search across
     * lead ID, applicant first name, last name, mobile number, and email.
     */
    @Query("SELECT l FROM Lead l WHERE " +
           "(:status IS NULL OR :status = '' OR LOWER(l.status) = LOWER(:status)) AND " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(l.leadId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(l.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(l.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " l.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
           " LOWER(l.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Lead> searchLeads(@Param("status") String status, @Param("search") String search, Pageable pageable);

    @Query(value = "SELECT COUNT(*) > 0 FROM u001 WHERE id = :userId", nativeQuery = true)
    boolean existsUserInBankDb(@Param("userId") Long userId);
}
