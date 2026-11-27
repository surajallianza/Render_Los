package com.los.bank.dbArchitecture;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import com.los.bank.setup.service.MasterDatabaseInitializer;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManagerFactory;

/**
 * Unified Database Architecture Configuration for Allianza LOS. Configures both
 * DataSources and their respective JPA Entity Managers: 1. Setup Database
 * (bank_setup) -> com.los.bank.setup.repository 2. Bank Database (Dynamic) ->
 * com.los.bank.lookup.repository and other core banking repositories
 */
@Configuration
public class DatabaseConfig {

    // ==========================================
    // 1. SETUP DATABASE DATASOURCE
    // ==========================================
    @Bean(name = "setupDataSource")
    @DependsOn("masterDatabaseInitializer")
    @ConfigurationProperties(prefix = "app.datasource.setup")
    public HikariDataSource setupDataSource(MasterDatabaseInitializer masterDatabaseInitializer) {
        masterDatabaseInitializer.initializeMasterDatabase();
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    // ==========================================
    // 2. DYNAMIC BANK DATASOURCE (For Lead Management & Core Banking)
    // ==========================================
    @Primary
    @Bean(name = {"bankDataSource", "leadDataSource", "dynamicBankDataSource"})
    public DynamicBankDataSource bankDataSource(
            @Qualifier("setupDataSource") DataSource setupDataSource,
            @Value("${DB_HOST:localhost}") String dbHost,
            @Value("${DB_PORT:5432}") int dbPort,
            @Value("${app.datasource.bank.username:${app.datasource.setup.username:postgres}}") String bankUsername,
            @Value("${app.datasource.bank.password:${app.datasource.setup.password:postgres}}") String bankPassword) {
        return new DynamicBankDataSource(
                setupDataSource,
                dbHost,
                dbPort,
                bankUsername,
                bankPassword);
    }

    // ==========================================
    // 3. SETUP JPA CONFIGURATION (bank_setup)
    // ==========================================
    @Configuration
    @EnableJpaRepositories(basePackages = "com.los.bank.setup.repository", entityManagerFactoryRef = "setupEntityManagerFactory", transactionManagerRef = "setupTransactionManager")
    public static class SetupJpaConfiguration {

        @Bean(name = "setupEntityManagerFactory")
        public LocalContainerEntityManagerFactoryBean setupEntityManagerFactory(
                @Qualifier("setupDataSource") DataSource setupDataSource) {
            LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
            em.setDataSource(setupDataSource);
            em.setPackagesToScan("com.los.bank.setup.entity");
            em.setPersistenceUnitName("setup");

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            em.setJpaVendorAdapter(vendorAdapter);

            Map<String, Object> properties = new HashMap<>();
            properties.put("hibernate.hbm2ddl.auto", "none");
            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            em.setJpaPropertyMap(properties);

            return em;
        }

        @Bean(name = "setupTransactionManager")
        public PlatformTransactionManager setupTransactionManager(
                @Qualifier("setupEntityManagerFactory") EntityManagerFactory setupEntityManagerFactory) {
            return new JpaTransactionManager(setupEntityManagerFactory);
        }
    }

    // ==========================================
    // 4. BANK JPA CONFIGURATION (Dynamic) — Lazy, initialized on demand
    // ==========================================
    @Lazy
    @Configuration
    @EnableJpaRepositories(basePackages = {
        "com.los.bank.lookup.repository",
        "com.los.bank.branch.repository",
        "com.los.bank.customer.repository",
        "com.los.bank.loan.repository",
        "com.los.bank.product.repository",
        "com.los.bank.goldrate.repository",
        "com.los.bank.rule.repository",
        "com.los.bank.interestrate.repository",
        "com.los.bank.bankmaster.repository"
    }, entityManagerFactoryRef = "bankEntityManagerFactory", transactionManagerRef = "bankTransactionManager")
    public static class BankJpaConfiguration {

        @Primary
        @Bean(name = "bankEntityManagerFactory")
        public LocalContainerEntityManagerFactoryBean bankEntityManagerFactory(
                @Qualifier("bankDataSource") DataSource bankDataSource) {
            LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
            em.setDataSource(bankDataSource);
            em.setPackagesToScan(
                    "com.los.bank.lookup.entity",
                    "com.los.bank.branch.entity",
                    "com.los.bank.customer.entity",
                    "com.los.bank.loan.entity",
                    "com.los.bank.product.entity",
                    "com.los.bank.goldrate.entity",
                    "com.los.bank.rule.entity",
                    "com.los.bank.interestrate.entity",
                    "com.los.bank.bankmaster.entity");

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            em.setJpaVendorAdapter(vendorAdapter);

            Map<String, Object> properties = new HashMap<>();
            properties.put("hibernate.hbm2ddl.auto", "none");
            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            em.setJpaPropertyMap(properties);

            return em;
        }

        @Primary
        @Bean(name = "bankTransactionManager")
        public PlatformTransactionManager bankTransactionManager(
                @Qualifier("bankEntityManagerFactory") EntityManagerFactory bankEntityManagerFactory) {
            return new JpaTransactionManager(bankEntityManagerFactory);
        }
    }
}
