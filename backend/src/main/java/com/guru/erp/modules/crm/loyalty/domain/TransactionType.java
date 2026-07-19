package com.guru.erp.modules.crm.loyalty.domain;

/**
 * CustomerTransaction.type — read-projection of a POS/Sales transaction
 * (reference {@code TransactionType}, US-041). Wire values already match the
 * Java constant names.
 */
public enum TransactionType {
    SALE,
    REFUND
}
