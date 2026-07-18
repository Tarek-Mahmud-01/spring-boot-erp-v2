package com.guru.erp.modules.product.promotions.service;

import com.guru.erp.modules.product.promotions.domain.Promotion;
import java.util.Map;

/**
 * Builds the before/after audit image for a {@link Promotion} (full snapshot,
 * same shape as the reference {@code _promotion_audit_image}). Kept out of
 * {@link PromotionService} to hold the service under its line cap.
 */
final class PromotionSnapshot {

    private PromotionSnapshot() {
    }

    static Map<String, Object> of(Promotion p) {
        return Map.ofEntries(
            Map.entry("id", p.getPublicId()),
            Map.entry("companyId", p.getCompanyId()),
            Map.entry("name", p.getName()),
            Map.entry("type", p.getType().name()),
            Map.entry("config", p.getConfig() == null ? Map.of() : p.getConfig()),
            Map.entry("scope", p.getScope() == null ? Map.of() : p.getScope()),
            Map.entry("dateFrom", p.getDateFrom().toString()),
            Map.entry("dateTo", p.getDateTo().toString()),
            Map.entry("stackable", p.isStackable()),
            Map.entry("reasonRequired", p.isReasonRequired()),
            Map.entry("status", p.getStatus().name()),
            Map.entry("version", p.getVersion()));
    }
}
