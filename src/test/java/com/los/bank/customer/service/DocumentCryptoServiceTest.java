package com.los.bank.customer.service;

import com.los.bank.customer.config.CustomerSecurityProperties;
import com.los.bank.customer.exception.DocumentStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocumentCryptoServiceTest {

    private CustomerSecurityProperties properties;
    private DocumentCryptoService cryptoService;

    private static final String MASTER_SECRET_V1 = "TestMasterSecretForBankLOSSecurity2026V1!";
    private static final String MASTER_SECRET_V2 = "TestMasterSecretForBankLOSSecurity2026V2Rotated!";
    private static final String TENANT_A = "bank_abc001";
    private static final String TENANT_B = "bank_xyz002";
    private static final String PATH_1 = "bank_abc001/CUS-001/aadhaar.pdf";
    private static final String PATH_2 = "bank_abc001/CUS-002/aadhaar.pdf";

    @BeforeEach
    void setUp() {
        properties = new CustomerSecurityProperties();
        properties.setMasterSecret(MASTER_SECRET_V1);
        properties.setDocumentKeyVersion(1);
        properties.setDocumentEncryptionEnabled(true);
        properties.setAllowLegacyPlaintextDocuments(false);

        cryptoService = new DocumentCryptoService(properties);
    }

    @Test
    @DisplayName("Round-trip: Encrypt and decrypt preserves identical plaintext")
    void testRoundTripEncryptDecrypt() {
        byte[] original = "%PDF-1.4\nSample bank customer verification document content...".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = cryptoService.encrypt(original, TENANT_A, PATH_1);
        assertNotNull(encrypted);
        assertTrue(cryptoService.isEncrypted(encrypted));
        assertEquals('E', (char) encrypted[0]);
        assertEquals('N', (char) encrypted[1]);
        assertEquals('C', (char) encrypted[2]);
        assertEquals('1', (char) encrypted[3]);
        assertEquals(1, encrypted[4]); // Key version 1

        byte[] decrypted = cryptoService.decrypt(encrypted, TENANT_A, PATH_1);
        assertArrayEquals(original, decrypted);
    }

    @Test
    @DisplayName("Requirement 1: Decryption with wrong tenant key fails authentication")
    void testWrongTenantFailsDecryption() {
        byte[] original = "Confidential customer KYC file content".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = cryptoService.encrypt(original, TENANT_A, PATH_1);

        DocumentStorageException ex = assertThrows(DocumentStorageException.class, () ->
                cryptoService.decrypt(encrypted, TENANT_B, PATH_1)
        );
        assertTrue(ex.getMessage().contains("decryption/authentication failed"));
    }

    @Test
    @DisplayName("Requirement 1: Encrypted file moved to another relative path fails AAD authentication")
    void testMovedFilePathFailsAad() {
        byte[] original = "Confidential customer KYC file content".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = cryptoService.encrypt(original, TENANT_A, PATH_1);

        // Attempting to decrypt at PATH_2 instead of original PATH_1 fails because AAD binds relativeStoragePath
        DocumentStorageException ex = assertThrows(DocumentStorageException.class, () ->
                cryptoService.decrypt(encrypted, TENANT_A, PATH_2)
        );
        assertTrue(ex.getMessage().contains("decryption/authentication failed"));
    }

    @Test
    @DisplayName("Tampered ciphertext payload fails GCM tag authentication")
    void testTamperedCiphertextFails() {
        byte[] original = "Sensitive passport scan bytes".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = cryptoService.encrypt(original, TENANT_A, PATH_1);

        // Flip one bit in the ciphertext / tag region
        byte[] tampered = Arrays.copyOf(encrypted, encrypted.length);
        tampered[20] ^= 0x01;

        DocumentStorageException ex = assertThrows(DocumentStorageException.class, () ->
                cryptoService.decrypt(tampered, TENANT_A, PATH_1)
        );
        assertTrue(ex.getMessage().contains("decryption/authentication failed"));
    }

    @Test
    @DisplayName("Requirement 3: Key version rotation handles both old and rotated keys seamlessly")
    void testKeyVersionHandlingAndRotation() {
        // 1. Encrypt document with Version 1
        byte[] docV1 = "Document encrypted under key version 1".getBytes(StandardCharsets.UTF_8);
        byte[] encryptedV1 = cryptoService.encrypt(docV1, TENANT_A, PATH_1);
        assertEquals(1, encryptedV1[4]); // Key version 1 in header

        // 2. Rotate to Version 2
        properties.setDocumentKeyVersion(2);
        properties.setMasterSecret(MASTER_SECRET_V2);
        Map<Integer, String> secretsMap = new HashMap<>();
        secretsMap.put(1, MASTER_SECRET_V1);
        secretsMap.put(2, MASTER_SECRET_V2);
        properties.setMasterSecrets(secretsMap);

        // Create updated service representing rotated state
        DocumentCryptoService rotatedCryptoService = new DocumentCryptoService(properties);

        // 3. Encrypt new document with Version 2
        byte[] docV2 = "Document encrypted under key version 2".getBytes(StandardCharsets.UTF_8);
        byte[] encryptedV2 = rotatedCryptoService.encrypt(docV2, TENANT_A, PATH_2);
        assertEquals(2, encryptedV2[4]); // Key version 2 in header

        // 4. Decrypt both V1 and V2 documents successfully
        byte[] decryptedV1 = rotatedCryptoService.decrypt(encryptedV1, TENANT_A, PATH_1);
        assertArrayEquals(docV1, decryptedV1);

        byte[] decryptedV2 = rotatedCryptoService.decrypt(encryptedV2, TENANT_A, PATH_2);
        assertArrayEquals(docV2, decryptedV2);

        // 5. Unknown key version (e.g. 99) fails cleanly
        byte[] unknownVersionDoc = Arrays.copyOf(encryptedV1, encryptedV1.length);
        unknownVersionDoc[4] = 99; // Non-existent key version
        DocumentStorageException ex = assertThrows(DocumentStorageException.class, () ->
                rotatedCryptoService.decrypt(unknownVersionDoc, TENANT_A, PATH_1)
        );
        assertTrue(ex.getMessage().contains("Unknown document key version: 99"));
    }

    @Test
    @DisplayName("Requirement 2: Legacy plaintext rejection vs fallback warning")
    void testLegacyPlaintextHandling() {
        byte[] plaintextFile = "%PDF-1.4\nLegacy unencrypted document".getBytes(StandardCharsets.UTF_8);

        // By default: allowLegacyPlaintextDocuments = false -> must reject with DocumentStorageException
        assertFalse(cryptoService.isEncrypted(plaintextFile));
        DocumentStorageException ex = assertThrows(DocumentStorageException.class, () ->
                cryptoService.decrypt(plaintextFile, TENANT_A, PATH_1)
        );
        assertTrue(ex.getMessage().contains("missing the required 'ENC1' encryption header"));

        // When explicitly enabled via config -> serves plaintext
        properties.setAllowLegacyPlaintextDocuments(true);
        byte[] served = cryptoService.decrypt(plaintextFile, TENANT_A, PATH_1);
        assertArrayEquals(plaintextFile, served);
    }

    @Test
    @DisplayName("Requirement 4: Document key and PII key derivation separation")
    void testKeyDerivationSeparation() {
        byte[] masterSecret = MASTER_SECRET_V1.getBytes(StandardCharsets.UTF_8);

        byte[] docKeyTenantA = cryptoService.deriveDocumentKey(masterSecret, TENANT_A);
        byte[] piiKeyTenantA = cryptoService.derivePiiKey(masterSecret, TENANT_A);
        byte[] docKeyTenantB = cryptoService.deriveDocumentKey(masterSecret, TENANT_B);

        assertEquals(32, docKeyTenantA.length);
        assertEquals(32, piiKeyTenantA.length);
        assertEquals(32, docKeyTenantB.length);

        // Document key and PII key for same tenant must NEVER be identical
        assertFalse(Arrays.equals(docKeyTenantA, piiKeyTenantA),
                "Document key and PII key must use domain separation and not be identical");

        // Document keys for different tenants must not be identical
        assertFalse(Arrays.equals(docKeyTenantA, docKeyTenantB),
                "Document keys for different tenants must be distinct");
    }
}
