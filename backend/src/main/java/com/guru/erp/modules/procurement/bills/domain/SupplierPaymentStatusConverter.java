package com.guru.erp.modules.procurement.bills.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link SupplierPaymentStatus} as its human wire label to match the reference model and
 * the {@code ck_supplier_payments_status} check constraint.
 */
@Converter(autoApply = false)
public class SupplierPaymentStatusConverter
    implements AttributeConverter<SupplierPaymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(SupplierPaymentStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public SupplierPaymentStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SupplierPaymentStatus.fromWire(dbData);
    }
}
