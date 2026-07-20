package com.springboot.erp.modules.procurement.returns.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Wire DTOs for ENT-031 SupplierReturn (records — ARCHITECTURE.md §2). Mirrors the reference
 * Pydantic constraints: 26-char ULID ids, min 1 line, strictly positive qty, reason ≤ 500 chars,
 * required {@code returnedAt} (every document states its own date — no server-side {@code now()}
 * fallback). Cross-slice ids are ULID {@code char(26)} strings.
 */
public final class ReturnDtos {

    private ReturnDtos() {
    }

    /**
     * One returned line. {@code grnLineId} is the GoodsReceiptLine the units came in on;
     * {@code qty} is what goes back (capped at received-minus-already-returned in the service).
     */
    public record ReturnLineRequest(
        @Size(min = 26, max = 26) String grnLineId,
        @NotNull @Positive BigDecimal qty,
        @Size(max = 500) String reason,
        // Optional: line-level cost inputs so the debit note + stock reversal can be valued without
        // a hard join into the procurement/inventory tables. minor units; 0 lets posting fall back.
        @Size(min = 26, max = 26) String productId,
        @Size(min = 26, max = 26) String variantId,
        long unitPriceAmount,
        @Size(max = 3) String unitPriceCurrency,
        BigDecimal taxRatePercent
    ) {
    }

    /** POST /api/procurement/returns body. */
    public record ReturnCreateRequest(
        @NotNull @Size(min = 26, max = 26) String supplierId,
        @NotNull @Size(min = 26, max = 26) String grnId,
        @NotNull Instant returnedAt,
        @NotEmpty @Valid List<ReturnLineRequest> lines
    ) {
    }

    /**
     * PATCH /api/procurement/returns/{id} body — full replacement of the returned lines. Supplier +
     * GRN stay fixed. On save the reference reverses the old V-007 voucher + stock-out and re-posts;
     * here the equivalent reversal + re-post events are emitted.
     */
    public record ReturnUpdateRequest(
        @NotEmpty @Valid List<ReturnLineRequest> lines,
        Long version
    ) {
    }

    /** POST /api/procurement/returns/{id}/transition body. */
    public record ReturnTransitionRequest(
        @NotNull String toStatus
    ) {
    }

    /** Return line read shape. {@code id} is the ULID public id. */
    public record ReturnLineResponse(
        String id,
        String grnLineId,
        String variantId,
        BigDecimal qty,
        String reason
    ) {
    }

    /** Return read shape. {@code id} is the ULID public id. Money flattens to amount/currency pairs. */
    public record ReturnResponse(
        String id,
        String number,
        String supplierId,
        String grnId,
        Instant returnedAt,
        String status,
        long debitNoteAmount,
        String debitNoteCurrency,
        BigDecimal exchangeRate,
        long baseDebitNoteAmount,
        String baseDebitNoteCurrency,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<ReturnLineResponse> lines
    ) {
    }
}
