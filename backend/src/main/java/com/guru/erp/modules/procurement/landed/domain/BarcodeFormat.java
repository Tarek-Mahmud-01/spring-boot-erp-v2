package com.guru.erp.modules.procurement.landed.domain;

/**
 * Barcode symbology stored on a {@link UnitBarcode} ({@code ck_unit_barcodes_format}). Persisted as
 * the enum name (already UPPERCASE and matching the reference values) via
 * {@code @Enumerated(STRING)} — no converter needed.
 */
public enum BarcodeFormat {
    EAN13,
    UPCA,
    CODE128,
    OTHER
}
