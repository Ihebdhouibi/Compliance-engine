package com.devteam.aiauditserver.Tools.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-shot migration: rename the legacy enum value RICS_AUDIT to RICS_RESPONSIBLE_AI
 * in tables that store AuditType as a string.
 *
 * Runs before FirstTimeInitializer so any downstream seeding sees consistent data.
 */
@Component
@Order(0)
public class AuditTypeRenameMigration implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AuditTypeRenameMigration.class);

    private final JdbcTemplate jdbc;

    public AuditTypeRenameMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        renameInTable("audit_form_templates");
        renameInTable("audit_requests");
        alterColumnToText("audit_form_fields", "label");
        alterColumnToText("audit_form_fields", "placeholder");
        alterColumnToText("audit_request_answers", "field_label");
    }

    private void renameInTable(String table) {
        try {
            int updated = jdbc.update(
                    "UPDATE " + table + " SET audit_type = ? WHERE audit_type = ?",
                    "RICS_RESPONSIBLE_AI", "RICS_AUDIT");
            if (updated > 0) {
                logger.info("AuditTypeRenameMigration: renamed {} row(s) in {}", updated, table);
            }
        } catch (Exception e) {
            // Table may not exist yet on first ever boot — that's fine.
            logger.debug("AuditTypeRenameMigration: skipped {} ({})", table, e.getMessage());
        }
    }

    private void alterColumnToText(String table, String column) {
        try {
            jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN " + column + " TYPE TEXT");
            logger.info("AuditTypeRenameMigration: altered {}.{} -> TEXT", table, column);
        } catch (Exception e) {
            logger.debug("AuditTypeRenameMigration: alter {}.{} skipped ({})",
                    table, column, e.getMessage());
        }
    }
}
