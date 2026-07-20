package com.springboot.erp.modules.inventory.counts.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link BarcodeRuleType} as its lowercase wire value
 * ("weighted_qty" / "weighted_price"), matching the
 * {@code ck_barcode_nomenclature_rule_type} check constraint and the reference
 * model.
 */
@Converter
public class BarcodeRuleTypeConverter implements AttributeConverter<BarcodeRuleType, String> {

    @Override
    public String convertToDatabaseColumn(BarcodeRuleType attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public BarcodeRuleType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BarcodeRuleType.fromWire(dbData);
    }
}
