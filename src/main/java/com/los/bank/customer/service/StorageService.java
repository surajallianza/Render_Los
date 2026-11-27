package com.los.bank.customer.service;

import java.io.InputStream;

/**
 * Storage abstraction for customer documents to allow swapping local disk with S3/GCS in the future.
 */
public interface StorageService {

    /**
     * Stores a file under the given subfolder and returns the relative path.
     *
     * @param subFolder Subfolder (e.g. leadId or customerCode)
     * @param originalFilename Original client filename
     * @param content File content bytes
     * @param contentType Declared MIME content type
     * @return Relative storage path (e.g. "LEAD12345/3fa85f64_pan.pdf")
     */
    String storeFile(String subFolder, String originalFilename, byte[] content, String contentType);

    /**
     * Loads the file content bytes for the given relative path.
     *
     * @param relativePath Relative storage path
     * @return File content bytes
     */
    byte[] loadFile(String relativePath);

    /**
     * Opens an input stream for streaming the file.
     *
     * @param relativePath Relative storage path
     * @return InputStream
     */
    InputStream loadFileInputStream(String relativePath);

    /**
     * Permanently deletes the stored file. Does nothing if the file does not exist.
     * Only used when a DRAFT customer is discarded; normal document deletes are soft deletes.
     *
     * @param relativePath Relative storage path
     */
    void deleteFile(String relativePath);
}
