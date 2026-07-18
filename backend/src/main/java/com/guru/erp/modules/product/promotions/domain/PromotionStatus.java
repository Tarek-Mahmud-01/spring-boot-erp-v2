package com.guru.erp.modules.product.promotions.domain;

/**
 * ENT-014.status — lifecycle of a promotion record (distinct from the product
 * lifecycle). Wire value equals the constant name (reference StrEnum), so it
 * persists via {@link jakarta.persistence.EnumType#STRING} and satisfies
 * {@code ck_promotions_status}.
 */
public enum PromotionStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    EXPIRED
}
