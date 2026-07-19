package com.guru.erp.modules.pos.registers.domain;

/**
 * ENT-060a.type (reference {@code app.pos.constants.PeripheralType}). Persisted
 * UPPERCASE via {@code @Enumerated(STRING)} — matches the reference wire values
 * verbatim.
 */
public enum PeripheralType {
    RECEIPT_PRINTER,
    CASH_DRAWER,
    SCANNER,
    CARD_READER,
    LABEL_PRINTER
}
