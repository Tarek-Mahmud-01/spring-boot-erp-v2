package com.springboot.erp.modules.procurement.orders.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link PrStatus} as its human wire label ({@code "Draft"}, {@code "Under Review"} …)
 * rather than the enum constant name, matching the reference model and the
 * {@code ck_purchase_requisitions_status} check constraint.
 */
@Converter(autoApply = false)
public class PrStatusConverter implements AttributeConverter<PrStatus, String> {

    @Override
    public String convertToDatabaseColumn(PrStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public PrStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PrStatus.fromWire(dbData);
    }
}
