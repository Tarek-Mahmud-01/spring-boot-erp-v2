package com.guru.erp.modules.product.promotions.domain;

/**
 * ENT-014.type — FR-065. The discount rule family a promotion carries. The wire
 * value is the uppercase constant name (matches the reference StrEnum, whose
 * value equals its name), so this persists via {@link jakarta.persistence.EnumType#STRING}
 * and satisfies {@code ck_promotions_type}.
 */
public enum PromotionType {
    PERCENT,
    FIXED,
    BUY_X_GET_Y,
    SPEND_THRESHOLD
}
