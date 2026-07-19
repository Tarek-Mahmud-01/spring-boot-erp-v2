package com.guru.erp.modules.pos.registers.domain;

/**
 * PosTillSession lifecycle (reference {@code app.pos.constants.TillSessionStatus}).
 * Persisted UPPERCASE via {@code @Enumerated(STRING)} — matches the reference
 * wire values verbatim ({@code "OPEN"} / {@code "CLOSED"}).
 *
 * <p>Workflow (reference FR-191..194): OPEN -&gt; CLOSED. One open session per
 * register at a time (partial unique index reproduced in the migration).
 */
public enum TillSessionStatus {
    OPEN,
    CLOSED
}
