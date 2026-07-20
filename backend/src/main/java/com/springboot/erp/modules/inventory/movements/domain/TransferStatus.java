package com.springboot.erp.modules.inventory.movements.domain;

/**
 * ENT-042 StockTransfer lifecycle (reference {@code app.inventory.constants.TransferStatus}).
 *
 * <p>The reference stores the human wire label ({@code "Draft"}, {@code "Approved"}, …) in the
 * status column, guarded by a check constraint. We persist that same label via
 * {@link TransferStatusConverter} so the DDL check constraint and the reference data agree; the
 * enum constant name is the internal Java handle.
 *
 * <p>Workflow (reference {@code TRANSFER_TRANSITIONS}): DRAFT → APPROVED (confirm, stocks out of
 * source) → PARTIALLY_COMPLETE / COMPLETE (receive into destination). The task brief's
 * "draft→in_transit→received" is the same shape — confirm moves stock in-transit out of source,
 * receive lands it — but we keep the reference's exact labels.
 */
public enum TransferStatus {

    DRAFT("Draft"),
    APPROVED("Approved"),
    PARTIALLY_COMPLETE("Partially Complete"),
    COMPLETE("Complete");

    private final String wire;

    TransferStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static TransferStatus fromWire(String wire) {
        for (TransferStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown transfer status: " + wire);
    }
}
