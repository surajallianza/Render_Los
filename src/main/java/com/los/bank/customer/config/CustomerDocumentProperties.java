package com.los.bank.customer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Document rules for the customer onboarding flow (prefix app.customer.documents).
 * Codes are Document Type sub lookup codes (m002.sub_type_code).
 */
@Configuration
@ConfigurationProperties(prefix = "app.customer.documents")
@Getter
@Setter
public class CustomerDocumentProperties {

    /**
     * Document types that must have one active file before the onboarding can be submitted.
     */
    private List<String> requiredTypes = new ArrayList<>();

    /**
     * Document types that may have several active files per customer (for example salary slips).
     * Uploading a type NOT in this list replaces (soft deletes) the customer's previous file of that type.
     */
    private List<String> multipleAllowedTypes = new ArrayList<>();

    /**
     * Minimum number of active documents (of any type) needed to submit the onboarding.
     */
    private int minCount = 1;
}
