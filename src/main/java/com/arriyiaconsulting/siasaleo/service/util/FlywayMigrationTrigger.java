package com.arriyiaconsulting.siasaleo.service.util;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

/**
 * Runs Flyway database migrations when the application starts.
 */
@ApplicationScoped
public class FlywayMigrationTrigger {

    private static final Logger LOGGER = Logger.getLogger(FlywayMigrationTrigger.class.getName());

    @Resource(lookup = "jdbc/Siasaleo")
    private DataSource dataSource;

    public void migrate(@Observes Startup event) {
        LOGGER.info("Starting Flyway database migration");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:database/migrations")
                .baselineOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();

        LOGGER.log(Level.INFO, "Flyway migration complete: {0} migration(s) applied, schema version {1}",
                new Object[]{result.migrationsExecuted, result.targetSchemaVersion});
    }
}
