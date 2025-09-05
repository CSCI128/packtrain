package edu.mines.packtrain.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Slf4j
public class DatabaseConfig {
    private final ClassPathResource masterMigrationStatsSqlFile;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        masterMigrationStatsSqlFile = new ClassPathResource("sql/master_migration_stats_view.sql");
    }

    @EventListener(ContextRefreshedEvent.class)
    public void setupMasterMigrationStatsView() {
        log.debug("Attempting to setup master migration stats view");
        try (InputStream is = masterMigrationStatsSqlFile.getInputStream()) {
            byte[] data = is.readAllBytes();
            String decoded = new String(data, StandardCharsets.UTF_8);

            jdbcTemplate.execute(decoded);

        } catch (IOException exception) {
            log.error("Failed to setup master_migration_stats_view view", exception);
        }

    }

}
