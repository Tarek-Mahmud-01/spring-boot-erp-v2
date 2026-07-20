package com.springboot.erp.modules.finance.coa.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link AccountStatus} as its lower-case wire value ({@code active} /
 * {@code inactive}) rather than the enum constant name, matching the reference
 * model and {@code ck_accounts_status check (status in ('active','inactive'))}.
 * {@code @Enumerated(EnumType.STRING)} would store {@code ACTIVE} and violate
 * the check on every write.
 */
@Converter
public class AccountStatusConverter implements AttributeConverter<AccountStatus, String> {

    @Override
    public String convertToDatabaseColumn(AccountStatus attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public AccountStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AccountStatus.fromWire(dbData);
    }
}
