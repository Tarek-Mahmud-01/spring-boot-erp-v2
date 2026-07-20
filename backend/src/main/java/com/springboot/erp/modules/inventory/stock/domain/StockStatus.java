package com.springboot.erp.modules.inventory.stock.domain;

/**
 * Stock bucket a ledger row belongs to (reference {@code StockStatus}). Persisted
 * verbatim (upper-case) into the {@code stock_ledger.status} check-constrained
 * column, so {@code @Enumerated(EnumType.STRING)} matches the DDL directly.
 */
public enum StockStatus {
    AVAILABLE,
    RESERVED,
    QUARANTINE,
    IN_TRANSIT
}
