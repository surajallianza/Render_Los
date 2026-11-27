package com.los.bank.lead.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Dedicated Spring Data JPA configuration for the Lead Module.
 * Connects directly to the dynamically resolved Bank Database via 'leadDataSource'
 * provided by the centralized Database Architecture module (DatabaseConfig).
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.los.bank.lead.repository", entityManagerFactoryRef = "leadEntityManagerFactory", transactionManagerRef = "leadTransactionManager")
public class LeadJpaConfig {

    @Bean(name = "leadEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean leadEntityManagerFactory(
            @Qualifier("leadDataSource") DataSource leadDataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(leadDataSource);
        em.setPackagesToScan("com.los.bank.lead.entity");
        em.setPersistenceUnitName("lead");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean(name = "leadTransactionManager")
    public PlatformTransactionManager leadTransactionManager(
            @Qualifier("leadEntityManagerFactory") EntityManagerFactory leadEntityManagerFactory) {
        return new JpaTransactionManager(leadEntityManagerFactory);
    }
}
