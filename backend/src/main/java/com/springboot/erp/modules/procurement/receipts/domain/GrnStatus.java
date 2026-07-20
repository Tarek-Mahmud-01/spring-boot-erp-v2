package com.springboot.erp.modules.procurement.receipts.domain;

/**
 * ENT-029 GoodsReceipt lifecycle (reference {@code app.procurement.constants.GRNStatus}).
 *
 * <p>Persisted as the human wire label via {@link GrnStatusConverter} to satisfy the
 * {@code ck_goods_receipts_status} check constraint and align with the reference data.
 *
 * <p>Workflow (reference {@code goods_receipts.py}): DRAFT → APPROVED (non-stock transitions),
 * APPROVED → PARTIALLY_RECEIVED, and DRAFT/APPROVED/PARTIALLY_RECEIVED → RECEIVED (the "confirm"
 * action, which posts stock via the outbox and rolls received quantities up to the PO).
 * {@code CONFIRMED} is a legacy value kept for backward compatibility with migrated rows —
 * treated identically to {@code RECEIVED} for the "already confirmed" guard.
 */
public enum GrnStatus {

    DRAFT("Draft"),
    APPROVED("Approved"),
    PARTIALLY_RECEIVED("Partially Received"),
    RECEIVED("Received"),
    /** Legacy — pre-migration rows; a received receipt in all but name. */
    CONFIRMED("Confirmed");

    private final String wire;

    GrnStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    /** True for the two terminal "goods on hand" states that must not re-post stock. */
    public boolean isReceived() {
        return this == RECEIVED || this == CONFIRMED;
    }

    public static GrnStatus fromWire(String wire) {
        for (GrnStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown GRN status: " + wire);
    }
}
