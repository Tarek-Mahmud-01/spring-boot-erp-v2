package com.springboot.erp.modules.procurement.bills.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link BillStatus} as its human wire label ({@code "Draft"} …) to match the reference
 * model and the {@code ck_supplier_bills_status} check constraint.
 */
@Converter(autoApply = false)
public class BillStatusConverter implements AttributeConverter<BillStatus, String> {

    @Override
    public String convertToDatabaseColumn(BillStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public BillStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BillStatus.fromWire(dbData);
    }
}
