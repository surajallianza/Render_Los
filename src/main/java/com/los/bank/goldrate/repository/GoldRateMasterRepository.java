package com.los.bank.goldrate.repository;

import com.los.bank.goldrate.entity.GoldRateMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Spring Data JPA Repository for GoldRateMaster entity.
 */
@Repository
public interface GoldRateMasterRepository extends JpaRepository<GoldRateMaster, Long> {

    Optional<GoldRateMaster> findByRecordId(String recordId);

    Optional<GoldRateMaster> findByEffectiveDate(LocalDate effectiveDate);

    boolean existsByRecordId(String recordId);
}
