package com.springboot.erp.modules.procurement.suppliers.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link SupplierStatus} as its PascalCase wire label ({@code "Active"} …) rather than
 * the enum constant name, matching the reference model and the {@code ck_suppliers_status} check
 * constraint. Using {@code @Enumerated(STRING)} would persist {@code ACTIVE} and violate the check.
 */
@Converter(autoApply = false)
public class SupplierStatusConverter implements AttributeConverter<SupplierStatus, String> {

    @Override
    public String convertToDatabaseColumn(SupplierStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public SupplierStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SupplierStatus.fromWire(dbData);
    }
}
