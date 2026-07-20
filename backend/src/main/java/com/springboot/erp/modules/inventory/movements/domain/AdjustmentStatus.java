package com.springboot.erp.modules.inventory.movements.domain;

/**
 * ENT-043 StockAdjustment lifecycle (reference {@code app.inventory.constants.AdjustmentStatus}).
 *
 * <p>Persisted as the human wire label via {@link AdjustmentStatusConverter} to satisfy the
 * {@code ck_stock_adjustments_status} check constraint and align with the reference data.
 *
 * <p>Workflow (reference {@code ADJUSTMENT_TRANSITIONS}): DRAFT → PENDING_APPROVAL / APPROVED →
 * POSTED (writes ledger movements + emits the outbox posting event) → REVERSED (on delete of a
 * posted adjustment). The task brief's "draft→approved→posted" is the happy path through this.
 */
public enum AdjustmentStatus {

    DRAFT("Draft"),
    PENDING_APPROVAL("Pending Approval"),
    APPROVED("Approved"),
    POSTED("Posted"),
    REVERSED("Reversed");

    private final String wire;

    AdjustmentStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static AdjustmentStatus fromWire(String wire) {
        for (AdjustmentStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown adjustment status: " + wire);
    }
}
