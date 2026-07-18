package com.guru.erp.modules.procurement.receipts.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link DiscrepancyType} as its human wire label, matching the reference and the
 * {@code ck_grn_lines_discrepancy_type} check constraint. Null-safe (the column is nullable).
 */
@Converter(autoApply = false)
public class DiscrepancyTypeConverter implements AttributeConverter<DiscrepancyType, String> {

    @Override
    public String convertToDatabaseColumn(DiscrepancyType attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public DiscrepancyType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DiscrepancyType.fromWire(dbData);
    }
}
