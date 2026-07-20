package com.springboot.erp.modules.procurement.bills.domain;

/**
 * ENT-034 AmountReceived lifecycle (reference {@code amount_received._TRANSITIONS}).
 *
 * <p>Persisted as the human wire label via {@link AmountReceivedStatusConverter}, matching the
 * reference and the {@code ck_amount_received_status} check constraint. Workflow:
 * DRAFT → APPROVED → CONFIRMED → VOIDED (Draft may also skip straight to Confirmed / Voided).
 */
public enum AmountReceivedStatus {

    DRAFT("Draft"),
    APPROVED("Approved"),
    CONFIRMED("Confirmed"),
    VOIDED("Voided");

    private final String wire;

    AmountReceivedStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static AmountReceivedStatus fromWire(String wire) {
        for (AmountReceivedStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown amount-received status: " + wire);
    }
}
