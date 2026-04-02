package com.tinyspring.garderie.config.transport;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransportSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public TransportSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer dateTrajetColumnCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'trajets'
                  AND column_name = 'date_trajet'
                """,
                Integer.class
        );

        if (dateTrajetColumnCount != null && dateTrajetColumnCount == 0) {
            jdbcTemplate.execute("ALTER TABLE trajets ADD COLUMN date_trajet DATE NULL");
            jdbcTemplate.execute("UPDATE trajets SET date_trajet = CURDATE() + INTERVAL 1 DAY WHERE date_trajet IS NULL");
            jdbcTemplate.execute("ALTER TABLE trajets MODIFY COLUMN date_trajet DATE NOT NULL");
        }
    }
}
