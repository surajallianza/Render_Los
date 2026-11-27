package com.los.bank.dbArchitecture;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configures independent Flyway migration instances for Setup Database and Bank Database.
 * Each migration configuration maintains its own migration script location and schema history table.
 */
@Configuration
public class FlywayConfig {

    @Value("${app.flyway.setup.location:classpath:db/migration/setup}")
    private String setupLocation;

    @Value("${app.flyway.setup.table:flyway_schema_history_setup}")
    private String setupTable;

    @Value("${app.flyway.setup.baseline-version:1}")
    private String setupBaselineVersion;

    @Value("${app.flyway.setup.baseline-description:Initial Setup Schema}")
    private String setupBaselineDescription;

    @Value("${app.flyway.bank.location:classpath:db/migration/bank}")
    private String bankLocation;

    @Value("${app.flyway.bank.table:flyway_schema_history_bank}")
    private String bankTable;

    @Value("${app.flyway.bank.baseline-version:1}")
    private String bankBaselineVersion;

    @Value("${app.flyway.bank.baseline-description:Initial Bank Schema}")
    private String bankBaselineDescription;

    /**
     * Independent Flyway instance for the Setup Database.
     */
    @Bean(name = "setupFlyway")
    public Flyway setupFlyway(@Qualifier("setupDataSource") DataSource setupDataSource) {
        return Flyway.configure()
                .dataSource(setupDataSource)
                .locations(setupLocation)
                .table(setupTable)
                .baselineVersion(MigrationVersion.fromVersion(setupBaselineVersion))
                .baselineDescription(setupBaselineDescription)
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateOnMigrate(false)
                .load();
    }

    /**
     * Independent Flyway instance for the Bank Database.
     */
    @Bean(name = "bankFlyway")
    public Flyway bankFlyway(@Qualifier("bankDataSource") DataSource bankDataSource) {
        return Flyway.configure()
                .dataSource(bankDataSource)
                .locations(bankLocation)
                .table(bankTable)
                .baselineVersion(MigrationVersion.fromVersion(bankBaselineVersion))
                .baselineDescription(bankBaselineDescription)
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .validateOnMigrate(false)
                .load();
    }
}
