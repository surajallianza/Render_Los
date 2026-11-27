package com.los.bank.setup.repository;

import com.los.bank.setup.entity.SetupActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SetupActivityLogRepository extends JpaRepository<SetupActivityLog, Long> {
    List<SetupActivityLog> findByBankIdOrderByCreatedAtAsc(Long bankId);
}

