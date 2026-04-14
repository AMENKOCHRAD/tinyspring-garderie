package com.tinyspring.garderie.config.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransportSchemaInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TransportSchemaInitializer.class);

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

        Integer trajetIdNotNull = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'demandes_transport'
                  AND column_name = 'trajet_id'
                  AND is_nullable = 'NO'
                """,
                Integer.class
        );

        if (trajetIdNotNull != null && trajetIdNotNull > 0) {
            jdbcTemplate.execute("ALTER TABLE demandes_transport MODIFY COLUMN trajet_id BIGINT NULL");
            logger.info("Schema transport mis a jour: demandes_transport.trajet_id accepte maintenant NULL");
        }

        ajouterColonneSiAbsente("ALTER TABLE demandes_transport ADD COLUMN sens_trajet VARCHAR(50) NULL", "sens_trajet");
        ajouterColonneSiAbsente("ALTER TABLE demandes_transport ADD COLUMN adresse_maison VARCHAR(255) NULL", "adresse_maison");
        ajouterColonneSiAbsente("ALTER TABLE demandes_transport ADD COLUMN latitude_maison DOUBLE NULL", "latitude_maison");
        ajouterColonneSiAbsente("ALTER TABLE demandes_transport ADD COLUMN longitude_maison DOUBLE NULL", "longitude_maison");
        ajouterColonneTrajetSiAbsente("ALTER TABLE trajets ADD COLUMN zone_desservie VARCHAR(120) NULL", "zone_desservie");
        ajouterColonneTrajetSiAbsente("ALTER TABLE trajets ADD COLUMN latitude_destination DOUBLE NULL", "latitude_destination");
        ajouterColonneTrajetSiAbsente("ALTER TABLE trajets ADD COLUMN longitude_destination DOUBLE NULL", "longitude_destination");

        jdbcTemplate.execute(
                """
                UPDATE demandes_transport
                SET sens_trajet = COALESCE(sens_trajet, 'MAISON_VERS_GARDERIE'),
                    adresse_maison = COALESCE(adresse_maison, point_ramassage),
                    latitude_maison = COALESCE(latitude_maison, 36.8065),
                    longitude_maison = COALESCE(longitude_maison, 10.1815)
                """
        );

        jdbcTemplate.execute("ALTER TABLE demandes_transport MODIFY COLUMN sens_trajet VARCHAR(50) NOT NULL");
        jdbcTemplate.execute("ALTER TABLE demandes_transport MODIFY COLUMN adresse_maison VARCHAR(255) NOT NULL");
        jdbcTemplate.execute("ALTER TABLE demandes_transport MODIFY COLUMN latitude_maison DOUBLE NOT NULL");
        jdbcTemplate.execute("ALTER TABLE demandes_transport MODIFY COLUMN longitude_maison DOUBLE NOT NULL");
    }

    private void ajouterColonneSiAbsente(String sql, String columnName) {
        ajouterColonneSiAbsente("demandes_transport", sql, columnName);
    }

    private void ajouterColonneTrajetSiAbsente(String sql, String columnName) {
        ajouterColonneSiAbsente("trajets", sql, columnName);
    }

    private void ajouterColonneSiAbsente(String tableName, String sql, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );

        if (count != null && count == 0) {
            jdbcTemplate.execute(sql);
            logger.info("Schema transport mis a jour: colonne {} ajoutee", columnName);
        }
    }
}
