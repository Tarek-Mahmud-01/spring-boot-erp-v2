package com.guru.erp.modules.procurement.landed.domain;

/**
 * How a landed cost spreads across the target PO / GRN lines (reference
 * {@code app.procurement.constants.AllocationBasis}).
 *
 * <ul>
 *   <li>{@code VALUE} — proportional to each line's net line value (qty × net unit price).</li>
 *   <li>{@code QUANTITY} — proportional to each line's qty.</li>
 *   <li>{@code WEIGHT} — proportional to product weight × qty (product weight is a cross-slice
 *       lookup, deferred here → contributes 0 until supplied).</li>
 *   <li>{@code VOLUME} — rejected: no volume column exists, so it would silently degrade to
 *       quantity weighting (reference {@code _reject_unsupported_basis}).</li>
 *   <li>{@code EQUAL} — split evenly across every line.</li>
 * </ul>
 *
 * <p>Persisted UPPERCASE via {@code @Enumerated(STRING)} (no converter needed).
 */
public enum AllocationBasis {
    VALUE,
    QUANTITY,
    WEIGHT,
    VOLUME,
    EQUAL
}
