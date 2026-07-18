package com.guru.erp.modules.inventory.stock.domain;

/**
 * Kind of stock movement a {@link StockLedger} row records (reference
 * {@code MovementType}). Stored verbatim in {@code stock_ledger.movement_type}.
 *
 * <p>{@code REVALUATION} is a value-only movement: it carries a non-zero
 * {@code value_delta_amount} with {@code qty_signed = 0}, so on-hand quantity is
 * untouched while inventory value shifts to the latest approved bill cost.
 */
public enum MovementType {
    RECEIPT,
    SALE,
    RETURN,
    TRANSFER_OUT,
    TRANSFER_IN,
    ADJUSTMENT,
    WRITE_OFF,
    RESERVE,
    UNRESERVE,
    STATUS_CHANGE,
    REVALUATION
}
