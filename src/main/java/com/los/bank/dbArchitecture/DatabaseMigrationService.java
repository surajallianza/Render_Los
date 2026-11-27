package com.los.bank.dbArchitecture;

import com.los.bank.setup.service.MasterDatabaseInitializer;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service orchestrating Flyway migrations for Setup Database and Bank Database.
 * Handles:
 * 1. Scenario 1: New database (executes initial V1 migration).
 * 2. Scenario 2: Existing database with Flyway history (validates and applies pending migrations).
 * 3. Scenario 3: Existing database without Flyway history (verifies schema compatibility before baseline).
 */
@Service
public class DatabaseMigrationService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationService.class);

    // Expected core tables in Setup Schema V1 for baseline compatibility verification
    private static final List<String> SETUP_CORE_TABLES = Arrays.asList(
            "bank_information",
            "database_configuration",
            "system_setup",
            "initial_admin_setup",
            "setup_activity_log"
    );

    // Expected core tables in Bank Schema V1 for baseline compatibility verification
    private static final List<String> BANK_CORE_TABLES = Arrays.asList(
            "b001", "r001", "r002", "u001", "u002",
            "c001", "c002", "m001", "m002", "p001", "a001", "g001"
    );

    private final DataSource setupDataSource;
    @SuppressWarnings("unused")
    private final DataSource bankDataSource;
    private final Flyway setupFlyway;
    @SuppressWarnings("unused")
    private final Flyway bankFlyway;

    @Value("${app.flyway.setup.table:flyway_schema_history_setup}")
    private String setupTable;

    @Value("${app.flyway.setup.location:classpath:db/migration/setup}")
    private String setupLocation;

    @Value("${app.flyway.bank.table:flyway_schema_history_bank}")
    private String bankTable;

    @Value("${app.flyway.bank.location:classpath:db/migration/bank}")
    private String bankLocation;

    @Value("${app.flyway.bank.baseline-version:1}")
    private String bankBaselineVersion;

    @Value("${app.flyway.bank.baseline-description:Initial Bank Schema}")
    private String bankBaselineDescription;

    @Value("${app.migration.auto-run:true}")
    private boolean autoRun;

    public DatabaseMigrationService(
            @Qualifier("setupDataSource") DataSource setupDataSource,
            @Qualifier("bankDataSource") DataSource bankDataSource,
            @Qualifier("setupFlyway") Flyway setupFlyway,
            @Qualifier("bankFlyway") Flyway bankFlyway) {
        this.setupDataSource = setupDataSource;
        this.bankDataSource = bankDataSource;
        this.setupFlyway = setupFlyway;
        this.bankFlyway = bankFlyway;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (autoRun) {
            runMigrations();
        } else {
            logger.info("Automatic database migration on startup is disabled (app.migration.auto-run=false).");
        }
    }

    /**
     * Executes Setup and Bank database migrations sequentially.
     */
    public synchronized void runMigrations() {
        try {
            // 1. Migrate Setup Database
            migrateTarget("Setup", setupDataSource, setupFlyway, setupTable, SETUP_CORE_TABLES);

            logger.info("Database initialization completed successfully.");
        } catch (DatabaseMigrationException e) {
            logger.error("Database initialization failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during database initialization: {}", e.getMessage());
            throw new DatabaseMigrationException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Dynamically executes Flyway migrations against a specific bank database DataSource.
     */
    public synchronized void migrateBankDatabase(DataSource dynamicBankDataSource) {
        Flyway dynamicFlyway = Flyway.configure()
                .dataSource(dynamicBankDataSource)
                .locations(bankLocation)
                .table(bankTable)
                .baselineVersion(org.flywaydb.core.api.MigrationVersion.fromVersion(bankBaselineVersion))
                .baselineDescription(bankBaselineDescription)
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateOnMigrate(false)
                .load();

        migrateTarget("Bank", dynamicBankDataSource, dynamicFlyway, bankTable, BANK_CORE_TABLES);
    }

    /**
     * Orchestrates migration for a specific database target (Setup or Bank).
     */
    private void migrateTarget(
            String targetName,
            DataSource dataSource,
            Flyway flyway,
            String historyTableName,
            List<String> expectedCoreTables) {

        // Establish and verify database connection
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbCatalog = connection.getCatalog();
            String dbSchema = connection.getSchema();
            String dbEngine = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();
            String location = "Setup".equalsIgnoreCase(targetName) ? setupLocation : bankLocation;

            logger.info("===================================================================");
            logger.info("[Migration Diagnostic] Target Datasource:       {}", targetName);
            logger.info("[Migration Diagnostic] Active Database Name:    {}", dbCatalog);
            logger.info("[Migration Diagnostic] Active Schema:           {}", dbSchema != null ? dbSchema : "public");
            logger.info("[Migration Diagnostic] Engine / Version:        {}", dbEngine);
            logger.info("[Migration Diagnostic] Migration Location:      {}", location);
            logger.info("[Migration Diagnostic] History Table:           {}", historyTableName);
            logger.info("===================================================================");

            boolean historyExists = hasAppliedMigrations(flyway, connection, historyTableName);

            logger.info("{} Flyway migration started.", targetName);

            if (historyExists) {
                // Scenario 2: Existing Database With Flyway History
                logger.info("[{}] Existing Flyway history found in '{}'. Validating and applying pending migrations...", targetName, historyTableName);
                try {
                    flyway.repair();
                } catch (Exception e) {
                    logger.debug("Flyway repair non-critical: {}", e.getMessage());
                }
                int appliedCount = flyway.migrate().migrationsExecuted;
                logger.info("[{}] {} new migration(s) applied. Existing data preserved.", targetName, appliedCount);
            } else {
                boolean hasUserTables = hasAnyUserTables(connection, historyTableName);

                if (!hasUserTables) {
                    // Scenario 1: New Database
                    logger.info("[{}] New database detected. Running initial Flyway migration...", targetName);
                    int appliedCount = flyway.migrate().migrationsExecuted;
                    logger.info("[{}] Initial migration applied ({} migration(s) executed).", targetName, appliedCount);
                } else {
                    // Scenario 3: Existing Database Without Flyway History
                    logger.warn("[{}] Existing tables detected without Flyway history table '{}'. Inspecting schema compatibility...", targetName, historyTableName);

                    List<String> missingTables = verifySchemaCompatibility(connection, expectedCoreTables);
                    if (!missingTables.isEmpty()) {
                        String errorMsg = String.format(
                                "[%s] Existing database schema is INCOMPATIBLE with V1 baseline! Missing required tables: %s. " +
                                "Migration aborted to protect existing data. Never dropping or recreating tables.",
                                targetName, missingTables);
                        logger.error(errorMsg);
                        throw new DatabaseMigrationException(errorMsg);
                    }

                    logger.info("[{}] Existing schema compatibility verified. Performing controlled baseline at version 1...", targetName);
                    flyway.baseline();
                    logger.info("[{}] Baseline created in '{}'. Applying any pending newer migrations...", targetName, historyTableName);
                    int appliedCount = flyway.migrate().migrationsExecuted;
                    logger.info("[{}] Post-baseline migration completed ({} migration(s) executed).", targetName, appliedCount);
                }
            }

            // For Setup database, guarantee all 35 columns are present via safe idempotent synchronization
            if ("Setup".equalsIgnoreCase(targetName)) {
                MasterDatabaseInitializer.synchronizeBankInformationSchema(connection, logger);
            }

            logger.info("{} migration completed.", targetName);

        } catch (SQLException e) {
            String errorMsg = String.format("%s database connection failure: %s", targetName, e.getMessage());
            logger.error(errorMsg);
            throw new DatabaseMigrationException(errorMsg, e);
        } catch (FlywayException e) {
            String errorMsg = String.format("%s Flyway migration failure: %s", targetName, e.getMessage());
            logger.error(errorMsg);
            throw new DatabaseMigrationException(errorMsg, e);
        }
    }

    /**
     * Checks whether the Flyway schema history table exists and contains applied migrations.
     */
    private boolean hasAppliedMigrations(Flyway flyway, Connection connection, String historyTableName) {
        try {
            if (!doesTableExist(connection, historyTableName)) {
                return false;
            }
            MigrationInfo[] applied = flyway.info().applied();
            return applied != null && applied.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if any user tables exist in the database (excluding system schemas and Flyway history tables).
     */
    private boolean hasAnyUserTables(Connection connection, String historyTableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
            if (rs == null) {
                return false;
            }
            while (rs.next()) {
                String schemaName = rs.getString("TABLE_SCHEM");
                String tableName = rs.getString("TABLE_NAME");

                if (schemaName != null && (schemaName.startsWith("pg_") || schemaName.equalsIgnoreCase("information_schema"))) {
                    continue;
                }

                if (tableName != null && (tableName.equalsIgnoreCase(historyTableName)
                        || tableName.equalsIgnoreCase("flyway_schema_history")
                        || tableName.startsWith("flyway_schema_history"))) {
                    continue;
                }

                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a specific table exists in the database.
     */
    private boolean doesTableExist(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

        // 1. Direct lowercase check (standard PostgreSQL unquoted identifiers)
        try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, tableName.toLowerCase(), new String[]{"TABLE", "VIEW"})) {
            if (rs != null && rs.next()) {
                return true;
            }
        }

        // 2. Direct uppercase check
        try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, tableName.toUpperCase(), new String[]{"TABLE", "VIEW"})) {
            if (rs != null && rs.next()) {
                return true;
            }
        }

        // 3. Fallback: scan all tables in catalog
        try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE", "VIEW"})) {
            if (rs != null) {
                while (rs.next()) {
                    String existing = rs.getString("TABLE_NAME");
                    if (tableName.equalsIgnoreCase(existing)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Verifies that all expected core tables exist in an existing unmanaged database before baselining.
     */
    private List<String> verifySchemaCompatibility(Connection connection, List<String> expectedCoreTables) throws SQLException {
        List<String> missingTables = new ArrayList<>();
        for (String expectedTable : expectedCoreTables) {
            if (!doesTableExist(connection, expectedTable)) {
                missingTables.add(expectedTable);
            }
        }
        return missingTables;
    }
}
