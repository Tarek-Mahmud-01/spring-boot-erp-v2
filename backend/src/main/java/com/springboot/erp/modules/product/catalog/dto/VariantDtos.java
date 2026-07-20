package com.springboot.erp.modules.product.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/** Request/response DTOs for ENT-011a ProductVariant (FR-047). */
public final class VariantDtos {

    private VariantDtos() {
    }

    public record VariantInputRequest(
        @NotBlank @Size(min = 1, max = 50) String sku,
        Map<String, Object> attributes,
        @Min(0) long costAmount,
        @NotBlank @Size(min = 3, max = 3) String costCurrency,
        @Min(0) long sellAmount,
        @NotBlank @Size(min = 3, max = 3) String sellCurrency,
        @Size(min = 26, max = 26) String taxCodeId
    ) {
    }

    public record VariantsCreateRequest(
        @NotEmpty @Valid List<VariantInputRequest> variants
    ) {
    }

    /** Partial PATCH of a single variant. {@code taxCodeSet} distinguishes
     *  "clear to inherit" (true + null) from "leave unchanged" (false/absent). */
    public record VariantUpdateRequest(
        @Size(min = 1, max = 50) String sku,
        Map<String, Object> attributes,
        @Min(0) Long costAmount,
        @Size(min = 3, max = 3) String costCurrency,
        @Min(0) Long sellAmount,
        @Size(min = 3, max = 3) String sellCurrency,
        @Size(min = 26, max = 26) String taxCodeId,
        Boolean taxCodeSet,
        Long version
    ) {
    }

    public record VariantResponse(
        String id,
        String productId,
        String sku,
        Map<String, Object> attributes,
        long costAmount,
        String costCurrency,
        long poCostAmount,
        long landedCostAmount,
        long currentCostAmount,
        long sellAmount,
        String sellCurrency,
        String taxCodeId,
        List<String> imageKeys,
        long version
    ) {
    }
}
