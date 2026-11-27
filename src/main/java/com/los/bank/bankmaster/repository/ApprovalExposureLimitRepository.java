package com.los.bank.bankmaster.repository;

import com.los.bank.bankmaster.entity.ApprovalExposureLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalExposureLimitRepository extends JpaRepository<ApprovalExposureLimit, Long> {

    List<ApprovalExposureLimit> findByIsActiveTrueOrderByIdDesc();

    Optional<ApprovalExposureLimit> findByIdAndIsActiveTrue(Long id);

    List<ApprovalExposureLimit> findByBranchIdAndIsActiveTrue(Long branchId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM ApprovalExposureLimit a " +
           "WHERE LOWER(TRIM(a.approvalAuthority)) = LOWER(TRIM(:authority)) " +
           "AND (:branchId IS NULL AND a.branchId IS NULL OR a.branchId = :branchId) " +
           "AND a.isActive = true")
    boolean existsActiveAuthority(
            @Param("authority") String authority,
            @Param("branchId") Long branchId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM ApprovalExposureLimit a " +
           "WHERE LOWER(TRIM(a.approvalAuthority)) = LOWER(TRIM(:authority)) " +
           "AND (:branchId IS NULL AND a.branchId IS NULL OR a.branchId = :branchId) " +
           "AND a.id <> :id AND a.isActive = true")
    boolean existsActiveAuthorityExcludingId(
            @Param("authority") String authority,
            @Param("branchId") Long branchId,
            @Param("id") Long id);
}
