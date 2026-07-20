package com.springboot.erp.modules.procurement.returns.domain;

/**
 * ENT-031 SupplierReturn lifecycle (reference {@code app.procurement.constants.ReturnStatus}).
 *
 * <p>Persisted as the human wire label ({@code "Draft"} / {@code "Confirmed"}) via
 * {@link ReturnStatusConverter} to satisfy the {@code ck_supplier_returns_status} check constraint
 * and align with the reference data.
 *
 * <p>Workflow (reference {@code transition_return}): DRAFT → CONFIRMED. On CONFIRMED the reference
 * posts the V-007 debit-note journal and the negative RETURN stock rows; here those cross-slice
 * effects are emitted as outbox events (see {@code ReturnPostingService}).
 */
public enum ReturnStatus {

    DRAFT("Draft"),
    CONFIRMED("Confirmed");

    private final String wire;

    ReturnStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static ReturnStatus fromWire(String wire) {
        for (ReturnStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown return status: " + wire);
    }
}
