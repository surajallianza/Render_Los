package com.los.bank.customer.service;

import com.los.bank.customer.config.CustomerSecurityProperties;
import com.los.bank.customer.config.CustomerSecurityStartupValidator;
import com.los.bank.customer.exception.DocumentNotFoundException;
import com.los.bank.customer.exception.DocumentStorageException;
import com.los.bank.customer.exception.DocumentValidationException;
import com.los.bank.user.context.BankContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;

/**
 * Local filesystem implementation of {@link StorageService}.
 * Features transparent AES-256-GCM encryption at rest with GCM AAD binding to tenant and relative storage path.
 * In-memory decryption guarantees GCM tag verification before any data is streamed to clients.
 */
@Service
public class LocalStorageServiceImpl implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageServiceImpl.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    /** Used under the user's home folder when no storage path is configured. */
    static final String DEFAULT_FOLDER = "uploads/customer-documents";

    private final Path rootLocation;
    private final DocumentCryptoService documentCryptoService;
    private final CustomerSecurityProperties properties;

    @Autowired
    public LocalStorageServiceImpl(
            @Value("${storage.customer-documents.path:}") String basePath,
            DocumentCryptoService documentCryptoService,
            CustomerSecurityProperties properties) {

        this.rootLocation = resolveRootLocation(basePath);
        this.documentCryptoService = documentCryptoService;
        this.properties = properties;

        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            logger.error("Could not initialize document storage at {}", this.rootLocation, e);
            throw new DocumentStorageException("Could not initialize document storage folder: " + this.rootLocation, e);
        }
        logger.info("Customer document storage folder: {}", this.rootLocation);
    }

    /**
     * Turns the configured folder into an absolute path that works on any machine.
     * <ul>
     *   <li>blank / not set: {@code <user home>/uploads/customer-documents} of whoever runs the app</li>
     *   <li>starts with {@code ~}: {@code ~} is replaced by the user's home folder</li>
     *   <li>relative path (e.g. {@code uploads}): resolved against the folder the app is started from</li>
     *   <li>absolute path: used as is</li>
     * </ul>
     * Only paths RELATIVE to this folder are stored in c013.storage_path, so the folder can be
     * changed or moved without touching the database.
     */
    static Path resolveRootLocation(String basePath) {
        String userHome = System.getProperty("user.home");
        String path = basePath == null ? "" : basePath.trim();
        if (path.isEmpty()) {
            return Paths.get(userHome, DEFAULT_FOLDER).toAbsolutePath().normalize();
        }
        if (path.equals("~")) {
            path = userHome;
        } else if (path.startsWith("~/") || path.startsWith("~\\")) {
            path = userHome + path.substring(1);
        }
        return Paths.get(path).toAbsolutePath().normalize();
    }

    /**
     * Absolute folder under which all customer documents are stored.
     */
    public Path getRootLocation() {
        return rootLocation;
    }

    /**
     * Testing constructor with default development crypto service and configuration.
     */
    public LocalStorageServiceImpl(String basePath) {
        this(basePath, createDefaultCryptoService(), new CustomerSecurityProperties());
    }

    private static DocumentCryptoService createDefaultCryptoService() {
        CustomerSecurityProperties props = new CustomerSecurityProperties();
        props.setMasterSecret(CustomerSecurityStartupValidator.DEV_FALLBACK_MASTER_SECRET);
        return new DocumentCryptoService(props);
    }

    @Override
    public String storeFile(String subFolder, String originalFilename, byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new DocumentValidationException("File content cannot be empty");
        }
        if (content.length > MAX_FILE_SIZE) {
            throw new DocumentValidationException("File size exceeds 5MB limit. Actual size: " + content.length + " bytes");
        }
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new DocumentValidationException("Original filename cannot be empty");
        }

        // Validate plaintext magic bytes to prevent malware / invalid formats
        validateMagicBytes(originalFilename, content);

        String sanitizedFilename = sanitizeFilename(originalFilename);
        String subDirectory = (subFolder == null || subFolder.trim().isEmpty()) ? "common" : sanitizeFolder(subFolder.trim());
        String uniqueFileName = UUID.randomUUID().toString() + "_" + sanitizedFilename;
        String relativePath = subDirectory + "/" + uniqueFileName;

        Path targetDir = rootLocation.resolve(subDirectory).normalize();
        validatePathTraversal(targetDir);

        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(uniqueFileName).normalize();
            validatePathTraversal(targetFile);

            String tenant = resolveTenant(subDirectory);

            byte[] bytesToWrite;
            if (properties.isDocumentEncryptionEnabled()) {
                bytesToWrite = documentCryptoService.encrypt(content, tenant, relativePath);
                logger.debug("Encrypted document '{}' for tenant '{}' with relative path '{}' (plaintext size: {}, encrypted size: {})",
                        originalFilename, tenant, relativePath, content.length, bytesToWrite.length);
            } else {
                bytesToWrite = content;
                logger.warn("Document encryption is disabled! Storing unencrypted plaintext for file '{}'", originalFilename);
            }

            Files.write(targetFile, bytesToWrite, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return relativePath;
        } catch (IOException e) {
            logger.error("Failed to store file {}", originalFilename, e);
            throw new DocumentStorageException("Failed to store file on disk: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] loadFile(String relativePath) {
        try {
            Path filePath = resolveAndValidate(relativePath);
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new DocumentNotFoundException("Document file not found at: " + relativePath);
            }

            byte[] rawBytes = Files.readAllBytes(filePath);
            String tenant = resolveTenant(relativePath);

            // Transparently decrypt and authenticate GCM tag with AAD
            return documentCryptoService.decrypt(rawBytes, tenant, relativePath);
        } catch (IOException e) {
            throw new DocumentStorageException("Error reading document file: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream loadFileInputStream(String relativePath) {
        // Requirement 7: Decrypt the whole file in memory before streaming.
        // Never stream data before the GCM tag has been verified.
        byte[] decryptedBytes = loadFile(relativePath);
        return new ByteArrayInputStream(decryptedBytes);
    }

    private String resolveTenant(String pathSegment) {
        String tenant = BankContext.getCurrentBankCode();
        if (tenant == null || tenant.trim().isEmpty()) {
            tenant = BankContext.getCurrentDatabase();
        }
        if ((tenant == null || tenant.trim().isEmpty() || "default".equalsIgnoreCase(tenant)) && pathSegment != null) {
            String[] parts = pathSegment.replace('\\', '/').split("/");
            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                tenant = parts[0].trim();
            }
        }
        if (tenant == null || tenant.trim().isEmpty()) {
            tenant = "default";
        }
        return tenant.trim();
    }

    @Override
    public void deleteFile(String relativePath) {
        try {
            Path filePath = resolveAndValidate(relativePath);
            if (Files.isRegularFile(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            throw new DocumentStorageException("Error deleting document file: " + e.getMessage(), e);
        }
    }

    private Path resolveAndValidate(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new DocumentValidationException("Relative path cannot be empty");
        }
        Path resolved = rootLocation.resolve(relativePath).normalize();
        validatePathTraversal(resolved);
        return resolved;
    }

    private void validatePathTraversal(Path path) {
        if (!path.startsWith(rootLocation)) {
            logger.warn("Path traversal detected! Attempted path: {}, Root: {}", path, rootLocation);
            throw new DocumentValidationException("Access denied: Invalid file path.");
        }
    }

    private String sanitizeFilename(String filename) {
        String nameOnly = Paths.get(filename).getFileName().toString();
        return nameOnly.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sanitizeFolder(String folder) {
        return java.util.Arrays.stream(folder.split("[/\\\\]"))
                .filter(s -> !s.trim().isEmpty())
                .map(s -> s.replaceAll("[^a-zA-Z0-9_-]", "_"))
                .collect(java.util.stream.Collectors.joining("/"));
    }

    private void validateMagicBytes(String filename, byte[] bytes) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            // PDF starts with "%PDF-" (0x25, 0x50, 0x44, 0x46, 0x2D)
            if (bytes.length < 5 || bytes[0] != 0x25 || bytes[1] != 0x50 || bytes[2] != 0x44 || bytes[3] != 0x46 || bytes[4] != 0x2D) {
                throw new DocumentValidationException("File content does not match PDF format");
            }
        } else if (lower.endsWith(".png")) {
            // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
            if (bytes.length < 8 ||
                    (bytes[0] & 0xFF) != 0x89 ||
                    (bytes[1] & 0xFF) != 0x50 ||
                    (bytes[2] & 0xFF) != 0x4E ||
                    (bytes[3] & 0xFF) != 0x47 ||
                    (bytes[4] & 0xFF) != 0x0D ||
                    (bytes[5] & 0xFF) != 0x0A ||
                    (bytes[6] & 0xFF) != 0x1A ||
                    (bytes[7] & 0xFF) != 0x0A) {
                throw new DocumentValidationException("File content does not match PNG format");
            }
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            // JPEG magic bytes: FF D8 FF
            if (bytes.length < 3 ||
                    (bytes[0] & 0xFF) != 0xFF ||
                    (bytes[1] & 0xFF) != 0xD8 ||
                    (bytes[2] & 0xFF) != 0xFF) {
                throw new DocumentValidationException("File content does not match JPEG/JPG format");
            }
        } else {
            throw new DocumentValidationException("Unsupported file type. Only PDF, JPG, JPEG, and PNG files are allowed.");
        }
    }
}
