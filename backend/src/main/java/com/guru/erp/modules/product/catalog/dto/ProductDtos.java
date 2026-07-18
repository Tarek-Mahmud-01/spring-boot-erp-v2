package com.guru.erp.modules.product.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

/**
 * Request/response DTOs for ENT-011 Product (records per ARCHITECTURE.md §2).
 * Field constraints mirror the reference Pydantic schemas; cross-field business
 * rules (negative price, SKU collisions, tax resolution, weighed-goods PLU) live
 * in the service so the wire error code matches the rule.
 */
public final class ProductDtos {

    private ProductDtos() {
    }

    public record ProductCreateRequest(
        @NotBlank @Size(min = 1, max = 50) String sku,
        @NotBlank @Size(min = 1, max = 200) String name,
        @NotBlank @Size(min = 26, max = 26) String categoryId,
        @NotBlank @Size(min = 26, max = 26) String uomId,
        @Min(0) long sellAmount,
        @Min(0) long costAmount,
        @NotBlank @Size(min = 3, max = 3) String costCurrency,
        @NotBlank @Size(min = 3, max = 3) String sellCurrency,
        String description,
        @Size(max = 100) String brand,
        @Size(min = 26, max = 26) String supplierId,
        @Size(min = 26, max = 26) String taxCodeId,
        @Min(0) Integer weightGrams,
        Map<String, Object> dimensions,
        Boolean hasVariants,
        Boolean restrictionAge18,
        Boolean restrictionAge21,
        Boolean restrictionControlledDisplay,
        @Size(max = 500) String restrictionNote,
        Boolean soldByWeight,
        @Size(min = 1, max = 20) String plu,
        @Min(0) Long pricePerKgAmount
    ) {
    }

    /** Partial PATCH. {@code sku} is intentionally omitted — immutable after create. */
    public record ProductUpdateRequest(
        @Size(min = 1, max = 200) String name,
        String description,
        @Size(max = 100) String brand,
        @Size(min = 26, max = 26) String supplierId,
        @Size(min = 26, max = 26) String categoryId,
        @Size(min = 26, max = 26) String uomId,
        @Size(min = 26, max = 26) String taxCodeId,
        @Min(0) Long costAmount,
        @Min(0) Long sellAmount,
        @Size(min = 3, max = 3) String costCurrency,
        @Size(min = 3, max = 3) String sellCurrency,
        @Min(0) Integer weightGrams,
        Map<String, Object> dimensions,
        Boolean restrictionAge18,
        Boolean restrictionAge21,
        Boolean restrictionControlledDisplay,
        @Size(max = 500) String restrictionNote,
        Boolean soldByWeight,
        @Size(min = 1, max = 20) String plu,
        @Min(0) Long pricePerKgAmount,
        Long version
    ) {
    }

    public record ProductResponse(
        String id,
        String sku,
        String name,
        String description,
        String categoryId,
        String uomId,
        String taxCodeId,
        String brand,
        String supplierId,
        long costAmount,
        String costCurrency,
        long poCostAmount,
        long landedCostAmount,
        long currentCostAmount,
        long sellAmount,
        String sellCurrency,
        Integer weightGrams,
        Map<String, Object> dimensions,
        boolean hasVariants,
        boolean isActive,
        String lifecycleState,
        boolean restrictionAge18,
        boolean restrictionAge21,
        boolean restrictionControlledDisplay,
        String restrictionNote,
        boolean soldByWeight,
        String plu,
        Long pricePerKgAmount,
        Instant pricePerKgSyncedAt,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
