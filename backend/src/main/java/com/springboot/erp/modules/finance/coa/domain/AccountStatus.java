package com.springboot.erp.modules.finance.coa.domain;

/**
 * Reference {@code app.finance.constants.AccountStatus}. Persisted lower-case
 * ({@code active} / {@code inactive}) to match the reference's literal DB
 * values and the {@code ck_accounts_status} check, via
 * {@link AccountStatusConverter}.
 */
public enum AccountStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String wire;

    AccountStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static AccountStatus fromWire(String wire) {
        for (AccountStatus s : values()) {
            if (s.wire.equals(wire)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown account status: " + wire);
    }
}
