package com.springboot.erp.modules.finance.coa.domain;

/**
 * The semantic role an account plays for a given module (reference
 * {@code app.finance.constants.AccountPurpose}). One (module, purpose) pair
 * maps to exactly one GL account per company via {@link AccountMapping}.
 */
public enum AccountPurpose {
    // Shared / cross-module
    ACCOUNTS_RECEIVABLE,
    ACCOUNTS_PAYABLE,
    GST_PAYABLE,
    EXCHANGE_GAIN_LOSS,

    // Inventory module
    INVENTORY_ASSET,
    INVENTORY_VARIANCE,
    COST_OF_GOODS_SOLD,

    // Procurement module
    GRN_CLEARING,
    PURCHASE_ACCOUNT,
    PURCHASE_DISCOUNT,
    PURCHASE_RETURN,
    FREIGHT_LANDED_COST,
    PURCHASE_PRICE_VARIANCE,
    INVOICE_PENDING_RECEIPT,
    BANK_ACCOUNT,

    // Sales module
    SALES_REVENUE,
    SALES_DISCOUNT,
    SALES_RETURN,
    DELIVERY_INCOME,

    // POS module
    CASH_ON_HAND,
    CARD_CLEARING,
    CASH_OVER_SHORT,
    SURCHARGE_REVENUE,

    // Finance module — counter-account for COA opening balances.
    OPENING_BALANCE_EQUITY
}
