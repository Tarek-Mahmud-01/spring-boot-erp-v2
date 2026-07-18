package com.guru.erp.modules.product.pricing.domain;

/**
 * ENT PriceHistory.field — which base-price field a history row records.
 * Mirrors the reference {@code field IN ('cost','sell')} check
 * ({@code ck_product_price_history_field}).
 */
public enum PriceField {
    COST("cost"),
    SELL("sell");

    private final String value;

    PriceField(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static PriceField fromValue(String value) {
        for (PriceField f : values()) {
            if (f.value.equals(value)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown price field: " + value);
    }
}
