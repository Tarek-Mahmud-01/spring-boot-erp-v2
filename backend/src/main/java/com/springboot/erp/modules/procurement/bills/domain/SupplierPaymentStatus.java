package com.springboot.erp.modules.procurement.bills.domain;

/**
 * ENT-033 SupplierPayment lifecycle (reference {@code SUPPLIER_PAYMENT_TRANSITIONS}).
 *
 * <p>Persisted as the human wire label via {@link SupplierPaymentStatusConverter}, matching the
 * reference and the {@code ck_supplier_payments_status} check constraint. Workflow:
 * DRAFT → APPROVED → PARTIALLY_PAID → PAID.
 */
public enum SupplierPaymentStatus {

    DRAFT("Draft"),
    APPROVED("Approved"),
    PARTIALLY_PAID("Partially Paid"),
    PAID("Paid");

    private final String wire;

    SupplierPaymentStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static SupplierPaymentStatus fromWire(String wire) {
        for (SupplierPaymentStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown supplier payment status: " + wire);
    }
}
