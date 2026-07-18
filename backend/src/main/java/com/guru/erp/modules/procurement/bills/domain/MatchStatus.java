package com.guru.erp.modules.procurement.bills.domain;

/**
 * FR-099 three-way match outcome for a bill / bill line (reference
 * {@code app.procurement.constants.MatchStatus}). Stored as the human wire label; nullable on the
 * column, so no converter is applied — the service reads/writes the wire string directly for the
 * {@code match_status} varchar columns.
 */
public enum MatchStatus {

    MATCHED("Matched"),
    OVER_INVOICED("Over-Invoiced"),
    PRICE_MISMATCH("Price Mismatch"),
    MIXED("Mixed");

    private final String wire;

    MatchStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }
}
