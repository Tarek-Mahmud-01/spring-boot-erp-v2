package com.springboot.erp.modules.inventory.stock.dto;

import com.springboot.erp.modules.inventory.stock.domain.StockStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Bulk stock-availability check DTOs (reference {@code stock_availability}). One
 * query validates every (product, location, qty) triple before a transfer /
 * adjustment / sale is submitted. Ids are ULIDs.
 */
public final class StockAvailabilityDtos {

    private StockAvailabilityDtos() {
    }

    /** One product/location/qty triple to validate. */
    public record StockCheckItemRequest(
        @NotBlank @Size(min = 26, max = 26) String productId,
        @PositiveOrZero BigDecimal qtyRequired,
        @Size(min = 26, max = 26) String locationId,
        @Size(min = 26, max = 26) String variantId
    ) {
    }

    /** Bulk-check payload — at least one item, max 200; defaults to AVAILABLE bucket. */
    public record StockCheckRequest(
        @Size(min = 26, max = 26) String locationId,
        @NotEmpty @Size(max = 200) List<@jakarta.validation.Valid StockCheckItemRequest> items,
        StockStatus stockStatus
    ) {
    }

    public record StockCheckItemResponse(
        String productId,
        String variantId,
        String locationId,
        BigDecimal onHand,
        BigDecimal qtyRequired,
        boolean sufficient,
        BigDecimal shortfall
    ) {
    }

    public record StockCheckResponse(
        List<StockCheckItemResponse> items,
        boolean allSufficient
    ) {
    }
}
