package com.guru.erp.modules.inventory.movements.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Wire DTOs for ENT-043 StockAdjustment (records — ARCHITECTURE.md §2). Mirrors the reference
 * Pydantic constraints: min 1 line, non-zero qty delta (checked in the service), reason required,
 * non-negative unit cost. Cross-slice ids are ULID {@code char(26)} strings.
 */
public final class AdjustmentDtos {

    private AdjustmentDtos() {
    }

    /**
     * One line of a create/update payload. {@code qtyDelta} is the signed adjustment (the
     * variance): -10 removes 10, +5 adds 5. Zero is rejected in the service. {@code unitCostAmount}
     * 0 lets the posting step fall back to moving-average cost.
     */
    public record AdjustmentLineRequest(
        @NotNull @Size(min = 26, max = 26) String productId,
        @NotNull BigDecimal qtyDelta,
        @Size(min = 26, max = 26) String uomId,
        @Size(min = 26, max = 26) String variantId,
        @Size(max = 200) String writeOffReason,
        @PositiveOrZero long unitCostAmount,
        @Size(max = 3) String unitCostCurrency
    ) {
    }

    /** POST /api/inventory/adjustments body. */
    public record AdjustmentCreateRequest(
        @NotNull @Size(min = 26, max = 26) String locationId,
        @NotBlank @Size(max = 200) String reason,
        @Size(max = 2000) String notes,
        @Size(min = 26, max = 26) String varianceAccountId,
        @NotEmpty @Valid List<AdjustmentLineRequest> lines,
        // One-step create: skip Draft → Approved → Posted and post in a single call. Over-threshold
        // adjustments still stop in PENDING_APPROVAL even when this is true (FR-131).
        boolean autoComplete
    ) {
    }

    /** PATCH /api/inventory/adjustments/{id} body — full replacement (DRAFT only). */
    public record AdjustmentUpdateRequest(
        @NotNull @Size(min = 26, max = 26) String locationId,
        @NotBlank @Size(max = 200) String reason,
        @Size(max = 2000) String notes,
        @Size(min = 26, max = 26) String varianceAccountId,
        @NotEmpty @Valid List<AdjustmentLineRequest> lines,
        Long version
    ) {
    }

    /** Adjustment line read shape. */
    public record AdjustmentLineResponse(
        String id,
        int lineNo,
        String productId,
        String uomId,
        String variantId,
        String variantName,
        BigDecimal qtyCounted,
        BigDecimal qtyOnHandAtCount,
        BigDecimal qtyVariance,
        String writeOffReason,
        long unitCostAmount,
        String unitCostCurrency
    ) {
    }

    /** Adjustment read shape. {@code id} is the ULID public id. */
    public record AdjustmentResponse(
        String id,
        String number,
        String locationId,
        String reason,
        String notes,
        String status,
        boolean thresholdExceeded,
        String approverId,
        Instant approvedAt,
        Instant postedAt,
        String varianceAccountId,
        String journalEntryId,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<AdjustmentLineResponse> lines
    ) {
    }
}
