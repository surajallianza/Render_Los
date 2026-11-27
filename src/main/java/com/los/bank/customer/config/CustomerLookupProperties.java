package com.los.bank.customer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Optional override configuration for customer lookups.
 * Lookups are resolved dynamically by category name from the database (m001 table).
 * These fields can be used as optional overrides if needed, but have no hardcoded defaults.
 */
@Configuration
@ConfigurationProperties(prefix = "app.customer.lookups")
@Getter
@Setter
public class CustomerLookupProperties {

    private String title;
    private String gender;
    private String maritalStatus;
    private String customerType;
    private String kycStatus;
    private String residentialStatus;
    private String residentialType;
    private String state;
    private String employmentType;
    private String employerType;
    private String listedStatus;
    private String incomeTaxFilerStatus;
    private String riskCategory;
    private String bankAccountType;
    private String bankAccountStatus;
    private String documentType;
}
