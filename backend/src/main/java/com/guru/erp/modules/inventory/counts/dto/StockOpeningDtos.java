package com.guru.erp.modules.inventory.counts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wire DTOs for ENT-045 StockOpening (records — ARCHITECTURE.md §2).
 * Bean-validation mirrors the reference Pydantic constraints; the
 * one-live-opening / immutability / post rules live in the service.
 * {@code openingQty} is {@link BigDecimal} (NUMERIC(18,6)); {@code unitCostAmount}
 * is minor units (long, money rule).
 */
public final class StockOpeningDtos {

    private StockOpeningDtos() {
    }

    /** POST /api/inventory/stock-opening body. */
    public record StockOpeningCreateRequest(
        @NotBlank @Size(min = 26, max = 26) String companyId,
        @NotBlank @Size(min = 26, max = 26) String productId,
        @Size(min = 26, max = 26) String variantId,
        @NotBlank @Size(min = 26, max = 26) String locationId,
        @NotNull @Positive BigDecimal openingQty,
        @PositiveOrZero long unitCostAmount,
        @NotBlank @Size(min = 3, max = 3) String unitCostCurrency,
        @NotBlank @Size(min = 26, max = 26) String glAccountId,
        @Size(max = 500) String notes
    ) {
    }

    /**
     * PATCH /api/inventory/stock-opening/{id} — only DRAFT rows are editable.
     * Every field optional; {@code version} carries the optimistic-lock token.
     */
    public record StockOpeningUpdateRequest(
        @Positive BigDecimal openingQty,
        @PositiveOrZero Long unitCostAmount,
        @Size(min = 3, max = 3) String unitCostCurrency,
        @Size(min = 26, max = 26) String glAccountId,
        @Size(max = 500) String notes,
        Long version
    ) {
    }

    /** Read shape returned by every stock-opening endpoint. {@code id} is the ULID. */
    public record StockOpeningResponse(
        String id,
        String companyId,
        String productId,
        String variantId,
        String locationId,
        BigDecimal openingQty,
        long unitCostAmount,
        String unitCostCurrency,
        String glAccountId,
        String notes,
        String status,
        long openingTotalValue,
        Instant postedAt,
        String postedBy,
        String journalEntryId,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
