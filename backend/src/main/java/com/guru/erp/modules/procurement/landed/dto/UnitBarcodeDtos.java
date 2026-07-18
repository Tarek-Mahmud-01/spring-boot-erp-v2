package com.guru.erp.modules.procurement.landed.dto;

import com.guru.erp.modules.procurement.landed.domain.BarcodeFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Wire DTOs for the unit-barcode / label-size setup (records — ARCHITECTURE.md §2). Mirrors the
 * reference create / bulk-generate / patch / bundle shapes. Prices are minor units + ISO-4217
 * currency; cross-slice ids are ULID {@code char(26)} strings.
 */
public final class UnitBarcodeDtos {

    private UnitBarcodeDtos() {
    }

    /** One bundle-component line. */
    public record UnitBarcodeItemRequest(
        @NotNull @Size(min = 26, max = 26) String productId,
        @Size(min = 26, max = 26) String variantId,
        @Size(min = 26, max = 26) String grnLineId,
        @Positive BigDecimal qty
    ) {
    }

    /** POST /api/procurement/unit-barcodes — assign one barcode to a GRN line. */
    public record UnitBarcodeCreateRequest(
        @NotNull @Size(min = 26, max = 26) String grnLineId,
        @NotBlank @Size(max = 100) String barcode,
        BarcodeFormat barcodeFormat,
        boolean bundle,
        @Size(min = 26, max = 26) String productId,
        @Size(min = 26, max = 26) String variantId,
        @Positive BigDecimal qty,
        Long mrpAmount,
        @Size(max = 3) String mrpCurrency,
        Long sellPriceAmount,
        @Size(max = 3) String sellPriceCurrency,
        @Size(max = 100) String batchNo,
        @Size(max = 100) String serialNo,
        LocalDate expiryDate,
        LocalDate entryDate,
        @Size(max = 500) String notes,
        @Valid List<UnitBarcodeItemRequest> items
    ) {
    }

    /** POST /api/procurement/unit-barcodes/bulk-generate — auto-generate N EAN-13 barcodes. */
    public record UnitBarcodeBulkGenerateRequest(
        @NotNull @Size(min = 26, max = 26) String grnLineId,
        @Positive int count,
        @Size(min = 26, max = 26) String productId,
        @Size(min = 26, max = 26) String variantId,
        Long mrpAmount,
        @Size(max = 3) String mrpCurrency,
        Long sellPriceAmount,
        @Size(max = 3) String sellPriceCurrency
    ) {
    }

    /** PATCH /api/procurement/unit-barcodes/{id} — update price / status / notes (all optional). */
    public record UnitBarcodePatchRequest(
        @Size(max = 100) String barcode,
        BigDecimal qty,
        Long mrpAmount,
        @Size(max = 3) String mrpCurrency,
        Long sellPriceAmount,
        @Size(max = 3) String sellPriceCurrency,
        @Size(max = 100) String batchNo,
        @Size(max = 100) String serialNo,
        LocalDate expiryDate,
        LocalDate entryDate,
        String status,
        @Size(max = 500) String notes,
        Long version
    ) {
    }

    /** Bundle-component read shape. */
    public record UnitBarcodeItemResponse(
        String id,
        String grnLineId,
        String productId,
        String variantId,
        BigDecimal qty
    ) {
    }

    /** Unit barcode read shape. {@code id} is the ULID public id. */
    public record UnitBarcodeResponse(
        String id,
        String grnLineId,
        String grnId,
        String locationId,
        String barcode,
        String barcodeFormat,
        boolean bundle,
        String productId,
        String variantId,
        BigDecimal qty,
        Long mrpAmount,
        String mrpCurrency,
        Long sellPriceAmount,
        String sellPriceCurrency,
        String batchNo,
        String serialNo,
        LocalDate expiryDate,
        LocalDate entryDate,
        String status,
        String notes,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<UnitBarcodeItemResponse> items
    ) {
    }

    /** Quota summary for a GRN line — how many units received vs. already barcoded. */
    public record GrnLineSummaryResponse(
        String grnLineId,
        String grnId,
        BigDecimal assignedQty
    ) {
    }

    /** POST /api/procurement/bundles — create a multi-product bundle barcode. */
    public record BundleCreateRequest(
        @NotBlank @Size(max = 100) String barcode,
        BarcodeFormat barcodeFormat,
        @Size(max = 500) String name,
        Long mrpAmount,
        @Size(max = 3) String mrpCurrency,
        Long sellPriceAmount,
        @Size(max = 3) String sellPriceCurrency,
        @NotNull @Valid List<UnitBarcodeItemRequest> items
    ) {
    }

    /** PATCH /api/procurement/bundles/{id} — update a bundle (all optional). */
    public record BundleUpdateRequest(
        @Size(max = 100) String barcode,
        BarcodeFormat barcodeFormat,
        @Size(max = 500) String name,
        Long mrpAmount,
        @Size(max = 3) String mrpCurrency,
        Long sellPriceAmount,
        @Size(max = 3) String sellPriceCurrency,
        @Valid List<UnitBarcodeItemRequest> items,
        Long version
    ) {
    }

    /** Label size read shape (Print Label dropdown). */
    public record LabelSizeResponse(
        String id,
        String name,
        int widthMm,
        int heightMm,
        boolean isDefault,
        int sortOrder,
        boolean active
    ) {
    }

    /** Global duplicate-barcode check result. */
    public record BarcodeFreeCheckResponse(
        boolean exists
    ) {
    }
}
