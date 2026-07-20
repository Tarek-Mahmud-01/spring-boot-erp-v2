package com.springboot.erp.modules.procurement.landed.domain;

/**
 * Standard landed-cost charge categories (reference
 * {@code app.procurement.constants.LandedCostChargeType}). Persisted UPPERCASE via
 * {@code @Enumerated(STRING)} (no converter needed).
 */
public enum LandedCostChargeType {
    FREIGHT,
    DUTY,
    CUSTOMS_CLEARANCE,
    INSURANCE,
    HANDLING,
    BROKERAGE,
    DOCUMENTATION,
    DEMURRAGE,
    STORAGE,
    INSPECTION,
    OTHER
}
