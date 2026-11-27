package com.los.bank.customer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Security and encryption configuration properties for customer documents and PII.
 */
@Configuration
@ConfigurationProperties(prefix = "app.customer.security")
@Getter
@Setter
public class CustomerSecurityProperties {

    /**
     * Master secret from which tenant document and PII keys are derived via HMAC-SHA256.
     * Must come from an environment variable (CUSTOMER_MASTER_SECRET) or external configuration outside 'dev'.
     */
    private String masterSecret;

    /**
     * Secret key for AES-256 PII column encryption (legacy/direct key).
     */
    private String aesSecret;

    /**
     * Secret key for HMAC-SHA256 duplicate detection hashes.
     */
    private String hmacSecret;

    /**
     * Current active document key version (1 to 255). Default is 1.
     */
    private int documentKeyVersion = 1;

    /**
     * Controls whether new documents uploaded are encrypted at rest with AES-256-GCM. Default is true.
     * Outside the 'dev' profile, the application refuses to start if this is false.
     */
    private boolean documentEncryptionEnabled = true;

    /**
     * Controls whether legacy unencrypted plaintext documents (without ENC1 header) are served.
     * Default is false. When true, logs a warning on every legacy access.
     */
    private boolean allowLegacyPlaintextDocuments = false;

    /**
     * Versioned master secrets map for seamless key rotation.
     * E.g. app.customer.security.master-secrets.1=secret1, app.customer.security.master-secrets.2=secret2
     */
    private Map<Integer, String> masterSecrets = new HashMap<>();
}
