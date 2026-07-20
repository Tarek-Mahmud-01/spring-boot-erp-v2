package com.springboot.erp.modules.product.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request/response DTOs for ENT-012 CustomAttributeValue. */
public final class CustomAttributeDtos {

    private CustomAttributeDtos() {
    }

    public record CustomAttributeUpsertRequest(
        @NotBlank @Size(min = 1, max = 100) String key,
        String value,
        @Pattern(regexp = "^(product|variant)$", message = "scope must be 'product' or 'variant'")
        String scope,
        @Size(min = 26, max = 26) String variantId
    ) {
    }

    public record CustomAttributeResponse(
        String id,
        String productId,
        String variantId,
        String scope,
        String key,
        String value,
        long version
    ) {
    }
}
