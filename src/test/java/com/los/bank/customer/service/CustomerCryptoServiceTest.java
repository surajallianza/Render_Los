package com.los.bank.customer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomerCryptoServiceTest {

    private CustomerCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new CustomerCryptoService(
                "MyTestSecretKeyForAES256Encryption2026!",
                "MyTestSecretKeyForHmacSHA256Hashing2026!"
        );
    }

    @Test
    @DisplayName("AES-256 encrypt and decrypt round trip preserves plaintext")
    void testAesRoundTrip() {
        String plainText = "ABCDE1234F";
        byte[] encrypted = cryptoService.encrypt(plainText);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 12);

        String decrypted = cryptoService.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    @DisplayName("AES-256 encrypt returns null for null or empty string")
    void testAesNullOrEmpty() {
        assertNull(cryptoService.encrypt(null));
        assertNull(cryptoService.encrypt("   "));
        assertNull(cryptoService.decrypt(null));
        assertNull(cryptoService.decrypt(new byte[5]));
    }

    @Test
    @DisplayName("HMAC-SHA256 returns 64-char lowercase hex and is case-insensitive")
    void testHmacSha256() {
        String hash1 = cryptoService.computeHmacHex("abcde1234f");
        String hash2 = cryptoService.computeHmacHex("ABCDE1234F");

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2); // Case insensitive hashing for duplicates
    }

    @Test
    @DisplayName("Masking functions mask characters correctly")
    void testMasking() {
        assertEquals("ABCDE****F", cryptoService.maskPan("ABCDE1234F"));
        assertEquals("********9012", cryptoService.maskAadhaar("123456789012"));
        assertEquals("********9012", cryptoService.maskAccount("123456789012"));
        assertEquals("******9012", cryptoService.maskAccount("1234569012"));
        assertEquals("**********1234", cryptoService.maskCkyc("12345678901234"));
    }

    @Test
    @DisplayName("isMasked detects masked and unmasked strings accurately")
    void testIsMasked() {
        assertTrue(cryptoService.isMasked("ABCDE****F"));
        assertTrue(cryptoService.isMasked("********9012"));
        assertTrue(cryptoService.isMasked("******9012"));
        assertTrue(cryptoService.isMasked("**********1234"));

        assertFalse(cryptoService.isMasked("ABCDE1234F"));
        assertFalse(cryptoService.isMasked("123456789012"));
        assertFalse(cryptoService.isMasked(null));
    }
}
