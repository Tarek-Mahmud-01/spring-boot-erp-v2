package com.guru.erp.modules.product.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response DTOs for ENT-011c ProductImage (FR-046). */
public final class ImageDtos {

    private ImageDtos() {
    }

    public record ImageCreateRequest(
        @NotBlank @Size(min = 1, max = 80) String fileId,
        @Min(0) long bytes
    ) {
    }

    public record ImageResponse(
        String id,
        String productId,
        String fileId,
        int position,
        long bytes
    ) {
    }
}
