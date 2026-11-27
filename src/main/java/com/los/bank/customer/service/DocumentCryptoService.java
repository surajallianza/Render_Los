package com.los.bank.customer.service;

import com.los.bank.customer.config.CustomerSecurityProperties;
import com.los.bank.customer.exception.DocumentStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for encrypting and decrypting customer documents at rest.
 * Uses AES-256-GCM with:
 * - 4-byte Magic Header: "ENC1"
 * - 1-byte Key Version (1 to 255)
 * - 12-byte random IV/Nonce
 * - GCM Additional Authenticated Data (AAD) binding (tenant + "|" + relativeStoragePath)
 * - 16-byte GCM authentication tag
 */
@Service
@org.springframework.context.annotation.DependsOn("customerSecurityStartupValidator")
public class DocumentCryptoService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentCryptoService.class);

    public static final byte[] MAGIC_HEADER = new byte[]{'E', 'N', 'C', '1'};
    public static final int MAGIC_LENGTH = 4;
    public static final int VERSION_LENGTH = 1;
    public static final int GCM_IV_LENGTH = 12;
    public static final int HEADER_LENGTH = MAGIC_LENGTH + VERSION_LENGTH + GCM_IV_LENGTH; // 17 bytes
    public static final int GCM_TAG_LENGTH_BITS = 128;
    public static final int GCM_TAG_LENGTH_BYTES = 16;
    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final CustomerSecurityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<Integer, byte[]> versionedMasterSecrets = new HashMap<>();

    public DocumentCryptoService(CustomerSecurityProperties properties) {
        this.properties = properties;
        initializeKeys();
    }

    private void initializeKeys() {
        int activeVersion = properties.getDocumentKeyVersion();
        String activeSecret = properties.getMasterSecret();
        if (activeSecret != null && !activeSecret.trim().isEmpty()) {
            versionedMasterSecrets.put(activeVersion, activeSecret.trim().getBytes(StandardCharsets.UTF_8));
        }

        // Add any additional historical/rotated keys configured
        if (properties.getMasterSecrets() != null) {
            properties.getMasterSecrets().forEach((ver, sec) -> {
                if (sec != null && !sec.trim().isEmpty()) {
                    versionedMasterSecrets.put(ver, sec.trim().getBytes(StandardCharsets.UTF_8));
                }
            });
        }
    }

    /**
     * Encrypts plaintext document bytes using AES-256-GCM.
     * Header format: "ENC1" (4B) + keyVersion (1B) + IV (12B) + ciphertext with tag (16B).
     */
    public byte[] encrypt(byte[] plaintext, String tenantCode, String relativeStoragePath) {
        if (plaintext == null || plaintext.length == 0) {
            return plaintext;
        }

        int version = properties.getDocumentKeyVersion();
        byte[] masterSecret = getMasterSecretForVersion(version);
        if (masterSecret == null) {
            throw new DocumentStorageException("Master secret not configured for document key version " + version);
        }
        byte[] docKey = deriveDocumentKey(masterSecret, tenantCode);

        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(docKey, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // Bind tenant and relative path as Additional Authenticated Data (AAD)
            byte[] aad = buildAad(tenantCode, relativeStoragePath);
            cipher.updateAAD(aad);

            byte[] cipherWithTag = cipher.doFinal(plaintext);

            // Assemble output: ENC1 (4) + version (1) + IV (12) + cipherWithTag
            ByteBuffer buffer = ByteBuffer.allocate(HEADER_LENGTH + cipherWithTag.length);
            buffer.put(MAGIC_HEADER);
            buffer.put((byte) (version & 0xFF));
            buffer.put(iv);
            buffer.put(cipherWithTag);

            return buffer.array();
        } catch (Exception e) {
            logger.error("Document encryption failed for path '{}', tenant '{}': {}", relativeStoragePath, tenantCode, e.getMessage(), e);
            throw new DocumentStorageException("Failed to encrypt document for storage: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts file bytes using AES-256-GCM.
     * Verifies ENC1 header, extracts key version, derives tenant document key, binds AAD,
     * and verifies GCM authentication tag before returning plaintext.
     */
    public byte[] decrypt(byte[] fileBytes, String tenantCode, String relativeStoragePath) {
        if (fileBytes == null || fileBytes.length == 0) {
            return fileBytes;
        }

        // Check for ENC1 header
        if (!isEncrypted(fileBytes)) {
            if (properties.isAllowLegacyPlaintextDocuments()) {
                logger.warn("[SECURITY WARNING] Serving unencrypted legacy plaintext document at relative path '{}' for tenant '{}'",
                        relativeStoragePath, tenantCode);
                return fileBytes;
            } else {
                throw new DocumentStorageException(
                        "Document at '" + relativeStoragePath + "' is missing the required 'ENC1' encryption header. " +
                        "Unencrypted legacy documents are disabled by security policy.");
            }
        }

        if (fileBytes.length < HEADER_LENGTH + GCM_TAG_LENGTH_BYTES) {
            throw new DocumentStorageException("Encrypted document at '" + relativeStoragePath + "' is truncated or corrupted (size: " + fileBytes.length + " bytes).");
        }

        int keyVersion = fileBytes[MAGIC_LENGTH] & 0xFF;
        byte[] masterSecret = getMasterSecretForVersion(keyVersion);
        if (masterSecret == null) {
            throw new DocumentStorageException("Unknown document key version: " + keyVersion + " for document at '" + relativeStoragePath + "'. Master key not found.");
        }

        byte[] docKey = deriveDocumentKey(masterSecret, tenantCode);

        ByteBuffer buffer = ByteBuffer.wrap(fileBytes);
        buffer.position(HEADER_LENGTH - GCM_IV_LENGTH); // Skip ENC1 (4) + keyVersion (1) -> offset 5
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);

        byte[] cipherWithTag = new byte[buffer.remaining()];
        buffer.get(cipherWithTag);

        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(docKey, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // Bind expected tenant and relative storage path as AAD
            byte[] aad = buildAad(tenantCode, relativeStoragePath);
            cipher.updateAAD(aad);

            // Decrypt and verify GCM authentication tag
            return cipher.doFinal(cipherWithTag);
        } catch (Exception e) {
            logger.error("Document decryption/authentication failed for path '{}', tenant '{}': {}", relativeStoragePath, tenantCode, e.getMessage());
            throw new DocumentStorageException(
                    "Document decryption/authentication failed for '" + relativeStoragePath +
                    "'. The file may be corrupted, tampered with, or moved from another tenant/path: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if the byte array starts with the "ENC1" magic header.
     */
    public boolean isEncrypted(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length < MAGIC_LENGTH) {
            return false;
        }
        return fileBytes[0] == MAGIC_HEADER[0] &&
               fileBytes[1] == MAGIC_HEADER[1] &&
               fileBytes[2] == MAGIC_HEADER[2] &&
               fileBytes[3] == MAGIC_HEADER[3];
    }

    /**
     * Derives a 256-bit AES key for the document module using HMAC-SHA256 with label "doc|<tenantCode>".
     */
    public byte[] deriveDocumentKey(byte[] masterSecret, String tenantCode) {
        String label = "doc|" + (tenantCode != null && !tenantCode.trim().isEmpty() ? tenantCode.trim() : "default");
        return hmacSha256(masterSecret, label);
    }

    /**
     * Derives a 256-bit AES key for PII fields using HMAC-SHA256 with label "pii|<tenantCode>".
     */
    public byte[] derivePiiKey(byte[] masterSecret, String tenantCode) {
        String label = "pii|" + (tenantCode != null && !tenantCode.trim().isEmpty() ? tenantCode.trim() : "default");
        return hmacSha256(masterSecret, label);
    }

    /**
     * Registers or overrides a versioned master secret in memory (useful for dynamic key rotation and testing).
     */
    public void registerKeyVersion(int version, String secret) {
        if (secret != null && !secret.trim().isEmpty()) {
            versionedMasterSecrets.put(version, secret.trim().getBytes(StandardCharsets.UTF_8));
        }
    }

    public byte[] getMasterSecretForVersion(int version) {
        byte[] secret = versionedMasterSecrets.get(version);
        if (secret != null) {
            return secret;
        }
        if (properties.getMasterSecrets() != null && properties.getMasterSecrets().containsKey(version)) {
            String sec = properties.getMasterSecrets().get(version);
            if (sec != null && !sec.trim().isEmpty()) {
                secret = sec.trim().getBytes(StandardCharsets.UTF_8);
                versionedMasterSecrets.put(version, secret);
                return secret;
            }
        }
        if (version == properties.getDocumentKeyVersion()) {
            String activeSecret = properties.getMasterSecret();
            if (activeSecret != null && !activeSecret.trim().isEmpty()) {
                secret = activeSecret.trim().getBytes(StandardCharsets.UTF_8);
                versionedMasterSecrets.put(version, secret);
                return secret;
            }
        }
        return null;
    }

    private byte[] buildAad(String tenantCode, String relativeStoragePath) {
        String tenant = tenantCode != null && !tenantCode.trim().isEmpty() ? tenantCode.trim() : "default";
        String normalizedPath = relativeStoragePath != null ? relativeStoragePath.replace('\\', '/').trim() : "";
        String aad = tenant + "|" + normalizedPath;
        return aad.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] hmacSha256(byte[] key, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 failed: " + e.getMessage(), e);
        }
    }
}
