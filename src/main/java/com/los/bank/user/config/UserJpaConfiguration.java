package com.los.bank.user.config;

import com.los.bank.user.datasource.BankRoutingDataSource;
import com.los.bank.user.datasource.DynamicBankDataSourceManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Isolated JPA configuration for the User Module and Setup Module.
 * Defines both the Primary 'entityManagerFactory' for the setup master database
 * (bank_setup)
 * and the 'userEntityManagerFactory' bound to BankRoutingDataSource for dynamic
 * bank databases.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.los.bank.user.repository",
                "com.los.bank.login.repository",
                "com.los.bank.Otp.repository",
                "com.los.bank.Rbac.repository"
        },
        entityManagerFactoryRef = "userEntityManagerFactory",
        transactionManagerRef = "userTransactionManager"
)
public class UserJpaConfiguration {

    // =========================================================================
    // 1. SETUP / MASTER DATABASE ENTITY MANAGER FACTORY (bank_setup)
    // =========================================================================

    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("setupDataSource") DataSource setupDataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(setupDataSource);
        em.setPackagesToScan("com.los.bank.setup.entity");
        em.setPersistenceUnitName("setupMasterUnit");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.hbm2ddl.auto", "none");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProperties.put("hibernate.show_sql", false);
        jpaProperties.put("hibernate.format_sql", false);
        em.setJpaPropertyMap(jpaProperties);

        return em;
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    // =========================================================================
    // 2. USER ENTITY MANAGER FACTORY (Dynamic Bank Routing)
    // =========================================================================

    @Bean(name = "bankRoutingDataSource")
    public DataSource bankRoutingDataSource(DynamicBankDataSourceManager dataSourceManager) {
        return new BankRoutingDataSource(dataSourceManager);
    }

    @Bean(name = "userEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean userEntityManagerFactory(
            @Qualifier("bankRoutingDataSource") DataSource bankRoutingDataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(bankRoutingDataSource);
        em.setPackagesToScan(
                "com.los.bank.user.entity",
                "com.los.bank.login.entity",
                "com.los.bank.Otp.entity",
                "com.los.bank.Rbac.entity"
        );
        em.setPersistenceUnitName("userBankUnit");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.hbm2ddl.auto", "none");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProperties.put("hibernate.show_sql", false);
        jpaProperties.put("hibernate.format_sql", false);
        em.setJpaPropertyMap(jpaProperties);

        return em;
    }

    @Bean(name = "userTransactionManager")
    public PlatformTransactionManager userTransactionManager(
            @Qualifier("userEntityManagerFactory") EntityManagerFactory userEntityManagerFactory) {
        return new JpaTransactionManager(userEntityManagerFactory);
    }
}
