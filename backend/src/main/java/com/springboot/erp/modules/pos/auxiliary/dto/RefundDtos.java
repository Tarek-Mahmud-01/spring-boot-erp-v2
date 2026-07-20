package com.springboot.erp.modules.pos.auxiliary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Wire DTOs for PosRefund (records — ARCHITECTURE.md §2). Mirrors the reference Pydantic
 * constraints: at least one line/item, positive return qty, non-negative manager-entered price.
 * Cross-slice ids (transaction, line, payment method, customer) are ULID {@code char(26)} strings.
 */
public final class RefundDtos {

    private RefundDtos() {
    }

    /** US-034 — one original line to return, by its public id + return qty. */
    public record RefundLineInput(
        @NotNull @Size(min = 26, max = 26) String lineId,
        @NotNull @DecimalMin(value = "0.000", inclusive = false) BigDecimal qty
    ) {
    }

    /** US-034 FR-177 — refund selected lines against the original receipt. */
    public record RefundReceiptLinkedRequest(
        @NotNull @Size(min = 26, max = 26) String originalTransactionId,
        @NotEmpty @Valid List<RefundLineInput> lines,
        @NotNull @Size(min = 26, max = 26) String refundMethodId,
        @Size(max = 255) String reason
    ) {
    }

    /**
     * US-034 FR-178 — one item to refund without a receipt. {@code unitPriceAmount} is optional:
     * omit it to price at the lowest unit price sold in the lookback window; when no such sale
     * exists a manager-entered price becomes mandatory (enforced in the service).
     */
    public record RefundItemInput(
        @NotNull @Size(min = 26, max = 26) String productId,
        @Size(min = 26, max = 26) String variantId,
        @NotNull @DecimalMin(value = "0.000", inclusive = false) BigDecimal qty,
        @PositiveOrZero Long unitPriceAmount
    ) {
    }

    /**
     * US-034 FR-178 — refund with no original receipt, manager-gated. {@code managerUsername} /
     * {@code managerPassword} carry the step-up manager authorization the online endpoint verifies
     * live; the offline-replay path omits them (the syncing operator's own permission gates it
     * instead).
     */
    public record RefundNoReceiptRequest(
        @NotEmpty @Valid List<RefundItemInput> items,
        @NotNull @Size(min = 26, max = 26) String refundMethodId,
        @NotBlank @Size(max = 255) String reason,
        @Size(max = 200) String managerUsername,
        @Size(max = 200) String managerPassword,
        @Size(min = 26, max = 26) String customerId
    ) {
    }

    /** US-034 — a completed REFUND transaction + its refund metadata. */
    public record RefundResponse(
        String id,
        String transactionId,
        String originalTransactionId,
        String mode,
        String pricedFrom,
        String reason,
        boolean managerApproved,
        String managerApprovalMethod,
        long totalRefundAmount,
        String currency,
        Instant createdAt
    ) {
    }
}
