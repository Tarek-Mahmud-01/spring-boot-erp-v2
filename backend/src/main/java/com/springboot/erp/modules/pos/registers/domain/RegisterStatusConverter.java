package com.springboot.erp.modules.pos.registers.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link RegisterStatus} as its lower-case wire value ({@code active} /
 * {@code inactive}) rather than the enum constant name, matching the reference
 * model and the {@code ck_registers_status} check constraint.
 */
@Converter(autoApply = false)
public class RegisterStatusConverter implements AttributeConverter<RegisterStatus, String> {

    @Override
    public String convertToDatabaseColumn(RegisterStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public RegisterStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RegisterStatus.fromWire(dbData);
    }
}
