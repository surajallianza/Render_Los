package com.los.bank.user.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dynamic DataSource provisioning and pooling for separate bank
 * databases.
 * Inspects the master database (bank_setup) to resolve bank database names and
 * connection settings.
 */
@Component
public class DynamicBankDataSourceManager {

    private static final Logger logger = LoggerFactory.getLogger(DynamicBankDataSourceManager.class);

    private final DataSource setupDataSource;
    private final Map<String, HikariDataSource> dataSourceCache = new ConcurrentHashMap<>();

    @Value("${app.datasource.setup.username:${SETUP_DB_USERNAME:postgres}}")
    private String defaultUsername;

    @Value("${app.datasource.setup.password:${SETUP_DB_PASSWORD:root}}")
    private String defaultPassword;

    @Value("${DB_HOST:localhost}")
    private String defaultHost;

    @Value("${DB_PORT:5432}")
    private int defaultPort;

    @Value("${app.datasource.bank.url:#{null}}")
    private String fallbackBankUrl;

    public DynamicBankDataSourceManager(@Qualifier("setupDataSource") DataSource setupDataSource) {
        this.setupDataSource = setupDataSource;
    }

    /**
     * Resolves the target database name based on bank code, database name, or
     * identifier.
     * If identifier is null, resolves the first active bank database from the
     * master setup schema.
     */
    public String resolveBankDatabase(String bankIdentifier) {
        if (bankIdentifier != null && !bankIdentifier.trim().isEmpty()) {
            String trimmed = bankIdentifier.trim();
            if (trimmed.toLowerCase().startsWith("bank_")) {
                return trimmed.toLowerCase();
            }

            // Look up by bank_code in master database
            String resolved = findDatabaseNameByBankCode(trimmed);
            if (resolved != null) {
                return resolved;
            }

            // Standard naming convention fallback: bank_<code_lowercase>
            return "bank_" + trimmed.toLowerCase().replaceAll("[^a-z0-9_]", "");
        }

        return resolveDefaultBankDatabase();
    }

    /**
     * Resolves the default bank database from database_configuration or fallback
     * configuration.
     */
    public String resolveDefaultBankDatabase() {
        String sql = "SELECT database_name FROM database_configuration ORDER BY id ASC LIMIT 1";
        try (Connection conn = setupDataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                String dbName = rs.getString("database_name");
                if (dbName != null && !dbName.trim().isEmpty()) {
                    logger.debug("[DynamicBankDataSourceManager] Resolved default bank database from master DB: '{}'",
                            dbName);
                    return dbName.trim();
                }
            }
        } catch (SQLException e) {
            logger.warn("[DynamicBankDataSourceManager] Failed to query default database from master: {}",
                    e.getMessage());
        }

        if (fallbackBankUrl != null && fallbackBankUrl.contains("/")) {
            String db = fallbackBankUrl.substring(fallbackBankUrl.lastIndexOf('/') + 1);
            if (db.contains("?")) {
                db = db.substring(0, db.indexOf('?'));
            }
            if (!db.isEmpty()) {
                return db;
            }
        }

        return null;
    }

    /**
     * Retrieves or lazily creates a Hikari connection pool for the given bank
     * database.
     */
    public DataSource getOrCreateDataSource(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            databaseName = resolveDefaultBankDatabase();
        }
        if (databaseName == null || databaseName.trim().isEmpty()) {
            return null;
        }
        final String targetDb = databaseName.trim().toLowerCase();

        return dataSourceCache.computeIfAbsent(targetDb, this::createBankDataSource);
    }

    private HikariDataSource createBankDataSource(String databaseName) {
        logger.info("[DynamicBankDataSourceManager] Initializing new connection pool for bank database '{}'",
                databaseName);

        String host = defaultHost;
        int port = defaultPort;

        // Try looking up host and port from database_configuration
        String sql = "SELECT database_host, database_port FROM database_configuration WHERE LOWER(database_name) = ? LIMIT 1";
        try (Connection conn = setupDataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, databaseName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String h = rs.getString("database_host");
                    int p = rs.getInt("database_port");
                    if (h != null && !h.trim().isEmpty()) {
                        host = h.trim();
                    }
                    if (p > 0) {
                        port = p;
                    }
                }
            }
        } catch (SQLException e) {
            logger.warn("[DynamicBankDataSourceManager] Unable to load host/port for '{}' from setup DB: {}",
                    databaseName, e.getMessage());
        }

        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(defaultUsername);
        config.setPassword(defaultPassword);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(120000);
        config.setPoolName("BankPool-" + databaseName);

        logger.info("[DynamicBankDataSourceManager] Built HikariDataSource for bank database '{}' (URL: {})",
                databaseName, jdbcUrl);
        return new HikariDataSource(config);
    }

    private String findDatabaseNameByBankCode(String bankCode) {
        String sql = "SELECT d.database_name FROM database_configuration d " +
                "JOIN bank_information b ON b.id = d.bank_id " +
                "WHERE UPPER(b.bank_code) = UPPER(?) " +
                "LIMIT 1";
        try (Connection conn = setupDataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bankCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("database_name");
                }
            }
        } catch (SQLException e) {
            logger.debug("[DynamicBankDataSourceManager] Could not find database by bank_code '{}': {}", bankCode,
                    e.getMessage());
        }
        return null;
    }
}
