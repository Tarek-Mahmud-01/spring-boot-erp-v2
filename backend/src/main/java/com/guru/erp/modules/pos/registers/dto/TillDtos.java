package com.guru.erp.modules.pos.registers.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Wire DTOs for PosTillSession / PosTillMovement (records — ARCHITECTURE.md
 * §2). Mirrors the reference Pydantic schemas ({@code TillOpenRequest} /
 * {@code TillMovementRequest} / {@code TillCloseRequest} /
 * {@code TillSessionResponse} / {@code TillMovementResponse} /
 * {@code TillReportResponse}), US-037 FR-191..194.
 */
public final class TillDtos {

    private TillDtos() {
    }

    /** POST /api/pos/till/open body — opens a till with an opening float. */
    public record TillOpenRequest(
        @NotNull @Size(min = 26, max = 26) String registerId,
        @NotNull @PositiveOrZero Long openingFloat,
        String currency
    ) {
    }

    /** POST /api/pos/till/{id}/movements body — record a cash pickup / drop / payout. */
    public record TillMovementRequest(
        @NotBlank String type,
        @NotNull @Min(1) Long amount,
        @NotBlank @Size(min = 1, max = 255) String note
    ) {
    }

    /**
     * POST /api/pos/till/{id}/close body. {@code managerUsername}/{@code managerPassword} carry the
     * step-up manager authorization required when the counted-cash variance exceeds the threshold
     * (mirrors the reference no-receipt-refund step-up) — verified live against a real, active user
     * holding {@code pos.till.manage}; omitted on a plain close within the variance threshold.
     */
    public record TillCloseRequest(
        @NotNull @PositiveOrZero Long countedCash,
        @Size(max = 200) String managerUsername,
        @Size(max = 200) String managerPassword
    ) {
    }

    /** Cash-movement read shape. {@code id} is the ULID public id. */
    public record TillMovementResponse(
        String id,
        String type,
        long amount,
        String currency,
        String note,
        Instant createdAt,
        String createdBy
    ) {
    }

    /** Till session read shape. {@code id} is the ULID public id. */
    public record TillSessionResponse(
        String id,
        String registerId,
        String locationId,
        String cashierId,
        String status,
        long openingFloatAmount,
        Long expectedCashAmount,
        Long countedCashAmount,
        Long varianceAmount,
        boolean varianceApproved,
        String currency,
        Instant openedAt,
        Instant closedAt,
        long version,
        List<TillMovementResponse> movements
    ) {
    }

    /** US-037 FR-193 — X (mid-session) / Z (end-of-day) till report. */
    public record TillReportResponse(
        String tillId,
        String registerId,
        String locationId,
        String reportType,
        String currency,
        long openingFloatAmount,
        long grossSalesAmount,
        long refundsAmount,
        long netSalesAmount,
        long cashMovementsInAmount,
        long cashMovementsOutAmount,
        long expectedCashAmount,
        Long countedCashAmount,
        Long varianceAmount,
        long transactionCount,
        Instant openedAt,
        Instant generatedAt,
        List<TillMovementResponse> movements
    ) {
    }
}
