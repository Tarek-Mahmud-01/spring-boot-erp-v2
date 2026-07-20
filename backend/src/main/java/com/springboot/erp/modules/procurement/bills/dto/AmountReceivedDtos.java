package com.springboot.erp.modules.procurement.bills.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Wire DTOs for ENT-034 AmountReceived (records) — refund / credit-note receipts. Cross-slice ids
 * are ULID {@code char(26)} strings; money is bigint minor units.
 */
public final class AmountReceivedDtos {

    private AmountReceivedDtos() {
    }

    /** POST /api/procurement/amount-received body. */
    public record AmountReceivedCreateRequest(
        @Size(max = 30) String number,
        @NotNull @Size(min = 26, max = 26) String supplierId,
        @Size(min = 26, max = 26) String purchaseReturnId,
        @Size(min = 26, max = 26) String poId,
        @Size(max = 100) String creditNoteReference,
        @NotNull @Size(min = 26, max = 26) String paymentMethodId,
        @NotNull @Size(min = 1, max = 100) String paymentMethodName,
        @NotNull Instant receivedDate,
        @Positive long amountAmount,
        @NotNull @Size(min = 3, max = 3) String amountCurrency,
        @Size(max = 100) String referenceNo,
        @Size(max = 2000) String notes,
        @Pattern(regexp = "percent|amount") String discountType,
        @PositiveOrZero long discountValue
    ) {
    }

    /** PATCH /api/procurement/amount-received/{id} body — partial; Voided is rejected. */
    public record AmountReceivedUpdateRequest(
        @Size(min = 26, max = 26) String supplierId,
        @Size(min = 26, max = 26) String purchaseReturnId,
        @Size(min = 26, max = 26) String poId,
        @Size(max = 100) String creditNoteReference,
        @Size(min = 26, max = 26) String paymentMethodId,
        @Size(min = 1, max = 100) String paymentMethodName,
        Instant receivedDate,
        @Positive Long amountAmount,
        @Size(min = 3, max = 3) String amountCurrency,
        @Size(max = 100) String referenceNo,
        @Size(max = 2000) String notes,
        @Pattern(regexp = "percent|amount") String discountType,
        @PositiveOrZero Long discountValue,
        Long version
    ) {
    }

    /** POST /api/procurement/amount-received/{id}/transition body. */
    public record AmountReceivedTransitionRequest(
        @NotNull String toStatus,
        @Size(max = 500) String reason
    ) {
    }

    /** AmountReceived read shape. {@code id} is the ULID public id. */
    public record AmountReceivedResponse(
        String id,
        String number,
        String supplierId,
        String purchaseReturnId,
        String poId,
        String creditNoteReference,
        String paymentMethodId,
        String paymentMethodName,
        Instant receivedDate,
        long amountAmount,
        String amountCurrency,
        BigDecimal exchangeRate,
        long baseAmount,
        String referenceNo,
        String notes,
        String discountType,
        long discountValue,
        String status,
        List<Map<String, Object>> statusHistory,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
