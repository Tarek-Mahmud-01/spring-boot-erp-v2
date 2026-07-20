package com.springboot.erp.modules.product.pricing.domain;

/**
 * ENT-013 PriceList.status — mirrors the reference {@code PriceListStatus}
 * StrEnum ({@code active} / {@code inactive}). Stored verbatim as its lowercase
 * wire value; reproduced by {@code ck_price_lists_status} in the migration.
 */
public enum PriceListStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    PriceListStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static PriceListStatus fromValue(String value) {
        for (PriceListStatus s : values()) {
            if (s.value.equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown price list status: " + value);
    }
}
