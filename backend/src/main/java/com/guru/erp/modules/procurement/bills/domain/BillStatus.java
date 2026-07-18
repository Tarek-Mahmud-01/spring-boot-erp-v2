package com.guru.erp.modules.procurement.bills.domain;

/**
 * ENT-030 SupplierBill lifecycle (reference {@code app.procurement.constants.BillStatus}).
 *
 * <p>Persisted as the human wire label via {@link BillStatusConverter} to match the reference data
 * and keep the status column readable. Workflow: DRAFT → (auto-APPROVED on a clean 3-way match, or
 * manual approve) → INVOICED_NOT_RECEIVED (bill-first) → PAID / PARTIALLY_PAID (settled by
 * payments) or CANCELLED.
 */
public enum BillStatus {

    DRAFT("Draft"),
    RECEIVED("Received"),
    APPROVED_FOR_PAYMENT("Approved"),
    PAID("Paid"),
    PARTIALLY_PAID("Partially Received"),
    CANCELLED("Cancelled"),
    INVOICED_NOT_RECEIVED("Invoiced Not Received");

    private final String wire;

    BillStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static BillStatus fromWire(String wire) {
        for (BillStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown bill status: " + wire);
    }
}
