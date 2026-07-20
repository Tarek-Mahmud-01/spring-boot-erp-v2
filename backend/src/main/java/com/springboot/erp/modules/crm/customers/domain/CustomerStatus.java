package com.springboot.erp.modules.crm.customers.domain;

/**
 * ENT-050.status (reference {@code CustomerStatus}) + the {@code ANONYMIZED}
 * end-state (FR-203). INACTIVE / ON_HOLD are kept for lifecycle parity even
 * though the current slice only actively drives ACTIVE / ANONYMIZED.
 */
public enum CustomerStatus {
    ACTIVE,
    INACTIVE,
    ON_HOLD,
    ANONYMIZED
}
