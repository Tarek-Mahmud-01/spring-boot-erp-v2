package com.guru.erp.modules.settings.company.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link CompanyStatus} as its lower-case wire value ({@code active} /
 * {@code inactive}) rather than the enum constant name. This matches the DB
 * {@code ck_companies_status check (status in ('active','inactive'))} constraint
 * and the reference model (which stores {@code CompanyStatus.value}). Using
 * {@code @Enumerated(EnumType.STRING)} here would persist {@code ACTIVE} /
 * {@code INACTIVE} and violate the check constraint on every write.
 */
@Converter
public class CompanyStatusConverter implements AttributeConverter<CompanyStatus, String> {

    @Override
    public String convertToDatabaseColumn(CompanyStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public CompanyStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CompanyStatus.fromWire(dbData);
    }
}
