package com.guru.erp.modules.product.catalog.dto;

import com.guru.erp.modules.product.catalog.domain.BarcodeFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request/response DTOs for ENT-011b ProductBarcode (FR-056–059). */
public final class BarcodeDtos {

    private BarcodeDtos() {
    }

    public record BarcodeCreateRequest(
        @NotBlank @Size(min = 1, max = 64) String barcode,
        @NotNull BarcodeFormat format,
        Boolean isPrimary,
        @Size(min = 26, max = 26) String variantId
    ) {
    }

    public record BarcodeResponse(
        String id,
        String productId,
        String variantId,
        String barcode,
        String format,
        boolean isPrimary
    ) {
    }
}
