package com.springboot.erp.modules.pos.transactions.domain;

/**
 * ENT-PosTransaction.type (reference {@code app.pos.constants.PosTransactionType}). This slice
 * only creates {@code SALE} transactions; {@code REFUND} rows are created by the refunds
 * sub-slice (out of scope here) but share this table/enum, so both values are declared.
 */
public enum PosTransactionType {
    SALE,
    REFUND
}
