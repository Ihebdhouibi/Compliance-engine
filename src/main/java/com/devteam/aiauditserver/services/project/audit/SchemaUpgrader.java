package com.devteam.aiauditserver.services.project.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Idempotent startup hook for schema/data migrations that Hibernate
 * hbm2ddl=update cannot perform on its own:
 *
 *  - dropping the legacy unique constraint on audit_form_templates.audit_type
 *    (so the new composite uniqueness (audit_type, level, template_version)
 *    can coexist),
 *  - back-filling AuditRequest.phase = 'LEGACY' for rows created before the
 *    two-level feature.
 *
 * Each statement is wrapped in try/catch — failures are logged and skipped
 * so the service still starts on a fresh database where the legacy
 * constraint never existed.
 */
@Component
@Order(0)
public class SchemaUpgrader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SchemaUpgrader.class);

    private final JdbcTemplate jdbc;

    public SchemaUpgrader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        // Drop legacy unique constraint on audit_type (Postgres auto-named).
        tryExecute("ALTER TABLE audit_form_templates "
                + "DROP CONSTRAINT IF EXISTS audit_form_templates_audit_type_key");
        // Some Hibernate runs create a UKxxx-named constraint instead.
        // Find any UNIQUE constraint that targets only audit_type and drop it.
        tryExecute(
                "DO $$ DECLARE r record; BEGIN "
                + "  FOR r IN SELECT conname FROM pg_constraint c "
                + "    JOIN pg_class t ON t.oid = c.conrelid "
                + "    WHERE t.relname = 'audit_form_templates' "
                + "      AND c.contype = 'u' "
                + "      AND (SELECT array_agg(att.attname) "
                + "             FROM unnest(c.conkey) k "
                + "             JOIN pg_attribute att ON att.attnum = k AND att.attrelid = c.conrelid) "
                + "          = ARRAY['audit_type']::name[] "
                + "  LOOP EXECUTE 'ALTER TABLE audit_form_templates DROP CONSTRAINT ' || quote_ident(r.conname); "
                + "  END LOOP; END $$;");

        // Backfill AuditRequest.phase for rows created before the column existed.
        tryExecute("UPDATE audit_requests SET phase = 'LEGACY' WHERE phase IS NULL");

        // Backfill new columns added to audit_form_templates so pre-existing
        // templates remain valid under the (audit_type, level, template_version)
        // unique constraint. LEVEL_2 keeps legacy questionnaires as the
        // primary RICS audit form.
        tryExecute("UPDATE audit_form_templates SET level = 'LEVEL_2' WHERE level IS NULL");
        tryExecute("UPDATE audit_form_templates SET template_version = 1 WHERE template_version IS NULL");
        tryExecute("UPDATE audit_form_templates SET active = TRUE WHERE active IS NULL");
    }

    private void tryExecute(String sql) {
        try {
            jdbc.execute(sql);
            logger.info("SchemaUpgrader applied: {}", sql.substring(0, Math.min(80, sql.length())));
        } catch (Exception ex) {
            logger.warn("SchemaUpgrader skipped ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
