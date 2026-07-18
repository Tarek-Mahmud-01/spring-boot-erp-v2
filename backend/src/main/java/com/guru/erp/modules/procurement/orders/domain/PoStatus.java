package com.guru.erp.modules.procurement.orders.domain;

/**
 * ENT-028 PurchaseOrder lifecycle (reference {@code app.procurement.constants.POStatus}).
 *
 * <p>Persisted as the human wire label via {@link PoStatusConverter} to match the reference data
 * and the {@code ck_purchase_orders_status} check constraint.
 *
 * <p>Workflow (reference {@code PO_TRANSITIONS}) — both the strict 6-step PRD path
 * (Draft → Submitted → Approved → Sent → Received → Closed) and the simplified shortcut the
 * frontend uses (Draft → Approved → Received) are allowed:
 * DRAFT → (SUBMITTED | APPROVED | CANCELLED); SUBMITTED → (APPROVED | CANCELLED);
 * APPROVED → (SENT | PARTIALLY_RECEIVED | RECEIVED | CANCELLED);
 * SENT → (PARTIALLY_RECEIVED | RECEIVED | CLOSED);
 * PARTIALLY_RECEIVED → (RECEIVED | CLOSED); RECEIVED → CLOSED. CLOSED and CANCELLED are terminal.
 */
public enum PoStatus {

    DRAFT("Draft"),
    SUBMITTED("Submitted"),
    APPROVED("Approved"),
    SENT("Sent"),
    PARTIALLY_RECEIVED("Partially Received"),
    RECEIVED("Received"),
    CLOSED("Closed"),
    CANCELLED("Cancelled");

    private final String wire;

    PoStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static PoStatus fromWire(String wire) {
        for (PoStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown PO status: " + wire);
    }
}
