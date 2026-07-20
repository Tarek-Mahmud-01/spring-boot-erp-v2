package com.springboot.erp.modules.procurement.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Wire DTOs for ENT-028 PurchaseOrder (records — ARCHITECTURE.md §2). Mirrors the reference Pydantic
 * constraints: qty_ordered &gt; 0, non-negative unit price, 0–100 discount. Cross-slice ids are ULID
 * {@code char(26)} strings. Money is long minor units; quantities are {@link BigDecimal}(18,6).
 *
 * <p>The reference "Direct PO" one-shot chain (auto-create GRN + bill + payments) and email/PDF
 * rendering are deferred — see the service Javadoc. The {@code isDirect} flag is persisted so a
 * downstream slice can complete that chain via the outbox.
 */
public final class PoDtos {

    private PoDtos() {
    }

    /** One line of a create/update payload. */
    public record PoLineRequest(
        @Size(min = 26, max = 26) String productId,
        @Size(min = 26, max = 26) String variantId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal qtyOrdered,
        @Size(min = 26, max = 26) String uomId,
        @PositiveOrZero long unitPriceAmount,
        @Size(max = 3) String unitPriceCurrency,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal discountPercent,
        @Size(min = 26, max = 26) String taxCodeId
    ) {
    }

    /** POST /api/procurement/purchase-orders body. */
    public record PoCreateRequest(
        @NotNull @Size(min = 26, max = 26) String supplierId,
        @NotNull @Size(min = 26, max = 26) String locationId,
        @NotNull Instant poDate,
        Instant expectedDeliveryDate,
        @Size(max = 3) String currency,
        BigDecimal exchangeRate,
        @Size(max = 100) String paymentTerms,
        @Size(min = 26, max = 26) String sourcePrId,
        String notes,
        @Size(max = 10) String invoiceDiscountType,
        BigDecimal invoiceDiscountValue,
        boolean isDirect,
        @Valid List<PoLineRequest> lines
    ) {
    }

    /** PATCH /api/procurement/purchase-orders/{id} body — amends a DRAFT PO (full line replace). */
    public record PoUpdateRequest(
        Instant expectedDeliveryDate,
        @Size(max = 100) String paymentTerms,
        String notes,
        @Size(max = 10) String invoiceDiscountType,
        BigDecimal invoiceDiscountValue,
        @Size(max = 500) String amendmentReason,
        @Size(min = 26, max = 26) String supplierId,
        @Size(min = 26, max = 26) String locationId,
        @Size(min = 3, max = 3) String currency,
        BigDecimal exchangeRate,
        Instant poDate,
        @Valid List<PoLineRequest> lines,
        Long version
    ) {
    }

    /** Body for the generic PO transition endpoint (submit / approve / send / receive / close…). */
    public record PoTransitionRequest(
        @NotNull String toStatus,
        @Size(max = 500) String reason
    ) {
    }

    /** PO line read shape. */
    public record PoLineResponse(
        String id,
        int lineNo,
        String productId,
        String variantId,
        String variantName,
        BigDecimal qtyOrdered,
        BigDecimal qtyReceivedTotal,
        String uomId,
        long unitPriceAmount,
        String unitPriceCurrency,
        BigDecimal discountPercent,
        String taxCodeId,
        BigDecimal taxRatePercent,
        long lineTotalAmount,
        String lineTotalCurrency,
        String lineStatus
    ) {
    }

    /** PO version-history entry read shape (FR-092). */
    public record PoVersionResponse(
        String id,
        int versionNo,
        Map<String, Object> snapshot,
        String reason,
        Instant createdAt,
        String createdBy
    ) {
    }

    /** PO read shape. {@code id} is the ULID public id. */
    public record PoResponse(
        String id,
        String number,
        String supplierId,
        String locationId,
        Instant poDate,
        Instant expectedDeliveryDate,
        String currency,
        BigDecimal exchangeRate,
        String paymentTerms,
        String sourcePrId,
        String status,
        String closeReason,
        int poVersion,
        String invoiceDiscountType,
        BigDecimal invoiceDiscountValue,
        boolean isDirect,
        String notes,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<PoLineResponse> lines
    ) {
    }
}
