package com.los.bank.setup.repository;

import com.los.bank.setup.entity.DatabaseConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatabaseConfigurationRepository extends JpaRepository<DatabaseConfiguration, Long> {
    Optional<DatabaseConfiguration> findByBankId(Long bankId);
    Optional<DatabaseConfiguration> findByDatabaseName(String databaseName);
}

