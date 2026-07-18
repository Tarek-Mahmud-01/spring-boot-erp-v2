package com.guru.erp.modules.procurement.receipts.domain;

/**
 * ENT-029a GoodsReceiptLine discrepancy classification (reference
 * {@code app.procurement.constants.DiscrepancyType}). Persisted as the human wire label via
 * {@link DiscrepancyTypeConverter}, guarded by {@code ck_grn_lines_discrepancy_type}.
 */
public enum DiscrepancyType {

    SHORT("Short"),
    DAMAGED("Damaged"),
    WRONG_ITEM("Wrong Item");

    private final String wire;

    DiscrepancyType(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static DiscrepancyType fromWire(String wire) {
        for (DiscrepancyType d : values()) {
            if (d.wire.equals(wire)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown discrepancy type: " + wire);
    }
}
