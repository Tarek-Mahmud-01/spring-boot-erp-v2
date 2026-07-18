package com.guru.erp.modules.inventory.stock.repository;

import java.math.BigDecimal;

/**
 * Projection of one availability bucket — {@code Σ qty_signed} grouped by
 * (product, variant, location) filtered to a single stock status (reference
 * {@code stock_availability} bulk query). Backs the pre-submit stock check used
 * by every "add line" dialog.
 */
public interface StockAvailabilityRow {
    String getProductId();

    String getVariantId();

    String getLocationId();

    BigDecimal getOnHand();
}
