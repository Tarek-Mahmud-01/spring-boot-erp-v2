package com.springboot.erp.modules.crm.loyalty.domain;

/**
 * ENT-071 LoyaltyLedger.type — every point movement is one of these (reference
 * {@code LoyaltyMovementType}, FR-209). Wire values already match the Java
 * constant names, so {@code @Enumerated(STRING)} persists verbatim.
 */
public enum LoyaltyMovementType {
    EARN,
    REDEEM,
    EXPIRE,
    REVERSE
}
