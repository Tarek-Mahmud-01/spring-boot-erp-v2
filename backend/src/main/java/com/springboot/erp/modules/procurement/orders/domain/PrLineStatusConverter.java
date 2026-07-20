package com.springboot.erp.modules.procurement.orders.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Persists {@link PrLineStatus} as its human wire label — matches {@code ck_pr_lines_status}. */
@Converter(autoApply = false)
public class PrLineStatusConverter implements AttributeConverter<PrLineStatus, String> {

    @Override
    public String convertToDatabaseColumn(PrLineStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public PrLineStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PrLineStatus.fromWire(dbData);
    }
}
