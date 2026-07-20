package com.springboot.erp.modules.inventory.stock.repository;

import com.springboot.erp.modules.inventory.stock.domain.StockStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projection of one on-hand bucket — the {@code GROUP BY} result over the ledger
 * (reference {@code get_stock_on_hand}). One row per (product, variant, location,
 * status) with the summed on-hand quantity and the latest movement timestamp.
 */
public interface StockOnHandRow {
    String getProductId();

    String getVariantId();

    String getLocationId();

    StockStatus getStatus();

    BigDecimal getQtyOnHand();

    Instant getUpdatedAt();
}
