package com.guru.erp.modules.product.promotions.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BA-c6 / FR-066 — advisory-only scope-overlap heuristic ported from the
 * reference {@code _scopes_overlap}. A false positive just surfaces an extra
 * non-blocking warning, so this stays deliberately conservative.
 */
final class PromotionOverlap {

    private static final List<String> DIMENSIONS =
        List.of("product_ids", "category_ids", "location_ids", "variant_ids");

    private PromotionOverlap() {
    }

    static boolean scopesOverlap(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> sa = a == null ? Map.of() : a;
        Map<String, Object> sb = b == null ? Map.of() : b;

        Set<String> aProducts = idSet(sa, "product_ids");
        Set<String> bProducts = idSet(sb, "product_ids");
        Set<String> aVariants = idSet(sa, "variant_ids");
        Set<String> bVariants = idSet(sb, "variant_ids");

        // Same product but disjoint variant sets can never co-fire — not an overlap.
        if (!aProducts.isEmpty() && !bProducts.isEmpty() && intersects(aProducts, bProducts)
            && !aVariants.isEmpty() && !bVariants.isEmpty()) {
            return intersects(aVariants, bVariants);
        }
        for (String key : DIMENSIONS) {
            Set<String> ka = idSet(sa, key);
            Set<String> kb = idSet(sb, key);
            if (!ka.isEmpty() && !kb.isEmpty() && intersects(ka, kb)) {
                return true;
            }
        }
        // Two basket-wide promotions (unrestricted on every dimension) overlap.
        return unrestricted(sa) && unrestricted(sb);
    }

    private static boolean unrestricted(Map<String, Object> scope) {
        for (String key : DIMENSIONS) {
            if (!idSet(scope, key).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> idSet(Map<String, Object> scope, String key) {
        Object v = scope.get(key);
        Set<String> out = new HashSet<>();
        if (v instanceof Collection<?> col) {
            for (Object o : col) {
                if (o != null) {
                    out.add(String.valueOf(o));
                }
            }
        }
        return out;
    }

    private static boolean intersects(Set<String> a, Set<String> b) {
        for (String s : a) {
            if (b.contains(s)) {
                return true;
            }
        }
        return false;
    }

    static List<String> warnings(List<String> overlappingNames) {
        if (overlappingNames == null || overlappingNames.isEmpty()) {
            return List.of();
        }
        List<String> sorted = new ArrayList<>(overlappingNames);
        sorted.sort(String::compareTo);
        String joined = String.join(", ", sorted.subList(0, Math.min(5, sorted.size())));
        return List.of("Overlaps with active promotion(s) on the same scope: " + joined + ".");
    }
}
