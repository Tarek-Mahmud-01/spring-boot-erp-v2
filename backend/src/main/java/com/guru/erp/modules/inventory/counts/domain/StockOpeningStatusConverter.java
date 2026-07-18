package com.guru.erp.modules.inventory.counts.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link StockOpeningStatus} as its wire value ("Draft" / "Posted"),
 * matching the {@code ck_stock_opening_status} check constraint and the
 * reference model (which stores {@code StockOpeningStatus.value}).
 */
@Converter
public class StockOpeningStatusConverter implements AttributeConverter<StockOpeningStatus, String> {

    @Override
    public String convertToDatabaseColumn(StockOpeningStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public StockOpeningStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : StockOpeningStatus.fromWire(dbData);
    }
}
