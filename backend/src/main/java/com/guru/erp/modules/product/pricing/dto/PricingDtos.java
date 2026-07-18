package com.guru.erp.modules.product.pricing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request/response DTOs for the PRODUCT pricing sub-slice (records, per
 * ARCHITECTURE.md §2). Field constraints mirror the reference Pydantic schemas
 * in {@code app/products/schemas.py}. Money crosses the wire as long minor units
 * ({@code priceAmount}) + a 3-letter ISO-4217 {@code priceCurrency}, never a
 * float. Every {@code id} field is a ULID publicId — internal ids are never
 * exposed.
 */
public final class PricingDtos {

    private PricingDtos() {
    }

    // --- PriceList ---

    /** Create a named price list for a company (FR-061). */
    public record PriceListCreateRequest(
        @NotBlank @Size(min = 1, max = 100) String name,
        @NotBlank @Size(min = 3, max = 3)
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be 3 ISO-4217 letters")
        String currency,
        @NotBlank @Size(min = 26, max = 26) String companyId,
        @Size(max = 16) String priceDisplayMode
    ) {
    }

    /** FR-061 (T17) — rename a price list / toggle active status. */
    public record PriceListUpdateRequest(
        @Size(min = 1, max = 100) String name,
        @Pattern(regexp = "^(active|inactive)$", message = "status must be active|inactive")
        String status,
        Long version
    ) {
    }

    public record PriceListResponse(
        String id,
        String companyId,
        String name,
        String currency,
        String status,
        String priceDisplayMode,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    // --- PriceListItem ---

    /** FR-061/FR-062 — schedule (upsert) a possibly future price for a product. */
    public record PriceListItemUpsertRequest(
        @NotBlank @Size(min = 26, max = 26) String productId,
        @Min(0) long priceAmount,
        @NotBlank @Size(min = 3, max = 3)
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "priceCurrency must be 3 ISO-4217 letters")
        String priceCurrency,
        Instant effectiveFrom,
        Instant effectiveTo,
        @Size(min = 26, max = 26) String variantId
    ) {
    }

    /** BUG-PL-013 — edit price/dates on an existing price list item. */
    public record PriceListItemPatchRequest(
        @Min(0) Long priceAmount,
        @Size(min = 3, max = 3)
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "priceCurrency must be 3 ISO-4217 letters")
        String priceCurrency,
        Instant effectiveFrom,
        Instant effectiveTo
    ) {
    }

    public record PriceListItemResponse(
        String id,
        String priceListId,
        String productId,
        String variantId,
        long priceAmount,
        String priceCurrency,
        Instant effectiveFrom,
        Instant effectiveTo
    ) {
    }

    // --- PriceHistory ---

    /** FR-062 / AC-013-2 — schedule a future base sell-price change. */
    public record ScheduledPriceChangeRequest(
        @NotBlank @Size(min = 26, max = 26) String productId,
        @Min(0) long newSellAmount,
        @NotBlank @Size(min = 3, max = 3)
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be 3 ISO-4217 letters")
        String currency,
        Instant effectiveFrom,
        @Size(min = 26, max = 26) String variantId,
        Long oldAmount
    ) {
    }

    public record PriceHistoryResponse(
        String id,
        String productId,
        String variantId,
        String field,
        Long oldAmount,
        long newAmount,
        String currency,
        Instant effectiveFrom,
        Instant changedAt,
        String changedBy
    ) {
    }

    // --- ProductLocationOverride ---

    /** FR-063 (T7) — set a product's price at a specific location. */
    public record LocationOverrideUpsertRequest(
        @NotBlank @Size(min = 26, max = 26) String productId,
        @NotBlank @Size(min = 26, max = 26) String locationId,
        @Min(0) long priceAmount,
        @NotBlank @Size(min = 3, max = 3)
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "priceCurrency must be 3 ISO-4217 letters")
        String priceCurrency,
        @Size(min = 26, max = 26) String variantId
    ) {
    }

    public record LocationOverrideResponse(
        String id,
        String productId,
        String locationId,
        String variantId,
        long priceAmount,
        String priceCurrency,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
