package com.los.bank.goldrate.repository;

import com.los.bank.goldrate.entity.GoldRateActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for GoldRateActivity entity.
 */
@Repository
public interface GoldRateActivityRepository extends JpaRepository<GoldRateActivity, Long> {

    List<GoldRateActivity> findByRecordIdOrderByActivityDateDescActivityTimeDesc(String recordId);

    List<GoldRateActivity> findByModuleOrderByActivityDateDescActivityTimeDesc(String module);

    List<GoldRateActivity> findByStatusOrderByActivityDateDescActivityTimeDesc(String status);

    List<GoldRateActivity> findAllByOrderByActivityDateDescActivityTimeDesc();
}
