package com.guru.erp.modules.procurement.receipts.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link GrnStatus} as its human wire label ({@code "Draft"} …) rather than the enum
 * constant name, matching the reference model and the {@code ck_goods_receipts_status} check
 * constraint.
 */
@Converter(autoApply = false)
public class GrnStatusConverter implements AttributeConverter<GrnStatus, String> {

    @Override
    public String convertToDatabaseColumn(GrnStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public GrnStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : GrnStatus.fromWire(dbData);
    }
}
