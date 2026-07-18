package com.guru.erp.modules.inventory.counts.domain;

/**
 * ENT-046 BarcodeNomenclatureRule rule type (reference: the {@code rule_type}
 * column, {@code ck_barcode_nomenclature_rule_type}). Persisted via
 * {@link BarcodeRuleTypeConverter} as the lowercase wire value.
 *
 * <ul>
 *   <li>{@code WEIGHTED_QTY} ("weighted_qty") — embedded measure is a quantity
 *       (kg / g / ltr).</li>
 *   <li>{@code WEIGHTED_PRICE} ("weighted_price") — embedded measure is a
 *       pre-calculated line price.</li>
 * </ul>
 */
public enum BarcodeRuleType {
    WEIGHTED_QTY("weighted_qty"),
    WEIGHTED_PRICE("weighted_price");

    private final String wire;

    BarcodeRuleType(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static BarcodeRuleType fromWire(String value) {
        for (BarcodeRuleType t : values()) {
            if (t.wire.equals(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown barcode rule type: " + value);
    }
}
