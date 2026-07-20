package com.springboot.erp.modules.pos.auxiliary.domain;

/**
 * PosRefund.mode (reference {@code app.pos.constants.RefundMode}). Persisted verbatim via
 * {@code @Enumerated(STRING)} — the wire values already match the Java constant names, so no
 * AttributeConverter is needed (unlike the lowercase-wire {@code CompanyStatus} family).
 */
public enum RefundMode {
    /** US-034 FR-177 — refund against the original receipt/transaction. */
    RECEIPT_LINKED,
    /** US-034 FR-178 — refund with no original receipt, manager-gated. */
    NO_RECEIPT
}
