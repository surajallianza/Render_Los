package com.los.bank.customer.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Validates customer security requirements on application startup.
 * Enforces that outside the 'dev' profile:
 * 1. Master secret MUST be configured via environment variable (CUSTOMER_MASTER_SECRET) or external config.
 * 2. Document encryption at rest CANNOT be disabled.
 */
@Component
public class CustomerSecurityStartupValidator {

    private static final Logger logger = LoggerFactory.getLogger(CustomerSecurityStartupValidator.class);
    public static final String DEV_FALLBACK_MASTER_SECRET = "DevMasterSecret2026!BankLOS_Key99_DoNotUseInProd";
    public static final String DEV_FALLBACK_AES_SECRET = "BankCustomerSecretKey2026AES256!";
    public static final String DEV_FALLBACK_HMAC_SECRET = "BankCustomerHmacKey2026SHA256Secret!";

    private final Environment environment;
    private final CustomerSecurityProperties properties;

    public CustomerSecurityStartupValidator(Environment environment, CustomerSecurityProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @PostConstruct
    public void validateSecurityConfiguration() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isDev = activeProfiles.contains("dev") ||
                (activeProfiles.isEmpty() && Arrays.asList(environment.getDefaultProfiles()).contains("dev"));

        String masterSecret = properties.getMasterSecret();

        if (!isDev) {
            // Production / Staging / Non-Dev profile validation
            if (masterSecret == null || masterSecret.trim().isEmpty() || DEV_FALLBACK_MASTER_SECRET.equals(masterSecret)) {
                throw new IllegalStateException(
                        "CRITICAL SECURITY CONFIGURATION ERROR: Master encryption secret is missing! " +
                        "In non-dev profiles, 'app.customer.security.master-secret' must be provided via external configuration " +
                        "or environment variable 'CUSTOMER_MASTER_SECRET'. The application cannot start without an external master secret.");
            }

            if (!properties.isDocumentEncryptionEnabled()) {
                throw new IllegalStateException(
                        "CRITICAL SECURITY CONFIGURATION ERROR: Document encryption at rest is disabled! " +
                        "In non-dev profiles, 'app.customer.security.document-encryption-enabled' cannot be set to false. " +
                        "Document encryption is mandatory for production environments.");
            }
            logger.info("Customer security configuration validated successfully for non-dev profile(s): {}", activeProfiles);
        } else {
            // Dev profile fallback handling
            if (masterSecret == null || masterSecret.trim().isEmpty()) {
                logger.warn("[SECURITY WARNING] Running in 'dev' profile with no external master secret configured. " +
                        "Using development fallback master secret. NEVER use this configuration in production!");
                properties.setMasterSecret(DEV_FALLBACK_MASTER_SECRET);
            }
            if (properties.getAesSecret() == null || properties.getAesSecret().trim().isEmpty()) {
                properties.setAesSecret(DEV_FALLBACK_AES_SECRET);
            }
            if (properties.getHmacSecret() == null || properties.getHmacSecret().trim().isEmpty()) {
                properties.setHmacSecret(DEV_FALLBACK_HMAC_SECRET);
            }
        }
    }
}
