package com.los.bank.user.datasource;

import com.los.bank.user.context.BankContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;

/**
 * Dynamic routing DataSource that routes queries to the appropriate bank
 * database
 * based on the current BankContext lookup key.
 * Does NOT eagerly connect to any bank database at startup — connections are
 * created lazily on first use.
 */
public class BankRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger logger = LoggerFactory.getLogger(BankRoutingDataSource.class);

    private final DynamicBankDataSourceManager dataSourceManager;
    private volatile boolean defaultInitialized = false;

    public BankRoutingDataSource(DynamicBankDataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
        // Initialize with empty target map — no eager connection to any bank DB
        setTargetDataSources(new HashMap<>());
        // Use a placeholder; determineTargetDataSource() is overridden so this is never
        // actually used
        setDefaultTargetDataSource(new LazyDefaultDataSource());
        afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String dbName = BankContext.getCurrentDatabase();
        logger.debug("[BankRoutingDataSource] Current lookup key from BankContext: '{}'", dbName);
        return dbName;
    }

    @Override
    protected DataSource determineTargetDataSource() {
        Object lookupKey = determineCurrentLookupKey();
        String targetDb;

        if (lookupKey != null && !lookupKey.toString().trim().isEmpty()) {
            targetDb = lookupKey.toString().trim();
        } else {
            targetDb = dataSourceManager.resolveDefaultBankDatabase();
            logger.debug("[BankRoutingDataSource] No explicit BankContext found; falling back to default bank DB: '{}'",
                    targetDb);
        }

        if (targetDb == null || targetDb.trim().isEmpty()) {
            return new LazyDefaultDataSource();
        }

        DataSource ds = dataSourceManager.getOrCreateDataSource(targetDb);
        if (ds == null) {
            return new LazyDefaultDataSource();
        }
        logger.debug("[BankRoutingDataSource] Routing connection request to target database '{}'", targetDb);
        return ds;
    }

    /**
     * Placeholder DataSource that is never actually used because
     * determineTargetDataSource() is overridden.
     * Required by AbstractRoutingDataSource to satisfy the non-null
     * defaultTargetDataSource contract.
     */
    private static class LazyDefaultDataSource implements DataSource {
        @Override
        public java.sql.Connection getConnection() throws java.sql.SQLException {
            throw new java.sql.SQLException(
                    "No bank database context set. Set BankContext before accessing bank data.");
        }

        @Override
        public java.sql.Connection getConnection(String username, String password) throws java.sql.SQLException {
            throw new java.sql.SQLException(
                    "No bank database context set. Set BankContext before accessing bank data.");
        }

        @Override
        public java.io.PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getLogger(getClass().getName());
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
            throw new java.sql.SQLException("Not a wrapper");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}
