package com.springboot.erp.modules.pos.transactions.domain;

/**
 * ENT-PosTransaction lifecycle (reference {@code app.pos.constants.PosTransactionStatus}).
 *
 * <p>Persisted verbatim via {@code @Enumerated(STRING)} — the wire values already match the Java
 * constant names ({@code OPEN}, {@code PARKED}, {@code COMPLETED}, {@code VOIDED}), so no
 * AttributeConverter is needed (unlike the lowercase-wire {@code PoStatus} family).
 *
 * <p>Workflow (reference sale lifecycle — cart, then payment, then completion; park/resume is a
 * side-trip back to OPEN; void is reachable from either OPEN or COMPLETED):
 * OPEN → (PARKED | COMPLETED | VOIDED); PARKED → OPEN; COMPLETED → VOIDED. VOIDED is terminal.
 */
public enum PosTransactionStatus {
    OPEN,
    PARKED,
    COMPLETED,
    VOIDED
}
