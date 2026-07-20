package com.springboot.erp.modules.procurement.bills.dto;

import jakarta.validation.Valid;
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
 * Wire DTOs for ENT-033 SupplierPayment (records). Cross-slice ids are ULID {@code char(26)}
 * strings; money is bigint minor units. A split payment carries {@code tenders} whose amounts must
 * sum to {@code amountAmount} (validated in the service).
 */
public final class PaymentDtos {

    private PaymentDtos() {
    }

    /** One METHOD within a split payment. */
    public record TenderRequest(
        @NotNull @Size(min = 26, max = 26) String paymentMethodId,
        @NotNull @Size(min = 1, max = 100) String paymentMethodName,
        @Positive long amountAmount,
        @Size(min = 3, max = 3) String amountCurrency,
        @Size(max = 100) String referenceNo
    ) {
    }

    public record TenderResponse(
        String id,
        String paymentMethodId,
        String paymentMethodName,
        long amountAmount,
        String amountCurrency,
        String referenceNo
    ) {
    }

    /** POST /api/procurement/supplier-payments body. */
    public record PaymentCreateRequest(
        @NotNull @Size(min = 26, max = 26) String supplierId,
        @Size(min = 26, max = 26) String poId,
        @Size(max = 100) String invoiceReference,
        @NotNull @Size(min = 26, max = 26) String paymentMethodId,
        @NotNull @Size(min = 1, max = 100) String paymentMethodName,
        @Valid List<TenderRequest> tenders,
        @NotNull Instant paymentDate,
        @Positive long amountAmount,
        @NotNull @Size(min = 3, max = 3) String amountCurrency,
        @Size(max = 100) String referenceNo,
        @Size(max = 2000) String notes,
        @Pattern(regexp = "percent|amount") String discountType,
        @PositiveOrZero long discountValue
    ) {
    }

    /** PATCH /api/procurement/supplier-payments/{id} body — Draft only; partial semantics. */
    public record PaymentUpdateRequest(
        @Size(min = 26, max = 26) String poId,
        @Size(max = 100) String invoiceReference,
        @Size(min = 26, max = 26) String paymentMethodId,
        @Size(min = 1, max = 100) String paymentMethodName,
        Instant paymentDate,
        @Positive Long amountAmount,
        @Size(min = 3, max = 3) String amountCurrency,
        @Size(max = 100) String referenceNo,
        @Size(max = 2000) String notes,
        @Pattern(regexp = "percent|amount") String discountType,
        @PositiveOrZero Long discountValue,
        Long version
    ) {
    }

    /** POST /api/procurement/supplier-payments/{id}/transition body. */
    public record PaymentTransitionRequest(
        @NotNull String toStatus
    ) {
    }

    /** Payment read shape. {@code id} is the ULID public id. */
    public record PaymentResponse(
        String id,
        String number,
        String supplierId,
        String poId,
        String invoiceReference,
        String paymentMethodId,
        String paymentMethodName,
        Instant paymentDate,
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
        List<TenderResponse> tenders,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
