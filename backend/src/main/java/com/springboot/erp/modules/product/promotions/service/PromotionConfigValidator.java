package com.springboot.erp.modules.product.promotions.service;

import com.springboot.erp.modules.product.promotions.domain.PromotionType;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import java.util.List;
import java.util.Map;

/**
 * FR-065 — validates the type-specific {@code config} shape of a promotion.
 * Ported from the reference {@code _validate_promotion_config}. A bad config
 * throws {@link ErrorCode#VALIDATION_FAILED} carrying the offending field.
 */
final class PromotionConfigValidator {

    private PromotionConfigValidator() {
    }

    static void validate(PromotionType type, Map<String, Object> config) {
        Map<String, Object> c = config == null ? Map.of() : config;
        switch (type) {
            case PERCENT -> requirePercent(c.get("percent"), "percent");
            case FIXED -> {
                requirePositiveLong(c.get("amount"), "amount/currency");
                requireCurrency(c.get("currency"), "amount/currency");
            }
            case BUY_X_GET_Y -> validateBuyXGetY(c);
            case SPEND_THRESHOLD -> {
                requirePositiveLong(c.get("threshold"), "threshold/currency");
                requireCurrency(c.get("currency"), "threshold/currency");
                if (!c.containsKey("percent") && !c.containsKey("amount")) {
                    throw invalid("discount");
                }
            }
            default -> throw invalid("type");
        }
    }

    private static void validateBuyXGetY(Map<String, Object> c) {
        // buy (X) and get (Y) must both be integers >= 1 (FR-065).
        requireIntAtLeast(c.get("buy"), 1, "buy/get");
        requireIntAtLeast(c.get("get"), 1, "buy/get");
        String rewardKind = c.get("reward_kind") == null ? "FREE" : String.valueOf(c.get("reward_kind"));
        if (!rewardKind.equals("FREE") && !rewardKind.equals("PERCENT")) {
            throw invalid("reward_kind");
        }
        if (rewardKind.equals("PERCENT")) {
            requirePercent(c.get("percent"), "percent");
        }
        requireUlidList(c.get("get_product_ids"), "get_product_ids");
        requireUlidList(c.get("get_variant_ids"), "get_variant_ids");
    }

    private static void requirePercent(Object v, String field) {
        if (!(v instanceof Number n) || n.doubleValue() <= 0 || n.doubleValue() > 100) {
            throw invalid(field);
        }
    }

    private static void requirePositiveLong(Object v, String field) {
        if (!(v instanceof Number n) || !isIntegral(n) || n.longValue() <= 0) {
            throw invalid(field);
        }
    }

    private static void requireIntAtLeast(Object v, long min, String field) {
        if (!(v instanceof Number n) || !isIntegral(n) || n.longValue() < min) {
            throw invalid(field);
        }
    }

    private static void requireCurrency(Object v, String field) {
        if (!(v instanceof String s) || s.length() != 3) {
            throw invalid(field);
        }
    }

    private static void requireUlidList(Object v, String field) {
        if (v == null) {
            return;
        }
        if (!(v instanceof List<?> list)) {
            throw invalid(field);
        }
        for (Object g : list) {
            if (!(g instanceof String s) || s.length() != 26) {
                throw invalid(field);
            }
        }
    }

    private static boolean isIntegral(Number n) {
        return n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte
            || (n.doubleValue() == Math.floor(n.doubleValue()) && !Double.isInfinite(n.doubleValue()));
    }

    private static DomainException invalid(String field) {
        return new DomainException(ErrorCode.VALIDATION_FAILED,
            "Invalid promotion config", Map.of("field", field));
    }
}
