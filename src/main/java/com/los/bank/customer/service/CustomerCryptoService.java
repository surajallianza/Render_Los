package com.los.bank.customer.service;

import com.los.bank.customer.config.CustomerSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Service for AES-256 encryption of sensitive PII columns at rest,
 * HMAC-SHA256 hashing for duplicate detection, and masking/unmasking.
 * Keys are derived from the master secret using domain-separated labels.
 */
@Service
@org.springframework.context.annotation.DependsOn("customerSecurityStartupValidator")
public class CustomerCryptoService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerCryptoService.class);
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] aesKeyBytes;
    private final byte[] hmacKeyBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public CustomerCryptoService(
            CustomerSecurityProperties properties,
            @Value("${app.customer.security.aes-secret:}") String legacyAesSecret,
            @Value("${app.customer.security.hmac-secret:}") String legacyHmacSecret) {

        String masterSecret = properties.getMasterSecret();
        if (masterSecret != null && !masterSecret.trim().isEmpty()) {
            // Requirement 4: Derive separate key with HMAC-SHA256 using label "pii"
            this.aesKeyBytes = deriveKeyWithHmac(masterSecret.trim(), "pii");
            this.hmacKeyBytes = deriveKeyWithHmac(masterSecret.trim(), "hmac");
        } else if (legacyAesSecret != null && !legacyAesSecret.trim().isEmpty()) {
            this.aesKeyBytes = deriveKey(legacyAesSecret.trim(), 32);
            this.hmacKeyBytes = (legacyHmacSecret != null && !legacyHmacSecret.trim().isEmpty())
                    ? legacyHmacSecret.trim().getBytes(StandardCharsets.UTF_8)
                    : deriveKeyWithHmac(legacyAesSecret.trim(), "hmac");
        } else {
            // Development fallback
            this.aesKeyBytes = deriveKey("BankCustomerSecretKey2026AES256!", 32);
            this.hmacKeyBytes = "BankCustomerHmacKey2026SHA256Secret!".getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Direct constructor for standalone unit tests.
     */
    public CustomerCryptoService(String aesSecret, String hmacSecret) {
        this.aesKeyBytes = deriveKey(aesSecret, 32);
        this.hmacKeyBytes = hmacSecret.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] deriveKeyWithHmac(String secret, String label) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(label.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive PII key: " + e.getMessage(), e);
        }
    }

    private static byte[] deriveKey(String secret, int length) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            byte[] key = new byte[length];
            System.arraycopy(hash, 0, key, 0, Math.min(hash.length, length));
            return key;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Encrypts plaintext using AES-256-GCM. Returns IV prepended to ciphertext.
     */
    public byte[] encrypt(String plaintext) {
        if (plaintext == null || plaintext.trim().isEmpty()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(aesKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] cipherBytes = cipher.doFinal(plaintext.trim().getBytes(StandardCharsets.UTF_8));
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherBytes);
            return byteBuffer.array();
        } catch (Exception e) {
            logger.error("Encryption error: {}", e.getMessage());
            throw new RuntimeException("Failed to encrypt sensitive data", e);
        }
    }

    /**
     * Decrypts ciphertext byte array using AES-256-GCM.
     */
    public String decrypt(byte[] encryptedBytes) {
        if (encryptedBytes == null || encryptedBytes.length <= GCM_IV_LENGTH) {
            return null;
        }
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(aesKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Decryption error: {}", e.getMessage());
            throw new RuntimeException("Failed to decrypt sensitive data", e);
        }
    }

    /**
     * Computes HMAC-SHA256 hex string for duplicate checks.
     */
    public String computeHmacHex(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(hmacKeyBytes, HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(value.trim().toUpperCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            logger.error("HMAC error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute HMAC hash", e);
        }
    }

    /**
     * Masks PAN (first 5 + 4 asterisks + last 1 char).
     */
    public String maskPan(String pan) {
        if (pan == null || pan.length() < 10) {
            return pan;
        }
        return pan.substring(0, 5) + "****" + pan.substring(9);
    }

    /**
     * Masks Aadhaar (first 8 asterisks + last 4 digits).
     */
    public String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 12) {
            return aadhaar;
        }
        return "********" + aadhaar.substring(aadhaar.length() - 4);
    }

    /**
     * Masks Account Number (asterisks except last 4 digits).
     */
    public String maskAccount(String account) {
        if (account == null || account.length() < 4) {
            return account;
        }
        int maskLength = account.length() - 4;
        return "*".repeat(maskLength) + account.substring(maskLength);
    }

    /**
     * Masks CKYC Number (asterisks except last 4 digits).
     */
    public String maskCkyc(String ckyc) {
        if (ckyc == null || ckyc.length() < 4) {
            return ckyc;
        }
        int maskLength = ckyc.length() - 4;
        return "*".repeat(maskLength) + ckyc.substring(maskLength);
    }

    /**
     * Checks if a field value appears to be masked (contains '*').
     */
    public boolean isMasked(String value) {
        return value != null && value.contains("*");
    }
}
