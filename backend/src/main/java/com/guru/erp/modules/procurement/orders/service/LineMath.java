package com.guru.erp.modules.procurement.orders.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared money math for PR/PO lines — reference {@code _calc_line_total}. Line total (minor units) =
 * qty × unit_price × (1 − discount%/100), rounded HALF_EVEN. Quantities go through
 * {@link BigDecimal} (never float) so 12.675 kg does not drift.
 */
final class LineMath {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private LineMath() {
    }

    /** Line total in minor units, net of the per-line discount. */
    static long lineTotal(BigDecimal qty, long unitPriceMinor, BigDecimal discountPercent) {
        BigDecimal gross = qty.multiply(BigDecimal.valueOf(unitPriceMinor));
        BigDecimal disc = gross.multiply(discountPercent).divide(HUNDRED, 10, RoundingMode.HALF_EVEN);
        return gross.subtract(disc).setScale(0, RoundingMode.HALF_EVEN).longValueExact();
    }
}
