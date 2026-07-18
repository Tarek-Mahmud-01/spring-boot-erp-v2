package com.guru.erp.modules.procurement.orders.domain;

/**
 * ENT-027 PurchaseRequisition lifecycle (reference {@code app.procurement.constants.PRStatus}).
 *
 * <p>Persisted as the human wire label via {@link PrStatusConverter} to match the reference data
 * and the {@code ck_purchase_requisitions_status} check constraint.
 *
 * <p>Workflow (reference {@code PR_TRANSITIONS}):
 * DRAFT → SUBMITTED → (UNDER_REVIEW | REJECTED); UNDER_REVIEW → (SENT_TO_SUPPLIER | CONVERTED |
 * REJECTED); SENT_TO_SUPPLIER → (CONVERTED | REJECTED). CONVERTED and REJECTED are terminal.
 */
public enum PrStatus {

    DRAFT("Draft"),
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under Review"),
    SENT_TO_SUPPLIER("Sent to Supplier"),
    CONVERTED("Converted"),
    REJECTED("Rejected");

    private final String wire;

    PrStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static PrStatus fromWire(String wire) {
        for (PrStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown PR status: " + wire);
    }
}
