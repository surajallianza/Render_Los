package com.los.bank.interestrate.repository;

import com.los.bank.interestrate.entity.InterestRateMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface InterestRateMasterRepository extends JpaRepository<InterestRateMaster, Long> {

    /**
     * The sheet that applies on {@code date}: latest APPROVED sheet effective on or before it.
     */
    Optional<InterestRateMaster> findFirstByProductIdAndStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            Long productId, String status, LocalDate date);

    /**
     * Duplicate check: another live (not rejected) sheet for the same product and date.
     */
    @Query("SELECT COUNT(m) > 0 FROM InterestRateMaster m WHERE m.productId = :productId " +
           "AND m.effectiveDate = :effectiveDate AND m.status <> 'REJECTED' " +
           "AND (:excludeId IS NULL OR m.id <> :excludeId)")
    boolean existsLiveSheet(@Param("productId") Long productId,
                            @Param("effectiveDate") LocalDate effectiveDate,
                            @Param("excludeId") Long excludeId);

    @Query("SELECT m FROM InterestRateMaster m WHERE " +
           "(:productId IS NULL OR m.productId = :productId) AND " +
           "(:status IS NULL OR m.status = :status)")
    Page<InterestRateMaster> search(@Param("productId") Long productId,
                                    @Param("status") String status,
                                    Pageable pageable);
}
