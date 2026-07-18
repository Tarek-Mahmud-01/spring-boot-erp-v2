package com.guru.erp.modules.procurement.landed.service;

import com.guru.erp.modules.procurement.landed.domain.AllocationBasis;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The money-math core of the landed-cost slice — the one place that turns a charge amount into
 * per-line slices (ports the reference {@code _alloc_weight} / {@code _finalize_weights} + the
 * last-line-remainder loop). Kept free of JPA / DB so the allocation rule is unit-testable and can
 * never diverge between the create and reallocate paths.
 *
 * <p>Weights per {@link AllocationBasis}:
 * <ul>
 *   <li>{@code VALUE} — qty × net unit price (line value).</li>
 *   <li>{@code QUANTITY} — qty.</li>
 *   <li>{@code WEIGHT} — qty × product unit weight (weight is a cross-slice lookup, deferred → 0).</li>
 *   <li>{@code EQUAL} — 1 per line.</li>
 *   <li>{@code VOLUME} — rejected upstream (never reaches here).</li>
 * </ul>
 *
 * <p>Zero-weight lines contribute 0 so they never steal cost from real dollar lines; only when the
 * ENTIRE basis sums to zero does it fall back to qty-weighting, then to equal. Rounding (HALF_EVEN)
 * lands on the LAST line so Σ slices == the input amount exactly (reference CALC-A / QA H4).
 */
public final class LandedCostAllocator {

    private LandedCostAllocator() {
    }

    /** One target line's inputs for the allocation. */
    public record Line(BigDecimal qty, long netUnitPriceMinor, BigDecimal unitWeight) {

        public static Line of(BigDecimal qty, long netUnitPriceMinor) {
            return new Line(qty == null ? BigDecimal.ZERO : qty, netUnitPriceMinor,
                BigDecimal.ZERO);
        }
    }

    /** Per-line weight before the batch-level zero-basis fallback. */
    static BigDecimal weight(AllocationBasis basis, Line line) {
        BigDecimal qty = line.qty() == null ? BigDecimal.ZERO : line.qty();
        return switch (basis) {
            case VALUE -> qty.multiply(BigDecimal.valueOf(line.netUnitPriceMinor()));
            case WEIGHT -> qty.multiply(line.unitWeight() == null ? BigDecimal.ZERO : line.unitWeight());
            case EQUAL -> BigDecimal.ONE;
            case QUANTITY -> qty;
            case VOLUME -> throw new IllegalArgumentException(
                "VOLUME allocation basis is not supported");
        };
    }

    /** Batch-level zero-basis fallback: qty-weight if any qty, else equal. */
    static List<BigDecimal> finalizeWeights(List<BigDecimal> weights, List<BigDecimal> qtys) {
        if (sum(weights).signum() > 0) {
            return weights;
        }
        if (sum(qtys).signum() > 0) {
            return new ArrayList<>(qtys);
        }
        List<BigDecimal> equal = new ArrayList<>(weights.size());
        for (int i = 0; i < weights.size(); i++) {
            equal.add(BigDecimal.ONE);
        }
        return equal;
    }

    /**
     * Split {@code amountMinor} across {@code lines} under {@code basis}. Returns one slice (minor
     * units) per input line, in order; the last non-empty slice carries the rounding remainder so the
     * slices sum to {@code amountMinor} exactly. An empty input returns an empty result.
     */
    public static List<Long> allocate(AllocationBasis basis, long amountMinor, List<Line> lines) {
        List<Long> out = new ArrayList<>(lines.size());
        if (lines.isEmpty()) {
            return out;
        }
        List<BigDecimal> weights = new ArrayList<>(lines.size());
        List<BigDecimal> qtys = new ArrayList<>(lines.size());
        for (Line line : lines) {
            weights.add(weight(basis, line));
            qtys.add(line.qty() == null ? BigDecimal.ZERO : line.qty());
        }
        weights = finalizeWeights(weights, qtys);
        BigDecimal totalWeight = sum(weights);
        if (totalWeight.signum() == 0) {
            totalWeight = BigDecimal.ONE;
        }

        long remaining = amountMinor;
        BigDecimal amount = BigDecimal.valueOf(amountMinor);
        for (int i = 0; i < lines.size(); i++) {
            boolean last = i == lines.size() - 1;
            long slice;
            if (last) {
                slice = remaining;
            } else {
                slice = amount.multiply(weights.get(i))
                    .divide(totalWeight, 0, RoundingMode.HALF_EVEN)
                    .longValueExact();
            }
            remaining -= slice;
            out.add(slice);
        }
        return out;
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            total = total.add(v == null ? BigDecimal.ZERO : v);
        }
        return total;
    }
}
