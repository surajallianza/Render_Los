package com.los.bank.branch.service;

import com.los.bank.dbArchitecture.DatabaseMigrationService;
import com.los.bank.setup.entity.BankInformation;
import com.los.bank.setup.entity.DatabaseConfiguration;
import com.los.bank.setup.repository.BankInformationRepository;
import com.los.bank.setup.repository.DatabaseConfigurationRepository;
import com.los.bank.setup.service.DatabaseProvisioningService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that dynamically resolves, provisions, and caches DataSources
 * for bank tenant databases based on bank configuration in the setup database.
 */
@Service
public class DynamicBankDataSourceService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicBankDataSourceService.class);

    private final BankInformationRepository bankInformationRepository;
    private final DatabaseConfigurationRepository databaseConfigurationRepository;
    private final DatabaseProvisioningService databaseProvisioningService;
    private final DatabaseMigrationService databaseMigrationService;

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private int dbPort;

    @Value("${app.datasource.setup.username:postgres}")
    private String dbUsername;

    @Value("${app.datasource.setup.password:postgres}")
    private String dbPassword;

    @Value("${app.datasource.bank.url:}")
    private String defaultBankDbUrl;

    private final ConcurrentHashMap<String, HikariDataSource> dataSourceCache = new ConcurrentHashMap<>();

    public DynamicBankDataSourceService(
            BankInformationRepository bankInformationRepository,
            DatabaseConfigurationRepository databaseConfigurationRepository,
            DatabaseProvisioningService databaseProvisioningService,
            DatabaseMigrationService databaseMigrationService) {
        this.bankInformationRepository = bankInformationRepository;
        this.databaseConfigurationRepository = databaseConfigurationRepository;
        this.databaseProvisioningService = databaseProvisioningService;
        this.databaseMigrationService = databaseMigrationService;
    }

    /**
     * Dynamically determines the target bank database name.
     * Checks BankInformation -> DatabaseConfiguration in Setup DB.
     * If not found, derives from bankName or uses default.
     */
    public String resolveDatabaseName(String bankName) {
        if (bankName != null && !bankName.trim().isEmpty()) {
            String trimmed = bankName.trim();

            // 1. Check BankInformation by bankName or bankCode
            BankInformation bank = bankInformationRepository.findByBankNameIgnoreCase(trimmed)
                    .or(() -> bankInformationRepository.findByBankCode(trimmed.toUpperCase()))
                    .orElse(null);

            if (bank != null) {
                DatabaseConfiguration dbConfig = databaseConfigurationRepository.findByBankId(bank.getId()).orElse(null);
                if (dbConfig != null && dbConfig.getDatabaseName() != null && !dbConfig.getDatabaseName().trim().isEmpty()) {
                    logger.info("Dynamically resolved database '{}' for bank '{}' (ID: {})", dbConfig.getDatabaseName(), trimmed, bank.getId());
                    return dbConfig.getDatabaseName().trim();
                }
            }

            // 2. Check DatabaseConfiguration directly by databaseName
            String derivedName = "bank_" + trimmed.toLowerCase().replaceAll("[^a-z0-9_]", "");
            if (databaseConfigurationRepository.findByDatabaseName(derivedName).isPresent()) {
                return derivedName;
            }

            if (derivedName.length() > 5) {
                return derivedName;
            }
        }

        // 3. Fallback: check first active database in database_configuration
        List<DatabaseConfiguration> allConfigs = databaseConfigurationRepository.findAll();
        for (DatabaseConfiguration config : allConfigs) {
            if (config.getDatabaseName() != null && !config.getDatabaseName().trim().isEmpty()) {
                logger.info("Using first available bank database from setup configuration: '{}'", config.getDatabaseName());
                return config.getDatabaseName().trim();
            }
        }

        // 4. Fallback to default configured bank database
        String defaultDb = extractDatabaseName(defaultBankDbUrl);
        if (defaultDb != null && !defaultDb.trim().isEmpty()) {
            logger.info("Using fallback bank database: '{}'", defaultDb);
            return defaultDb;
        }
        return null;
    }

    /**
     * Retrieves or creates a pooled DataSource for the target database,
     * ensuring the database is provisioned and migrated.
     */
    public DataSource getDataSourceForBank(String bankName) {
        String dbName = resolveDatabaseName(bankName);
        if (dbName == null || dbName.trim().isEmpty()) {
            throw new IllegalStateException("No active bank database configured. Please complete bank setup first.");
        }
        return getDataSourceByDatabaseName(dbName);
    }

    /**
     * Obtains or creates a thread-safe HikariDataSource pool for the given database name.
     */
    public DataSource getDataSourceByDatabaseName(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("Database name must not be null or empty");
        }
        return dataSourceCache.computeIfAbsent(databaseName, dbName -> {
            logger.info("Initializing dynamic DataSource connection pool for database '{}'", dbName);

            // Build HikariDataSource
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", dbHost, dbPort, dbName);
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(dbUsername);
            config.setPassword(dbPassword);
            config.setDriverClassName("org.postgresql.Driver");
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(20000);
            config.setIdleTimeout(60000);
            config.setMaxLifetime(120000);
            config.setInitializationFailTimeout(-1);
            config.setPoolName("DynamicBankPool-" + dbName);

            return new HikariDataSource(config);
        });
    }

    private String extractDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            return null;
        }
        int qIdx = jdbcUrl.indexOf('?');
        String clean = (qIdx > 0) ? jdbcUrl.substring(0, qIdx) : jdbcUrl;
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < clean.length() - 1) {
            return clean.substring(lastSlash + 1);
        }
        return null;
    }

    @PreDestroy
    public void closeAll() {
        dataSourceCache.forEach((name, ds) -> {
            try {
                if (ds != null && !ds.isClosed()) {
                    ds.close();
                }
            } catch (Exception ignored) {}
        });
        dataSourceCache.clear();
    }
}
