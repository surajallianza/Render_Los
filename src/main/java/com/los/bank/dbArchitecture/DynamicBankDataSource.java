package com.los.bank.dbArchitecture;

import com.los.bank.user.context.BankContext;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Dynamic Bank DataSource located in Database Architecture module.
 * Dynamically resolves the target bank database at runtime from:
 * 1. BankContext (thread-local context if set)
 * 2. Master Database (bank_setup -> database_configuration)
 * 
 * Caches connection pools per bank database.
 * Does NOT create or initialize any bank database at application startup.
 * Bank databases are provisioned exclusively on-demand through the Bank Setup process.
 */
public class DynamicBankDataSource extends AbstractDataSource {

    private static final Logger logger = LoggerFactory.getLogger(DynamicBankDataSource.class);
    private static final Pattern VALID_DB_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final DataSource setupDataSource;
    private final String dbHost;
    private final int dbPort;
    private final String dbUsername;
    private final String dbPassword;

    private final Map<String, HikariDataSource> poolCache = new ConcurrentHashMap<>();

    public DynamicBankDataSource(
            DataSource setupDataSource,
            String dbHost,
            int dbPort,
            String dbUsername,
            String dbPassword) {
        this.setupDataSource = setupDataSource;
        this.dbHost = (dbHost != null && !dbHost.trim().isEmpty()) ? dbHost.trim() : "localhost";
        this.dbPort = dbPort > 0 ? dbPort : 5432;
        this.dbUsername = (dbUsername != null && !dbUsername.trim().isEmpty()) ? dbUsername.trim() : "postgres";
        this.dbPassword = (dbPassword != null) ? dbPassword : "";
    }

    public DynamicBankDataSource(
            DataSource setupDataSource,
            String dbHost,
            int dbPort,
            String dbUsername,
            String dbPassword,
            String bankFlywayLocation,
            String bankFlywayTable) {
        this(setupDataSource, dbHost, dbPort, dbUsername, dbPassword);
    }

    @Override
    public Connection getConnection() throws SQLException {
        DataSource ds = getOrResolveDataSource();
        if (ds == null) {
            throw new SQLException("No bank database context set or configured. Set BankContext or provision bank database before accessing bank data.");
        }
        return ds.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        DataSource ds = getOrResolveDataSource();
        if (ds == null) {
            throw new SQLException("No bank database context set or configured. Set BankContext or provision bank database before accessing bank data.");
        }
        return ds.getConnection(username, password);
    }

    /**
     * Obtains or creates a thread-safe HikariDataSource for the active bank database.
     * Returns null if no bank database has been provisioned or configured yet.
     */
    public synchronized DataSource getOrResolveDataSource() {
        String dbName = resolveActiveDatabaseName();
        if (dbName == null || dbName.trim().isEmpty()) {
            return null;
        }
        validateDatabaseName(dbName);

        HikariDataSource existingPool = poolCache.get(dbName);
        if (existingPool != null && !existingPool.isClosed()) {
            return existingPool;
        }

        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", dbHost, dbPort, dbName);
        logger.info("[DynamicBankDataSource] Initializing connection pool for bank database '{}' ({})", dbName,
                jdbcUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("org.postgresql.Driver");
        config.setPoolName("DynamicBankPool-" + dbName);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(300000);
        config.setInitializationFailTimeout(-1);

        HikariDataSource ds = new HikariDataSource(config);
        poolCache.put(dbName, ds);
        return ds;
    }

    public synchronized HikariDataSource getOrInitDataSource() {
        DataSource ds = getOrResolveDataSource();
        if (ds instanceof HikariDataSource) {
            return (HikariDataSource) ds;
        }
        throw new IllegalStateException("No bank database configured or provisioned yet.");
    }

    /**
     * Resolves the active bank database name dynamically from:
     * 1. BankContext thread-local
     * 2. Master DB (bank_setup -> database_configuration)
     * Returns null if no bank database is configured yet.
     */
    public String resolveActiveDatabaseName() {
        // 1. Check BankContext
        try {
            String contextDb = BankContext.getCurrentDatabase();
            if (contextDb != null && !contextDb.trim().isEmpty()) {
                logger.debug("[DynamicBankDataSource] Target database from BankContext: '{}'", contextDb);
                return contextDb.trim().toLowerCase();
            }
        } catch (Throwable ignored) {
        }

        // 2. Query Master DB database_configuration
        if (setupDataSource != null) {
            try (Connection conn = setupDataSource.getConnection()) {
                // Try connected database first
                String sqlConnected = "SELECT database_name FROM database_configuration WHERE is_connected = true ORDER BY id DESC LIMIT 1";
                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(sqlConnected)) {
                    if (rs.next()) {
                        String dbName = rs.getString("database_name");
                        if (dbName != null && !dbName.trim().isEmpty()) {
                            logger.info(
                                    "[DynamicBankDataSource] Active bank database resolved from master database: '{}'",
                                    dbName.trim());
                            return dbName.trim().toLowerCase();
                        }
                    }
                }

                // Fallback to any active configured database
                String sqlAny = "SELECT database_name FROM database_configuration ORDER BY id ASC LIMIT 1";
                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(sqlAny)) {
                    if (rs.next()) {
                        String dbName = rs.getString("database_name");
                        if (dbName != null && !dbName.trim().isEmpty()) {
                            logger.info(
                                    "[DynamicBankDataSource] First bank database resolved from master database: '{}'",
                                    dbName.trim());
                            return dbName.trim().toLowerCase();
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("[DynamicBankDataSource] Could not query database_configuration from master: {}",
                        e.getMessage());
            }
        }

        // No bank database configured yet
        return null;
    }

    private void validateDatabaseName(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()
                || !VALID_DB_NAME_PATTERN.matcher(databaseName).matches()) {
            throw new IllegalArgumentException(
                    "Invalid database name: '" + databaseName + "'. Only alphanumeric and underscore permitted.");
        }
    }
}
