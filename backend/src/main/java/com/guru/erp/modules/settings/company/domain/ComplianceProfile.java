package com.guru.erp.modules.settings.company.domain;

/**
 * ENT-001 compliance profile — extensible (KSA, EU, ... added later). Stored as
 * its own name string; {@code AU} triggers the ABN / GST cross-field rules.
 */
public enum ComplianceProfile {
    NONE,
    AU
}
