package com.los.bank.dbArchitecture;

/**
 * Exception thrown when database connection, schema compatibility check, valida
 * ion, or migration fails.
 */
public class DatabaseMigrationException extends RuntimeException {

    public DatabaseMigrationException(String message) {
        super(message);
    }

    public DatabaseMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
