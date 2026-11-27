package com.los.bank.customer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class CustomerSecurityStartupValidatorTest {

    @Test
    @DisplayName("Requirement 5: Startup FAILS in non-dev profile when master secret is missing")
    void testNonDevProfileRefusesStartWhenMasterSecretMissing() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        CustomerSecurityProperties props = new CustomerSecurityProperties();
        props.setMasterSecret(null);
        props.setDocumentEncryptionEnabled(true);

        CustomerSecurityStartupValidator validator = new CustomerSecurityStartupValidator(env, props);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateSecurityConfiguration);
        assertTrue(ex.getMessage().contains("Master encryption secret is missing"));
        assertTrue(ex.getMessage().contains("CUSTOMER_MASTER_SECRET"));
    }

    @Test
    @DisplayName("Requirement 9: Startup FAILS in non-dev profile when document encryption is disabled")
    void testNonDevProfileRefusesStartWhenDocumentEncryptionDisabled() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");

        CustomerSecurityProperties props = new CustomerSecurityProperties();
        props.setMasterSecret("ProductionSecret2026MasterKey!ProperlySet");
        props.setDocumentEncryptionEnabled(false); // Disabled in prod!

        CustomerSecurityStartupValidator validator = new CustomerSecurityStartupValidator(env, props);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateSecurityConfiguration);
        assertTrue(ex.getMessage().contains("Document encryption at rest is disabled"));
    }

    @Test
    @DisplayName("Startup SUCCEEDS in non-dev profile when master secret and encryption are properly configured")
    void testNonDevProfileStartsSuccessfullyWhenConfigured() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("staging");

        CustomerSecurityProperties props = new CustomerSecurityProperties();
        props.setMasterSecret("StagingMasterSecretKey2026ValidLongSecureKey!");
        props.setDocumentEncryptionEnabled(true);

        CustomerSecurityStartupValidator validator = new CustomerSecurityStartupValidator(env, props);

        assertDoesNotThrow(validator::validateSecurityConfiguration);
    }

    @Test
    @DisplayName("Dev profile applies fallback master secret and starts successfully when secret is omitted")
    void testDevProfileAppliesDevFallback() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        CustomerSecurityProperties props = new CustomerSecurityProperties();
        props.setMasterSecret(null);
        props.setDocumentEncryptionEnabled(true);

        CustomerSecurityStartupValidator validator = new CustomerSecurityStartupValidator(env, props);

        assertDoesNotThrow(validator::validateSecurityConfiguration);
        assertNotNull(props.getMasterSecret());
        assertEquals(CustomerSecurityStartupValidator.DEV_FALLBACK_MASTER_SECRET, props.getMasterSecret());
    }
}
