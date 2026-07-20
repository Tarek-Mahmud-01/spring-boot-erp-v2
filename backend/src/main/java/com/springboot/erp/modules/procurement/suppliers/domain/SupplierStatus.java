package com.springboot.erp.modules.procurement.suppliers.domain;

/**
 * ENT-026 Supplier lifecycle status (reference {@code app.procurement.constants.SupplierStatus}).
 *
 * <p>Persisted as the PascalCase human wire label ({@code "Active"} / {@code "Inactive"} /
 * {@code "Blocked"}) via {@link SupplierStatusConverter} to satisfy the {@code ck_suppliers_status}
 * check constraint and align with the reference data.
 *
 * <p>Workflow: ACTIVE ⇄ INACTIVE ⇄ BLOCKED. A move to BLOCKED requires a block reason
 * (reference AC-016-3). Soft-delete flips the row to INACTIVE and stamps {@code deleted_at}.
 */
public enum SupplierStatus {

    ACTIVE("Active"),
    INACTIVE("Inactive"),
    BLOCKED("Blocked");

    private final String wire;

    SupplierStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static SupplierStatus fromWire(String wire) {
        for (SupplierStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown supplier status: " + wire);
    }
}
