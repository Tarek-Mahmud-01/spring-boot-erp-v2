package com.guru.erp.modules.product.catalog.domain;

/**
 * ENT-011b.format — FR-057. Persisted as the constant name matching the
 * reference {@code StrEnum} and the {@code ck_product_barcodes_format} check.
 */
public enum BarcodeFormat {
    EAN13,
    UPCA,
    CODE128,
    /** Scale-printed variable-weight barcode (GS1 prefix 02/20-29). */
    WEIGHTED,
    /** Short numeric cashier-entry code (e.g. "4011" = bananas). */
    PLU,
    OTHER
}
