package com.springboot.erp.modules.inventory.counts.domain;

/**
 * ENT-044 count scope selector (reference app.inventory.constants.CycleCountScope).
 * Persisted as the constant name (uppercase) matching the reference {@code StrEnum}
 * and the {@code ck_cycle_count_plans_scope} check constraint.
 *
 * <ul>
 *   <li>{@code ALL} — every active product at the location.</li>
 *   <li>{@code CATEGORY} — products in {@code scope_config.category_ids}.</li>
 *   <li>{@code ABC} — ABC-classification driven (reference reserves this value).</li>
 *   <li>{@code MANUAL} — an explicit {@code scope_config.product_ids} list.</li>
 * </ul>
 */
public enum CycleCountScope {
    ALL,
    CATEGORY,
    ABC,
    MANUAL
}
