package com.springboot.erp.modules.procurement.bills.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Wire DTOs for ENT-030 SupplierBill (records — ARCHITECTURE.md §2). Cross-slice ids are ULID
 * {@code char(26)} strings. Money is bigint minor units; {@code qty} is a decimal. Mirrors the
 * reference Pydantic constraints (bill_date required, qty {@literal >} 0, unit price {@literal >=} 0).
 */
public final class BillDtos {

    private BillDtos() {
    }

    /** One line of a create payload; matched against a PO line / GRN line for the 3-way match. */
    public record BillLineCreateRequest(
        @Size(min = 26, max = 26) String poLineId,
        @Size(min = 26, max = 26) String grnLineId,
        @Size(min = 26, max = 26) String productId,
        @Size(max = 500) String description,
        @NotNull @Positive BigDecimal qty,
        @PositiveOrZero long unitPriceAmount,
        @Size(min = 26, max = 26) String taxCodeId,
        boolean isCapitalItem
    ) {
    }

    /** POST /api/procurement/bills body. */
    public record BillCreateRequest(
        @NotNull @Size(min = 26, max = 26) String supplierId,
        @Size(min = 26, max = 26) String poId,
        List<@Size(min = 26, max = 26) String> grnIds,
        @Size(max = 100) String supplierBillNo,
        @NotNull Instant billDate,
        Instant dueDate,
        @Size(max = 3) String currency,
        @Size(max = 2000) String notes,
        @Valid List<BillLineCreateRequest> lines
    ) {
    }

    /** One line of an update payload (full replacement of the bill's line set). */
    public record BillLineUpdateRequest(
        @Size(min = 26, max = 26) String poLineId,
        @Size(min = 26, max = 26) String grnLineId,
        @Size(min = 26, max = 26) String productId,
        @Size(max = 500) String description,
        @NotNull @Positive BigDecimal qty,
        @PositiveOrZero long unitPriceAmount,
        @Size(min = 26, max = 26) String taxCodeId,
        boolean isCapitalItem
    ) {
    }

    /**
     * PATCH /api/procurement/bills/{id} body. Header fields edit in any status; {@code lines}
     * (when non-null) replaces the full set and recomputes totals — rejected past Draft if the
     * totals change (payable already posted). {@code poId} present-and-null clears the link.
     */
    public record BillUpdateRequest(
        @Size(max = 100) String supplierBillNo,
        Instant billDate,
        Instant dueDate,
        @Size(max = 2000) String notes,
        Long version,
        @Valid List<BillLineUpdateRequest> lines,
        @Size(min = 26, max = 26) String poId,
        @Size(min = 3, max = 3) String currency
    ) {
    }

    /** Bill line read shape. */
    public record BillLineResponse(
        String id,
        String poLineId,
        String grnLineId,
        String productId,
        String variantId,
        String description,
        BigDecimal qty,
        long unitPriceAmount,
        String taxCodeId,
        long lineTotalAmount,
        String matchStatus,
        boolean isCapitalItem
    ) {
    }

    /** Bill read shape. {@code id} is the ULID public id. */
    public record BillResponse(
        String id,
        String number,
        String supplierId,
        String poId,
        String supplierBillNo,
        Instant billDate,
        Instant dueDate,
        String currency,
        BigDecimal exchangeRate,
        long subtotalAmount,
        long taxAmount,
        long totalAmount,
        String status,
        String matchStatus,
        String notes,
        List<String> grnIds,
        List<String> poIds,
        List<BillLineResponse> lines,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
