package com.springboot.erp.modules.inventory.counts.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link CycleCountStatus} as its wire value ("Draft", "In Progress",
 * "Completed", "Approved") rather than the enum constant name, matching the
 * {@code ck_cycle_count_plans_status} check constraint and the reference model
 * (which stores {@code CycleCountStatus.value}).
 */
@Converter
public class CycleCountStatusConverter implements AttributeConverter<CycleCountStatus, String> {

    @Override
    public String convertToDatabaseColumn(CycleCountStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public CycleCountStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CycleCountStatus.fromWire(dbData);
    }
}
