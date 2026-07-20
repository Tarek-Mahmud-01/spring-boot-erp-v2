package com.springboot.erp.modules.procurement.receipts.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Wire DTOs for ENT-029 GoodsReceipt (records — ARCHITECTURE.md §2). Mirrors the reference Pydantic
 * constraints: {@code po_id}/{@code location_id} are 26-char ULIDs, {@code received_at} is required
 * (no server-invented accounting date), quantities are non-negative {@code numeric(18,6)}. Cross-
 * slice ids are ULID {@code char(26)} strings.
 */
public final class ReceiptDtos {

    private ReceiptDtos() {
    }

    /**
     * One line of a create payload. {@code poLineId} links to the source PurchaseOrderLine
     * (nullable for standalone receipts). {@code qtyReceived} is the received quantity;
     * {@code qtyDiscrepancy} the portion short/damaged that does NOT hit stock.
     */
    public record GrnLineCreateRequest(
        @Size(min = 26, max = 26) String poLineId,
        @NotNull @PositiveOrZero BigDecimal qtyReceived,
        @PositiveOrZero BigDecimal qtyDiscrepancy,
        String discrepancyType,
        @Size(max = 500) String discrepancyNote,
        @Size(min = 26, max = 26) String variantId,
        @Size(max = 100) String batchNo,
        @Size(max = 100) String serialNo,
        Instant expiryDate,
        @Size(max = 64) String supplierBarcode,
        Instant manufactureDate,
        @PositiveOrZero Long mrpAmount,
        @Size(max = 3) String mrpCurrency,
        @PositiveOrZero Long sellPriceAmount,
        @Size(max = 3) String sellPriceCurrency
    ) {
    }

    /**
     * POST /api/procurement/receipts body. {@code autoReceipt} with no lines auto-creates lines
     * from the PO (deferred cross-slice — noted). {@code confirm} posts stock in the same call.
     */
    public record GrnCreateRequest(
        @NotNull @Size(min = 26, max = 26) String poId,
        @NotNull @Size(min = 26, max = 26) String locationId,
        @NotNull Instant receivedAt,
        boolean autoReceipt,
        boolean confirm,
        @Size(max = 100) String deliveryNoteNo,
        @Size(max = 2000) String notes,
        @Valid List<GrnLineCreateRequest> lines
    ) {
    }

    /** One editable line of a PATCH payload — keyed by the existing GRN line's ULID. */
    public record GrnLinePatchItem(
        @NotNull @Size(min = 26, max = 26) String id,
        @NotNull @PositiveOrZero BigDecimal qtyReceived,
        @Size(max = 64) String supplierBarcode,
        Instant manufactureDate,
        @PositiveOrZero Long mrpAmount,
        @Size(max = 3) String mrpCurrency,
        @PositiveOrZero Long sellPriceAmount,
        @Size(max = 3) String sellPriceCurrency,
        @Size(max = 100) String batchNo,
        Instant expiryDate
    ) {
    }

    /** PATCH /api/procurement/receipts/{id} — header fields + optional per-line qty edits. */
    public record GrnPatchRequest(
        Instant receivedAt,
        @Size(max = 100) String deliveryNoteNo,
        @Size(max = 2000) String notes,
        @Valid List<GrnLinePatchItem> lines,
        Long version
    ) {
    }

    /** Body for the generic status transition endpoint (Draft→Approved, Approved→Partially Received). */
    public record GrnTransitionRequest(
        @NotNull String toStatus
    ) {
    }

    /** GRN line read shape. */
    public record GrnLineResponse(
        String id,
        String poLineId,
        String variantId,
        BigDecimal qtyReceived,
        BigDecimal qtyDiscrepancy,
        String discrepancyType,
        String discrepancyNote,
        String batchNo,
        String serialNo,
        Instant expiryDate,
        String supplierBarcode,
        Instant manufactureDate,
        Long mrpAmount,
        String mrpCurrency,
        Long sellPriceAmount,
        String sellPriceCurrency
    ) {
    }

    /** GRN read shape. {@code id} is the ULID public id; {@code poId} the source PO's ULID. */
    public record GrnResponse(
        String id,
        String number,
        String poId,
        String locationId,
        Instant receivedAt,
        String receivedBy,
        String status,
        boolean autoReceipt,
        String deliveryNoteNo,
        String notes,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<GrnLineResponse> lines
    ) {
    }
}
