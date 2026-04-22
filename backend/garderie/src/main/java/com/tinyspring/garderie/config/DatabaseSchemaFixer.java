package com.tinyspring.garderie.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fixes legacy MySQL schemas where Enum columns were created with a limited set of values
 * (or with a too-small VARCHAR) and Hibernate "update" can't safely widen/extend them.
 *
 * This app stores enums as strings (EnumType.STRING). We keep DB column as VARCHAR to avoid
 * truncation when new enum values are added (ex: JOUET, ACTIVITE).
 */
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

    /**
     * When we briefly introduced ML indicators on ObservationEnfant, Hibernate created
     * NOT NULL columns without default. If we later remove those fields from the entity,
     * inserts won't provide values and MySQL fails with "doesn't have a default value".
     *
     * We keep those legacy columns (no drop) but enforce a default 0 so inserts succeed.
     */
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
}
