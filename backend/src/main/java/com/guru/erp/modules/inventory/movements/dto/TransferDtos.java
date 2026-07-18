package com.guru.erp.modules.inventory.movements.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Wire DTOs for ENT-042 StockTransfer (records — ARCHITECTURE.md §2). Bean-validation mirrors the
 * reference Pydantic constraints (min 1 line, qty_sent &gt; 0, non-negative receipts). Cross-slice
 * ids are ULID {@code char(26)} strings; {@code id} in responses is always the public id.
 */
public final class TransferDtos {

    private TransferDtos() {
    }

    /** One line of a create/update payload. */
    public record TransferLineRequest(
        @NotNull @Size(min = 26, max = 26) String productId,
        @NotNull @Positive BigDecimal qtySent,
        @Size(min = 26, max = 26) String uomId,
        @Size(min = 26, max = 26) String variantId
    ) {
    }

    /** POST /api/inventory/transfers body. */
    public record TransferCreateRequest(
        @NotNull @Size(min = 26, max = 26) String sourceLocationId,
        @NotNull @Size(min = 26, max = 26) String destinationLocationId,
        @Size(max = 2000) String notes,
        LocalDate transferDate,
        @NotEmpty @Valid List<TransferLineRequest> lines,
        // Skip the Draft → Approved → Complete workflow, landing at Complete in one call.
        boolean autoComplete
    ) {
    }

    /** PATCH /api/inventory/transfers/{id} body — full replacement of header + lines (DRAFT only). */
    public record TransferUpdateRequest(
        @NotNull @Size(min = 26, max = 26) String sourceLocationId,
        @NotNull @Size(min = 26, max = 26) String destinationLocationId,
        @Size(max = 2000) String notes,
        LocalDate transferDate,
        @NotEmpty @Valid List<TransferLineRequest> lines,
        Long version
    ) {
    }

    /** One line of the POST /{id}/receive payload — quantities received/short/damaged per line. */
    public record TransferReceiveLineRequest(
        @NotNull @Size(min = 26, max = 26) String lineId,
        @NotNull @PositiveOrZero BigDecimal qtyReceived,
        @PositiveOrZero BigDecimal qtyShort,
        @PositiveOrZero BigDecimal qtyDamaged,
        @Size(max = 500) String discrepancyReason
    ) {
    }

    /** POST /api/inventory/transfers/{id}/receive body. */
    public record TransferReceiveRequest(
        @NotEmpty @Valid List<TransferReceiveLineRequest> lines
    ) {
    }

    /** Transfer line read shape. */
    public record TransferLineResponse(
        String id,
        int lineNo,
        String productId,
        String uomId,
        String variantId,
        BigDecimal qtySent,
        BigDecimal qtyReceived,
        BigDecimal qtyShort,
        BigDecimal qtyDamaged,
        String discrepancyReason
    ) {
    }

    /** Transfer read shape. {@code id} is the ULID public id. */
    public record TransferResponse(
        String id,
        String number,
        String sourceLocationId,
        String destinationLocationId,
        String status,
        String notes,
        LocalDate transferDate,
        Instant confirmedAt,
        Instant receivedAt,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<TransferLineResponse> lines
    ) {
    }
}
