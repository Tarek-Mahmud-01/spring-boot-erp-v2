package com.guru.erp.modules.procurement.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Wire DTOs for ENT-027 PurchaseRequisition (records — ARCHITECTURE.md §2). Mirrors the reference
 * Pydantic constraints: qty &gt; 0, non-negative money, 0–100 discount. Cross-slice ids are ULID
 * {@code char(26)} strings. Money is long minor units.
 */
public final class PrDtos {

    private PrDtos() {
    }

    /** One line of a create/update payload. */
    public record PrLineRequest(
        @Size(min = 26, max = 26) String productId,
        @Size(min = 26, max = 26) String variantId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal qty,
        @Size(min = 26, max = 26) String uomId,
        @Size(min = 26, max = 26) String preferredSupplierId,
        @Size(max = 500) String description,
        @PositiveOrZero long unitPriceAmount,
        @Size(max = 3) String unitPriceCurrency,
        @NotNull @DecimalMin("0") BigDecimal discountPercent,
        @Size(min = 26, max = 26) String taxCodeId,
        @PositiveOrZero long lineTotalAmount
    ) {
    }

    /** POST /api/procurement/purchase-requisitions body. */
    public record PrCreateRequest(
        @NotNull @Size(min = 26, max = 26) String locationId,
        @Size(min = 26, max = 26) String supplierId,
        @Size(min = 3, max = 3) String currency,
        Instant neededByDate,
        Instant requestDate,
        @Size(max = 100) String paymentTerms,
        BigDecimal exchangeRate,
        @Size(max = 10) String invoiceDiscountType,
        BigDecimal invoiceDiscountValue,
        @PositiveOrZero long totalAmount,
        String notes,
        @Valid List<PrLineRequest> lines
    ) {
    }

    /** PATCH /api/procurement/purchase-requisitions/{id} body — full replacement (DRAFT only). */
    public record PrUpdateRequest(
        @Size(min = 26, max = 26) String locationId,
        @Size(min = 26, max = 26) String supplierId,
        @Size(min = 3, max = 3) String currency,
        Instant neededByDate,
        Instant requestDate,
        @Size(max = 100) String paymentTerms,
        BigDecimal exchangeRate,
        @Size(max = 10) String invoiceDiscountType,
        BigDecimal invoiceDiscountValue,
        @PositiveOrZero long totalAmount,
        String notes,
        @Valid List<PrLineRequest> lines,
        Long version
    ) {
    }

    /** Body for the generic PR transition endpoint (reject / send-to-supplier / etc.). */
    public record PrTransitionRequest(
        @NotNull String toStatus,
        @Size(max = 500) String reason,
        @Size(min = 26, max = 26) String buyerId
    ) {
    }

    /** PR line read shape. */
    public record PrLineResponse(
        String id,
        int lineNo,
        String productId,
        String variantId,
        BigDecimal qty,
        String uomId,
        String preferredSupplierId,
        String description,
        long unitPriceAmount,
        String unitPriceCurrency,
        BigDecimal discountPercent,
        String taxCodeId,
        long lineTotalAmount,
        String status
    ) {
    }

    /** PR read shape. {@code id} is the ULID public id. */
    public record PrResponse(
        String id,
        String number,
        String locationId,
        String requesterUserId,
        String supplierId,
        String currency,
        Instant neededByDate,
        Instant requestDate,
        String paymentTerms,
        BigDecimal exchangeRate,
        String invoiceDiscountType,
        BigDecimal invoiceDiscountValue,
        long totalAmount,
        String notes,
        String status,
        String assignedBuyerId,
        String rejectionReason,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<PrLineResponse> lines
    ) {
    }
}
