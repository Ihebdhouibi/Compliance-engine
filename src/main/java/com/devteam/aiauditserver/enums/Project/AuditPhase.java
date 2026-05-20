package com.devteam.aiauditserver.enums.Project;

/**
 * Lifecycle phase of a two-level audit. Distinct from {@link AuditStatus}
 * which still tracks the legacy single-pass audit workflow (kept for
 * backward compatibility on existing records).
 *
 * Flow:
 *   DRAFT_L1 → L1_SUBMITTED → ROUTED → QUOTE_ACCEPTED → DRAFT_L2 →
 *   L2_SUBMITTED → ASSIGNED → IN_PROGRESS → COMPLETED
 *
 * LEGACY is assigned to all audits created before this two-level feature
 * shipped, so old data never satisfies the new phase-based queries.
 */
public enum AuditPhase {
    DRAFT_L1,
    L1_SUBMITTED,
    ROUTED,
    QUOTE_ACCEPTED,
    DRAFT_L2,
    L2_SUBMITTED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    LEGACY
}
