package com.los.bank.setup.service;

import com.los.bank.setup.exception.BankSetupException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Initializes the Master Database automatically on application startup.
 * Connects to PostgreSQL maintenance database ('postgres'), dynamically creates
 * the master database if missing, and executes Master Flyway migrations before
 * the normal application datasource and Hibernate entities initialize.
 */
@Component("masterDatabaseInitializer")
public class MasterDatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(MasterDatabaseInitializer.class);
    private static final Pattern VALID_DB_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private int dbPort;

    @Value("${app.datasource.setup.username:postgres}")
    private String dbUsername;

    @Value("${app.datasource.setup.password:postgres}")
    private String dbPassword;

    @Value("${app.datasource.setup.url:jdbc:postgresql://localhost:5432/bank_setup}")
    private String setupDbUrl;

    @Value("${app.flyway.setup.location:classpath:db/migration/setup}")
    private String setupLocation;

    @Value("${app.flyway.setup.table:flyway_schema_history_setup}")
    private String setupTable;

    @Value("${app.flyway.setup.baseline-version:1}")
    private String setupBaselineVersion;

    @Value("${app.flyway.setup.baseline-description:Initial Setup Schema}")
    private String setupBaselineDescription;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @PostConstruct
    public void onInit() {
        initializeMasterDatabase();
    }

    /**
     * Safe, idempotent master database provisioning and Flyway schema execution.
     */
    public static final String[] BANK_INFORMATION_ADD_COLUMNS = {
            "legal_name VARCHAR(200)",
            "bank_type VARCHAR(50)",
            "pan VARCHAR(20)",
            "gst_no VARCHAR(30)",
            "license_no VARCHAR(50)",
            "website VARCHAR(255)",
            "logo_url VARCHAR(500)",
            "status VARCHAR(30) DEFAULT 'ACTIVE'",
            "regulatory_status VARCHAR(30) DEFAULT 'ACTIVE'",
            "direct_clg_member BOOLEAN DEFAULT FALSE",
            "direct_member_iftas BOOLEAN DEFAULT FALSE",
            "micr1 VARCHAR(20)",
            "micr2 VARCHAR(20)",
            "micr3 VARCHAR(20)",
            "micr_number VARCHAR(30)",
            "ifsc_code VARCHAR(20)",
            "no_of_branches INT",
            "sponsor_bank_clg VARCHAR(150)",
            "sponsor_bank_iftas VARCHAR(150)",
            "address_type VARCHAR(50) DEFAULT 'Registered Office'",
            "unit_gala VARCHAR(150)",
            "street_road VARCHAR(200)",
            "landmark VARCHAR(150)",
            "city VARCHAR(100)",
            "state VARCHAR(100)",
            "country VARCHAR(100) DEFAULT 'India'",
            "pincode VARCHAR(20)"
    };

    public synchronized void initializeMasterDatabase() {
        if (initialized.get()) {
            return;
        }

        String masterDbName = extractDatabaseName(setupDbUrl);
        validateDatabaseName(masterDbName);

        logger.info("Initializing Master Database '{}' at application startup...", masterDbName);

        // 1. Connect to PostgreSQL maintenance database and provision Master Database if missing
        provisionMasterDatabaseIfNotExists(masterDbName);

        // 2. Execute Flyway migrations against Master Database
        executeMasterFlywayMigrations(masterDbName);

        // 3. Synchronize existing Master Database schema (adds any missing columns non-destructively)
        synchronizeMasterDatabaseSchema(masterDbName);

        initialized.set(true);
        logger.info("Master Database '{}' initialized, migrated, and synchronized successfully.", masterDbName);
    }

    private void provisionMasterDatabaseIfNotExists(String masterDbName) {
        String maintenanceUrl = String.format("jdbc:postgresql://%s:%d/postgres", dbHost, dbPort);
        logger.info("Connecting to PostgreSQL maintenance database at '{}:{}'...", dbHost, dbPort);

        try (Connection conn = DriverManager.getConnection(maintenanceUrl, dbUsername, dbPassword)) {
            if (doesDatabaseExist(conn, masterDbName)) {
                logger.info("Master Database '{}' already exists. Existing data preserved.", masterDbName);
                return;
            }

            // PostgreSQL CREATE DATABASE must execute outside a transaction block
            conn.setAutoCommit(true);
            try (Statement statement = conn.createStatement()) {
                String createSql = String.format("CREATE DATABASE \"%s\"", masterDbName);
                logger.info("Creating Master Database with statement: CREATE DATABASE \"{}\"", masterDbName);
                statement.executeUpdate(createSql);
                logger.info("Master Database '{}' created successfully.", masterDbName);
            }
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to connect or create Master Database '%s': %s", masterDbName, e.getMessage());
            logger.error(errorMsg);
            throw new BankSetupException(errorMsg, e);
        }
    }

    private void executeMasterFlywayMigrations(String masterDbName) {
        String masterDbJdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", dbHost, dbPort, masterDbName);
        logger.info("Executing Master Flyway migrations on '{}'...", masterDbJdbcUrl);

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(masterDbJdbcUrl, dbUsername, dbPassword)
                    .locations(setupLocation, "classpath:db/master/migration")
                    .table(setupTable)
                    .baselineVersion(MigrationVersion.fromVersion(setupBaselineVersion))
                    .baselineDescription(setupBaselineDescription)
                    .baselineOnMigrate(false)
                    .cleanDisabled(true)
                    .validateOnMigrate(false)
                    .load();

            flyway.repair();
            flyway.migrate();
            logger.info("Master Flyway migrations executed successfully on table '{}'.", setupTable);
        } catch (Exception e) {
            String errorMsg = String.format("Master Database Flyway migration failed for '%s': %s", masterDbName, e.getMessage());
            logger.error(errorMsg);
            throw new BankSetupException(errorMsg, e);
        }
    }

    private void synchronizeMasterDatabaseSchema(String masterDbName) {
        String masterDbJdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", dbHost, dbPort, masterDbName);
        try (Connection conn = DriverManager.getConnection(masterDbJdbcUrl, dbUsername, dbPassword)) {
            synchronizeBankInformationSchema(conn, logger);
        } catch (SQLException e) {
            String errorMsg = String.format("Failed to synchronize Master Database schema for '%s': %s", masterDbName, e.getMessage());
            logger.error(errorMsg, e);
            throw new BankSetupException(errorMsg, e);
        }
    }

    /**
     * Idempotent, safe synchronization method for bank_information table.
     * Uses ALTER TABLE ... ADD COLUMN IF NOT EXISTS to guarantee all 35 columns
     * are present without deleting, dropping, or truncating existing data.
     */
    public static void synchronizeBankInformationSchema(Connection conn, Logger log) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        String activeCatalog = conn.getCatalog();
        String activeSchema = conn.getSchema();
        String engine = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();

        log.info("===================================================================");
        log.info("[Setup Schema Sync] Active Database:  '{}'", activeCatalog);
        log.info("[Setup Schema Sync] Active Schema:    '{}'", activeSchema != null ? activeSchema : "public");
        log.info("[Setup Schema Sync] Engine:           '{}'", engine);
        log.info("[Setup Schema Sync] Target Table:     'bank_information'");
        log.info("[Setup Schema Sync] Action:           Safe ALTER TABLE ... ADD COLUMN IF NOT EXISTS");
        log.info("===================================================================");

        // Verify table exists
        boolean tableExists = false;
        try (ResultSet rs = meta.getTables(activeCatalog, "public", "bank_information", new String[]{"TABLE"})) {
            if (rs.next()) {
                tableExists = true;
            }
        }

        if (!tableExists) {
            log.info("[Setup Schema Sync] Table 'bank_information' not yet present in schema. Flyway V1 will create it.");
            return;
        }

        // Apply any missing columns safely
        int addedOrVerified = 0;
        try (Statement stmt = conn.createStatement()) {
            for (String columnDef : BANK_INFORMATION_ADD_COLUMNS) {
                String sql = "ALTER TABLE bank_information ADD COLUMN IF NOT EXISTS " + columnDef;
                stmt.execute(sql);
                addedOrVerified++;
            }
        }

        // Diagnostic summary of present columns
        int totalColumns = 0;
        try (ResultSet rs = meta.getColumns(activeCatalog, "public", "bank_information", "%")) {
            while (rs.next()) {
                totalColumns++;
            }
        }
        log.info("[Setup Schema Sync] 'bank_information' table schema synchronized successfully. Total active columns: {}", totalColumns);
    }

    private boolean doesDatabaseExist(Connection connection, String databaseName) throws SQLException {
        String checkSql = "SELECT 1 FROM pg_database WHERE datname = ?";
        try (PreparedStatement statement = connection.prepareStatement(checkSql)) {
            statement.setString(1, databaseName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            return "bank_setup";
        }
        String cleanUrl = jdbcUrl.split("\\?")[0];
        int lastSlash = cleanUrl.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < cleanUrl.length() - 1) {
            return cleanUrl.substring(lastSlash + 1);
        }
        return "bank_setup";
    }

    private void validateDatabaseName(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("Master database name cannot be null or empty.");
        }
        if (!VALID_DB_NAME_PATTERN.matcher(databaseName).matches()) {
            throw new IllegalArgumentException("Invalid Master database name: '" + databaseName + "'. Only alphanumeric characters and underscores are permitted.");
        }
    }
}
