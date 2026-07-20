package com.springboot.erp.modules.inventory.counts.service;

import com.springboot.erp.modules.inventory.counts.domain.CycleCountScope;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Resolves a cycle-count scope selector to its product list and per-product
 * expected on-hand quantities. The reference resolves ALL / CATEGORY against the
 * product catalog and reads expected on-hand from the stock ledger — both
 * cross-slice. To keep this slice free of a compile dependency on the catalog /
 * ledger, the caller passes the already-resolved ids and expected map inside
 * {@code scope_config} ({@code product_ids} + optional {@code expected}); this
 * resolver just reads them out.
 */
final class CycleCountScopeResolver {

    private CycleCountScopeResolver() {
    }

    /**
     * The product ULIDs to count. CATEGORY / MANUAL / ALL / ABC all read the
     * caller-resolved {@code scope_config.product_ids} list; an absent list
     * yields an empty result (the service then rejects the empty count).
     */
    static List<String> productIds(CycleCountScope scope, Map<String, Object> scopeConfig) {
        if (scopeConfig == null) {
            return List.of();
        }
        Object ids = scopeConfig.get("product_ids");
        if (ids instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    /** Expected on-hand for a product from {@code scope_config.expected}, else zero. */
    @SuppressWarnings("unchecked")
    static BigDecimal expectedFor(Map<String, Object> scopeConfig, String productId) {
        if (scopeConfig != null && scopeConfig.get("expected") instanceof Map<?, ?> m) {
            Object v = ((Map<String, Object>) m).get(productId);
            if (v != null) {
                return new BigDecimal(String.valueOf(v));
            }
        }
        return BigDecimal.ZERO;
    }
}
