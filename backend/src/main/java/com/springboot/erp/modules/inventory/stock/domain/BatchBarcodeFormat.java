package com.springboot.erp.modules.inventory.stock.domain;

/**
 * Barcode format carried by a {@link ProductBatch} (reference
 * {@code product_batches.barcode_format} check constraint). Persisted verbatim.
 */
public enum BatchBarcodeFormat {
    EAN13,
    UPCA,
    CODE128,
    WEIGHTED,
    PLU,
    OTHER
}
