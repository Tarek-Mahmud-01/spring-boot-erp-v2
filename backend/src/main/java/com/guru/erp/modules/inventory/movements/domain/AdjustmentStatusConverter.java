package com.guru.erp.modules.inventory.movements.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link AdjustmentStatus} as its human wire label ({@code "Draft"} …) rather than the
 * enum constant name, matching the reference model and the {@code ck_stock_adjustments_status}
 * check constraint.
 */
@Converter(autoApply = false)
public class AdjustmentStatusConverter implements AttributeConverter<AdjustmentStatus, String> {

    @Override
    public String convertToDatabaseColumn(AdjustmentStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public AdjustmentStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AdjustmentStatus.fromWire(dbData);
    }
}
