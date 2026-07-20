package com.springboot.erp.modules.procurement.bills.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link AmountReceivedStatus} as its human wire label to match the reference model and
 * the {@code ck_amount_received_status} check constraint.
 */
@Converter(autoApply = false)
public class AmountReceivedStatusConverter
    implements AttributeConverter<AmountReceivedStatus, String> {

    @Override
    public String convertToDatabaseColumn(AmountReceivedStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public AmountReceivedStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AmountReceivedStatus.fromWire(dbData);
    }
}
