package com.guru.erp.modules.inventory.stock.domain;

/**
 * The kind of source document that produced a stock movement (reference
 * {@code SourceDocType}). Stored verbatim in {@code stock_ledger.source_doc_type}
 * and {@code product_batches.source_doc_type}; the paired {@code source_doc_id}
 * is the ULID public id of that document.
 */
public enum SourceDocType {
    GRN,
    SALE,
    TRANSFER,
    ADJUSTMENT,
    WRITE_OFF,
    CYCLE_COUNT,
    MANUAL,
    STOCK_OPENING,
    RETURN,
    BILL,
    LANDED_COST
}
