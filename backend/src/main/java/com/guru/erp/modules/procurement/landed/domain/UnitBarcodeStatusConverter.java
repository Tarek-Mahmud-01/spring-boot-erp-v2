package com.guru.erp.modules.procurement.landed.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link UnitBarcodeStatus} as its lowercase wire value ({@code "available"} …) rather
 * than the enum constant name, matching the reference model and the {@code ck_unit_barcodes_status}
 * check constraint.
 */
@Converter(autoApply = false)
public class UnitBarcodeStatusConverter implements AttributeConverter<UnitBarcodeStatus, String> {

    @Override
    public String convertToDatabaseColumn(UnitBarcodeStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public UnitBarcodeStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : UnitBarcodeStatus.fromWire(dbData);
    }
}
