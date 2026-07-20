package com.springboot.erp.modules.procurement.orders.domain;

/**
 * ENT-027a PurchaseRequisitionLine status (reference {@code PRLineStatus}). Persisted as the wire
 * label via {@link PrLineStatusConverter} — matches {@code ck_pr_lines_status}.
 */
public enum PrLineStatus {

    OPEN("Open"),
    CONVERTED("Converted"),
    REJECTED("Rejected");

    private final String wire;

    PrLineStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static PrLineStatus fromWire(String wire) {
        for (PrLineStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown PR line status: " + wire);
    }
}
