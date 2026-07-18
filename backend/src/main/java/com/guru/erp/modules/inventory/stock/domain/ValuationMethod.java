package com.guru.erp.modules.inventory.stock.domain;

/**
 * Per-company inventory valuation method (reference {@code ValuationMethod}).
 * Stored verbatim in {@code inventory_valuation_configs.method}, matching the
 * {@code ck_valuation_config_method} check constraint.
 */
public enum ValuationMethod {
    MOVING_AVERAGE,
    FIFO,
    STANDARD
}
