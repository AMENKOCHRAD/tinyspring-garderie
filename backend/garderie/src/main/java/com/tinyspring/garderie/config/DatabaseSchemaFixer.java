package com.tinyspring.garderie.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
public class DatabaseSchemaFixer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        fixObservationTypeColumn();
        fixLegacyIndicColumnsDefaults();
        fixTraitementStatutColumn();
        ensureTraitementValidationEventsTable();
    }

    private void fixObservationTypeColumn() {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "select DATA_TYPE as dataType, CHARACTER_MAXIMUM_LENGTH as maxLen " +
                            "from information_schema.columns " +
                            "where table_schema = database() " +
                            "and table_name = 'observations_enfant' " +
                            "and column_name = 'type'"
            );

            String dataType = row.get("dataType") != null ? String.valueOf(row.get("dataType")) : "";
            Integer maxLen = null;
            if (row.get("maxLen") != null) {
                try {
                    maxLen = Integer.parseInt(String.valueOf(row.get("maxLen")));
                } catch (NumberFormatException ignore) {
                    maxLen = null;
                }
            }

            // If column is ENUM or too short -> force VARCHAR(32)
            if (!"varchar".equalsIgnoreCase(dataType) || (maxLen != null && maxLen < 32)) {
                jdbcTemplate.execute("ALTER TABLE observations_enfant MODIFY COLUMN type VARCHAR(32) NOT NULL");
            }
        } catch (Exception ignore) {
            // Don't block startup.
        }
    }


    private void fixLegacyIndicColumnsDefaults() {
        String[] cols = new String[]{
                "indic_fievre",
                "indic_fatigue",
                "indic_perte_appetit",
                "indic_somnolence",
                "indic_agitation",
                "indic_pleurs"
        };

        for (String col : cols) {
            try {
                Map<String, Object> row = jdbcTemplate.queryForMap(
                        "select DATA_TYPE as dataType, IS_NULLABLE as isNullable, COLUMN_DEFAULT as colDefault " +
                                "from information_schema.columns " +
                                "where table_schema = database() " +
                                "and table_name = 'observations_enfant' " +
                                "and column_name = '" + col + "'"
                );

                String dataType = row.get("dataType") != null ? String.valueOf(row.get("dataType")) : "";
                String isNullable = row.get("isNullable") != null ? String.valueOf(row.get("isNullable")) : "YES";
                Object colDefault = row.get("colDefault");

                boolean needsDefault = "NO".equalsIgnoreCase(isNullable) && colDefault == null;
                if (needsDefault || !"tinyint".equalsIgnoreCase(dataType)) {
                    jdbcTemplate.execute("ALTER TABLE observations_enfant MODIFY COLUMN " + col + " TINYINT(1) NOT NULL DEFAULT 0");
                }
            } catch (Exception ignore) {
                // column may not exist; ignore
            }
        }
    }

    private void fixTraitementStatutColumn() {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "select DATA_TYPE as dataType, CHARACTER_MAXIMUM_LENGTH as maxLen " +
                            "from information_schema.columns " +
                            "where table_schema = database() " +
                            "and table_name = 'traitements' " +
                            "and column_name = 'statut'"
            );

            String dataType = row.get("dataType") != null ? String.valueOf(row.get("dataType")) : "";
            Integer maxLen = null;
            if (row.get("maxLen") != null) {
                try {
                    maxLen = Integer.parseInt(String.valueOf(row.get("maxLen")));
                } catch (NumberFormatException ignore) {
                    maxLen = null;
                }
            }

            // If column is ENUM or too short -> force VARCHAR(32) and set a safe default.
            if (!"varchar".equalsIgnoreCase(dataType) || (maxLen != null && maxLen < 32)) {
                jdbcTemplate.execute("ALTER TABLE traitements MODIFY COLUMN statut VARCHAR(32) NOT NULL DEFAULT 'EN_ATTENTE_VALIDATION'");
            }
        } catch (Exception ignore) {
            // Don't block startup.
        }
    }

    private void ensureTraitementValidationEventsTable() {
        try {
            Integer exists = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = database() and table_name = 'traitement_validation_events'",
                    Integer.class
            );
            if (exists != null && exists > 0) {
                return;
            }

            jdbcTemplate.execute("""
                    CREATE TABLE traitement_validation_events (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        traitement_id BIGINT NOT NULL,
                        decision VARCHAR(32) NOT NULL,
                        source VARCHAR(32) NOT NULL,
                        confiance DOUBLE NULL,
                        facteurs_json LONGTEXT NULL,
                        note VARCHAR(500) NULL,
                        cree_le DATETIME NOT NULL,
                        cree_par_email VARCHAR(160) NULL,
                        PRIMARY KEY (id),
                        KEY idx_tve_traitement_id (traitement_id),
                        KEY idx_tve_cree_le (cree_le),
                        CONSTRAINT fk_tve_traitement FOREIGN KEY (traitement_id) REFERENCES traitements(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB
                    """);
        } catch (Exception ignore) {
            // Don't block startup.
        }
    }
}
