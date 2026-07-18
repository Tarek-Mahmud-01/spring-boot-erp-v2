package com.guru.erp.modules.settings.company.domain;

/**
 * ENT-001 company lifecycle status. Wire value is the lower-case string
 * ({@code active} / {@code inactive}) to match the reference contract; the
 * derived value maps 1:1 to the {@code status} column.
 */
public enum CompanyStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String wire;

    CompanyStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static CompanyStatus fromWire(String value) {
        for (CompanyStatus s : values()) {
            if (s.wire.equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown company status: " + value);
    }
}
