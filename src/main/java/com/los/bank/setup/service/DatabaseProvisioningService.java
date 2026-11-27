package com.los.bank.setup.service;

import com.los.bank.setup.exception.BankSetupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * Service responsible for dynamic PostgreSQL database provisioning.
 * Connects to PostgreSQL maintenance database ('postgres') and dynamically creates
 * dedicated databases for registered banks outside transaction blocks.
 */
@Service
public class DatabaseProvisioningService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseProvisioningService.class);
    private static final Pattern VALID_DB_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private int dbPort;

    @Value("${app.datasource.setup.username:postgres}")
    private String dbUsername;

    @Value("${app.datasource.setup.password:postgres}")
    private String dbPassword;

    /**
     * Dynamically creates the target database if it does not already exist.
     *
     * @param databaseName The name of the database to provision.
     * @return true if database exists or was created successfully.
     */
    public synchronized boolean createDatabaseIfNotExists(String databaseName) {
        validateDatabaseName(databaseName);

        String maintenanceUrl = String.format("jdbc:postgresql://%s:%d/postgres", dbHost, dbPort);
        logger.info("Connecting to PostgreSQL maintenance database at '{}' to verify/create '{}'", maintenanceUrl, databaseName);

        try (Connection connection = DriverManager.getConnection(maintenanceUrl, dbUsername, dbPassword)) {
            if (doesDatabaseExist(connection, databaseName)) {
                logger.info("Target database '{}' already exists. Preserving existing database without recreation.", databaseName);
                return true;
            }

            // PostgreSQL CREATE DATABASE cannot execute within a transaction block
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                String createSql = String.format("CREATE DATABASE \"%s\"", databaseName);
                logger.info("Executing database provisioning SQL: {}", createSql);
                statement.executeUpdate(createSql);
                logger.info("Database '{}' created successfully.", databaseName);
                return true;
            }
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to provision PostgreSQL database '%s': %s", databaseName, e.getMessage());
            logger.error(errorMsg, e);
            throw new BankSetupException(errorMsg, e);
        }
    }

    /**
     * Checks if a database with the given name exists in the PostgreSQL cluster.
     */
    private boolean doesDatabaseExist(Connection connection, String databaseName) throws SQLException {
        String checkSql = "SELECT 1 FROM pg_database WHERE datname = ?";
        try (PreparedStatement statement = connection.prepareStatement(checkSql)) {
            statement.setString(1, databaseName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * Ensures database name conforms to safe alphanumeric and underscore conventions.
     */
    public void validateDatabaseName(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("Database name cannot be null or empty.");
        }
        if (!VALID_DB_NAME_PATTERN.matcher(databaseName).matches()) {
            throw new IllegalArgumentException(
                    "Invalid database name: '" + databaseName + "'. Only alphanumeric characters and underscores are permitted.");
        }
    }

    public String getDbHost() {
        return dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }
}

