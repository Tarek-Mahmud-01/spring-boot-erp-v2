package com.guru.erp.modules.procurement.landed.domain;

/**
 * Per-unit barcode lifecycle (reference {@code app.procurement.constants.UnitBarcodeStatus}).
 *
 * <p>Persisted as the lowercase wire value ({@code "available"} …) via
 * {@link UnitBarcodeStatusConverter} to satisfy the {@code ck_unit_barcodes_status} check
 * constraint and match the reference data.
 */
public enum UnitBarcodeStatus {

    AVAILABLE("available"),
    SOLD("sold"),
    RETURNED("returned"),
    DAMAGED("damaged"),
    RESERVED("reserved");

    private final String wire;

    UnitBarcodeStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static UnitBarcodeStatus fromWire(String wire) {
        for (UnitBarcodeStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown unit barcode status: " + wire);
    }
}
