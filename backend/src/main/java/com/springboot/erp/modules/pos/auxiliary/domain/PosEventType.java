package com.springboot.erp.modules.pos.auxiliary.domain;

/**
 * PosEvent.type (reference {@code app.pos.constants.PosEventType}) — the append-only POS-domain
 * event kinds (FR-AU-013 / FR-198 / FR-25.8). Distinct from the platform {@code audit_logs} trail:
 * this is a receipt/journal-facing operational timeline (age verification, offline conflicts,
 * manual discounts, peripheral failures), not a generic before/after mutation record. Persisted
 * verbatim via {@code @Enumerated(STRING)}.
 */
public enum PosEventType {
    AGE_VERIFICATION_CONFIRMED,
    AGE_VERIFICATION_REFUSED,
    OFFLINE_SYNC_CONFLICT,
    OFFLINE_MAX_DURATION_BLOCKED,
    MANUAL_DISCOUNT_APPLIED,
    /** FR-25.8 — runtime peripheral failures (printer offline, drawer signal failed, ...). */
    PERIPHERAL_FAILURE,
    /** US-034 FR-178 — a manager step-up override was granted or denied at a register. */
    MANAGER_OVERRIDE_GRANTED,
    MANAGER_OVERRIDE_DENIED
}
