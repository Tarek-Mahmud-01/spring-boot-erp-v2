package com.springboot.erp.modules.procurement.orders.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link PoStatus} as its human wire label ({@code "Draft"}, {@code "Partially Received"}
 * …) rather than the enum constant name, matching the reference model and the
 * {@code ck_purchase_orders_status} check constraint.
 */
@Converter(autoApply = false)
public class PoStatusConverter implements AttributeConverter<PoStatus, String> {

    @Override
    public String convertToDatabaseColumn(PoStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public PoStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PoStatus.fromWire(dbData);
    }
}
