package com.los.bank.customer.service;

import com.los.bank.customer.exception.DocumentValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceImplTest {

    @TempDir
    Path tempUploadDir;

    private LocalStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageServiceImpl(tempUploadDir.toString());
    }

    @Test
    @DisplayName("Storage folder defaults to <user home>/uploads/customer-documents when not configured")
    void testRootLocationDefaultsToUserHome() {
        Path expected = Path.of(System.getProperty("user.home"), "uploads", "customer-documents").toAbsolutePath().normalize();
        assertEquals(expected, LocalStorageServiceImpl.resolveRootLocation(null));
        assertEquals(expected, LocalStorageServiceImpl.resolveRootLocation("   "));
    }

    @Test
    @DisplayName("Storage folder starting with ~ is resolved against the user's home folder")
    void testRootLocationTildeExpansion() {
        Path home = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        assertEquals(home.resolve("los-files"), LocalStorageServiceImpl.resolveRootLocation("~/los-files"));
        assertEquals(home, LocalStorageServiceImpl.resolveRootLocation("~"));
    }

    @Test
    @DisplayName("Relative storage folder is resolved against the working directory; absolute is kept")
    void testRootLocationRelativeAndAbsolute() {
        assertEquals(Path.of("uploads").toAbsolutePath().normalize(), LocalStorageServiceImpl.resolveRootLocation("uploads"));
        assertEquals(tempUploadDir.toAbsolutePath().normalize(), LocalStorageServiceImpl.resolveRootLocation(tempUploadDir.toString()));
        assertEquals(tempUploadDir.toAbsolutePath().normalize(), storageService.getRootLocation());
    }

    @Test
    @DisplayName("Successfully stores and loads a valid PDF file with transparent encryption at rest")
    void testStoreAndLoadValidPdf() throws Exception {
        byte[] pdfBytes = new byte[]{'%', 'P', 'D', 'F', '-', '1', '.', '4', '\n', 't', 'e', 's', 't'};
        String relativePath = storageService.storeFile("CUS123", "aadhaar_card.pdf", pdfBytes, "application/pdf");

        assertNotNull(relativePath);
        assertTrue(relativePath.startsWith("CUS123/"));
        assertTrue(relativePath.endsWith("_aadhaar_card.pdf"));

        // Verify that raw bytes on disk are encrypted (starts with ENC1, not %PDF)
        Path physicalFile = tempUploadDir.resolve(relativePath);
        assertTrue(java.nio.file.Files.exists(physicalFile));
        byte[] rawOnDisk = java.nio.file.Files.readAllBytes(physicalFile);
        assertNotEquals('%', (char) rawOnDisk[0]);
        assertEquals("ENC1", new String(rawOnDisk, 0, 4, java.nio.charset.StandardCharsets.UTF_8));

        // Verify transparent loadFile decrypts back to original plaintext
        byte[] loaded = storageService.loadFile(relativePath);
        assertArrayEquals(pdfBytes, loaded);

        // Verify loadFileInputStream returns complete decrypted stream
        try (java.io.InputStream stream = storageService.loadFileInputStream(relativePath)) {
            byte[] streamedBytes = stream.readAllBytes();
            assertArrayEquals(pdfBytes, streamedBytes);
        }
    }

    @Test
    @DisplayName("Requirement 1: Encrypted file moved to another path on disk fails AAD authentication")
    void testFileMovedOnDiskFailsAuthentication() throws Exception {
        byte[] pdfBytes = new byte[]{'%', 'P', 'D', 'F', '-', '1', '.', '4', '\n', 'k', 'y', 'c'};
        String originalPath = storageService.storeFile("CUS123", "doc.pdf", pdfBytes, "application/pdf");

        Path sourcePath = tempUploadDir.resolve(originalPath);
        Path targetDir = tempUploadDir.resolve("CUS999");
        java.nio.file.Files.createDirectories(targetDir);
        Path targetPath = targetDir.resolve("stolen.pdf");

        // Copy/move the encrypted file to another path
        java.nio.file.Files.copy(sourcePath, targetPath);

        // Attempting to load from the new path must fail GCM AAD authentication
        assertThrows(com.los.bank.customer.exception.DocumentStorageException.class, () ->
                storageService.loadFile("CUS999/stolen.pdf")
        );
    }

    @Test
    @DisplayName("Successfully stores and loads a valid PNG file")
    void testStoreAndLoadValidPng() {
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01};
        String relativePath = storageService.storeFile("LEAD001", "photo.png", pngBytes, "image/png");

        assertNotNull(relativePath);
        byte[] loaded = storageService.loadFile(relativePath);
        assertArrayEquals(pngBytes, loaded);
    }

    @Test
    @DisplayName("Successfully stores and loads a valid JPEG file")
    void testStoreAndLoadValidJpeg() {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        String relativePath = storageService.storeFile("CUS999", "photo.jpg", jpegBytes, "image/jpeg");

        assertNotNull(relativePath);
        byte[] loaded = storageService.loadFile(relativePath);
        assertArrayEquals(jpegBytes, loaded);
    }

    @Test
    @DisplayName("Rejects file when magic bytes do not match PDF extension")
    void testRejectPdfWithInvalidMagicBytes() {
        byte[] fakePdf = "This is plain text pretending to be pdf".getBytes();
        DocumentValidationException ex = assertThrows(DocumentValidationException.class, () ->
                storageService.storeFile("CUS123", "doc.pdf", fakePdf, "application/pdf")
        );
        assertTrue(ex.getMessage().contains("does not match PDF format"));
    }

    @Test
    @DisplayName("Rejects unsupported file extensions")
    void testRejectUnsupportedExtension() {
        byte[] fileBytes = new byte[]{1, 2, 3, 4};
        DocumentValidationException ex = assertThrows(DocumentValidationException.class, () ->
                storageService.storeFile("CUS123", "script.exe", fileBytes, "application/octet-stream")
        );
        assertTrue(ex.getMessage().contains("Unsupported file type"));
    }

    @Test
    @DisplayName("Rejects files exceeding 5MB size limit")
    void testRejectFileOver5MB() {
        byte[] largeFile = new byte[5 * 1024 * 1024 + 10];
        // Set PDF header
        largeFile[0] = '%';
        largeFile[1] = 'P';
        largeFile[2] = 'D';
        largeFile[3] = 'F';
        largeFile[4] = '-';

        DocumentValidationException ex = assertThrows(DocumentValidationException.class, () ->
                storageService.storeFile("CUS123", "large.pdf", largeFile, "application/pdf")
        );
        assertTrue(ex.getMessage().contains("exceeds 5MB limit"));
    }

    @Test
    @DisplayName("Rejects path traversal attempts")
    void testRejectPathTraversal() {
        DocumentValidationException ex = assertThrows(DocumentValidationException.class, () ->
                storageService.loadFile("../../../Windows/System32/drivers/etc/hosts")
        );
        assertTrue(ex.getMessage().contains("Invalid file path"));
    }
}
