package com.guru.erp.modules.product.promotions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request/response DTOs for the FR-068 discount reason-code master (records).
 * {@code code} is immutable after creation, so it only appears on create.
 */
public final class ReasonCodeDtos {

    private ReasonCodeDtos() {
    }

    public record ReasonCodeCreateRequest(
        @NotBlank @Size(min = 1, max = 50) String code,
        @NotBlank @Size(min = 1, max = 200) String label,
        @Size(max = 500) String description,
        Boolean isActive
    ) {
    }

    /** Partial PATCH — {@code code} is immutable so existing references stay stable. */
    public record ReasonCodeUpdateRequest(
        @Size(min = 1, max = 200) String label,
        @Size(max = 500) String description,
        Boolean isActive,
        Long version
    ) {
    }

    public record ReasonCodeResponse(
        String id,
        String code,
        String label,
        String description,
        boolean isActive,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
