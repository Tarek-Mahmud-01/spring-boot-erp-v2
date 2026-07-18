package com.guru.erp.modules.inventory.movements.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link TransferStatus} as its human wire label ({@code "Draft"} …) rather than the enum
 * constant name, matching the reference model and the {@code ck_stock_transfers_status} check
 * constraint. {@code @Enumerated(STRING)} would store {@code DRAFT} and violate the check.
 */
@Converter(autoApply = false)
public class TransferStatusConverter implements AttributeConverter<TransferStatus, String> {

    @Override
    public String convertToDatabaseColumn(TransferStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public TransferStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TransferStatus.fromWire(dbData);
    }
}
