package com.guru.erp.modules.inventory.stock.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Read-only projection DTOs over the ledger (reference {@code stock_on_hand}).
 * On-hand is a pure aggregate — no create/update; the endpoints only read.
 */
public final class StockOnHandDtos {

    private StockOnHandDtos() {
    }

    /**
     * One on-hand bucket. {@code totalValue} is backend-owned (qty × unit cost,
     * minor units) so the frontend never multiplies client-side.
     */
    public record StockOnHandResponse(
        String productId,
        String variantId,
        String locationId,
        String status,
        BigDecimal qtyOnHand,
        long unitCostAmount,
        String unitCostCurrency,
        long totalValue,
        Instant updatedAt
    ) {
    }

    /** Total SOH for one (product, variant) across all locations (procurement context). */
    public record ProductSohSummary(
        String productId,
        String variantId,
        BigDecimal totalQtyOnHand
    ) {
    }

    /** Bulk SOH-summary request — resolve many products in one query (max 200). */
    public record BatchSohRequest(
        @NotEmpty @Size(max = 200) List<String> productIds
    ) {
    }
}
