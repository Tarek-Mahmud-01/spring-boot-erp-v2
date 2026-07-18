package com.guru.erp.modules.inventory.stock.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link BatchBarcodeSource} as its lower-case wire value
 * ({@code manufacturer} / {@code generated}) rather than the enum constant name,
 * matching the reference model and the
 * {@code ck_product_batches_source check (barcode_source in ('manufacturer','generated'))}
 * constraint. {@code @Enumerated(EnumType.STRING)} would store {@code MANUFACTURER}
 * and violate the check on every write.
 */
@Converter
public class BatchBarcodeSourceConverter implements AttributeConverter<BatchBarcodeSource, String> {

    @Override
    public String convertToDatabaseColumn(BatchBarcodeSource attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public BatchBarcodeSource convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BatchBarcodeSource.fromWire(dbData);
    }
}
