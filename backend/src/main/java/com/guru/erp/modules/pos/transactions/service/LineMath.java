package com.guru.erp.modules.pos.transactions.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Line-amount math for {@code PosTransactionLine} (reference {@code _compute_line_amounts} /
 * {@code app.pos.views}). Returns {@code (net, tax, total)} minor units, tax rounded HALF_EVEN at
 * the line level (never a float). {@code qty} may be fractional (weighed goods); the gross is
 * {@code unitPriceAmount * qty} rounded HALF_EVEN back to whole minor units.
 */
final class LineMath {

    private LineMath() {
    }

    record LineAmounts(long net, long tax, long total) {
    }

    static LineAmounts compute(long unitPriceAmount, BigDecimal qty, long discountAmount,
                               BigDecimal taxRatePercent, boolean taxInclusive) {
        long gross = BigDecimal.valueOf(unitPriceAmount)
            .multiply(qty)
            .setScale(0, RoundingMode.HALF_EVEN)
            .longValueExact();
        return computeFromGross(gross, discountAmount, taxRatePercent, taxInclusive);
    }

    /** Variant that takes an authoritative gross (e.g. from a promo engine) instead of re-deriving it. */
    static LineAmounts computeFromGross(long gross, long discountAmount, BigDecimal taxRatePercent,
                                        boolean taxInclusive) {
        if (taxRatePercent != null && taxRatePercent.signum() > 0) {
            if (taxInclusive) {
                long grossLessDiscount = gross - discountAmount;
                long tax = taxFromInclusive(grossLessDiscount, taxRatePercent);
                long net = grossLessDiscount - tax;
                return new LineAmounts(net, tax, net + tax);
            }
            long net = gross - discountAmount;
            long tax = taxOnExclusive(net, taxRatePercent);
            return new LineAmounts(net, tax, net + tax);
        }
        long net = gross - discountAmount;
        return new LineAmounts(net, 0L, net);
    }

    private static long taxOnExclusive(long net, BigDecimal ratePercent) {
        return BigDecimal.valueOf(net)
            .multiply(ratePercent)
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_EVEN)
            .longValueExact();
    }

    private static long taxFromInclusive(long grossInclusive, BigDecimal ratePercent) {
        BigDecimal divisor = BigDecimal.valueOf(100).add(ratePercent);
        return BigDecimal.valueOf(grossInclusive)
            .multiply(ratePercent)
            .divide(divisor, 0, RoundingMode.HALF_EVEN)
            .longValueExact();
    }
}
