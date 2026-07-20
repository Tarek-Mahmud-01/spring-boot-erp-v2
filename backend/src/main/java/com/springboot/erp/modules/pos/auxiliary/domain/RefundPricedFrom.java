package com.springboot.erp.modules.pos.auxiliary.domain;

/**
 * PosRefund.pricedFrom (reference {@code app.pos.constants.RefundPricedFrom}) — how a no-receipt
 * refund's unit price was determined; {@code null} for a receipt-linked refund (reuses the
 * original line's price verbatim). Persisted verbatim via {@code @Enumerated(STRING)}.
 */
public enum RefundPricedFrom {
    /** Receipt-linked refund — reuses the original sold line's unit price. */
    ORIGINAL,
    /** FR-178 — no qualifying manager amount; priced at the lowest sale in the lookback window. */
    LOWEST_30D,
    /** FR-178 — no qualifying sale found; the approving manager typed in the refund price. */
    MANAGER_ENTERED
}
