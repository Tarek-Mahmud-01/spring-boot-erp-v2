package com.guru.erp.modules.procurement.returns.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link ReturnStatus} as its human wire label ({@code "Draft"} / {@code "Confirmed"})
 * rather than the enum constant name, matching the reference model and the
 * {@code ck_supplier_returns_status} check constraint.
 */
@Converter(autoApply = false)
public class ReturnStatusConverter implements AttributeConverter<ReturnStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReturnStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public ReturnStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ReturnStatus.fromWire(dbData);
    }
}
