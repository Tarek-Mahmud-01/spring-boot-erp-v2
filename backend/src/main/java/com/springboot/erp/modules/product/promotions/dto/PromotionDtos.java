package com.springboot.erp.modules.product.promotions.dto;

import com.springboot.erp.modules.product.promotions.domain.PromotionStatus;
import com.springboot.erp.modules.product.promotions.domain.PromotionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Request/response DTOs for ENT-014 Promotion (records — ARCHITECTURE.md §2).
 * Field constraints mirror the reference Pydantic schemas; cross-field business
 * rules (date range, config shape, name uniqueness, post-sale lock) live in the
 * service so the wire error code matches the rule.
 */
public final class PromotionDtos {

    private PromotionDtos() {
    }

    /** US-014 (FR-065..068) — create a promotion. */
    public record PromotionCreateRequest(
        @NotNull @Size(min = 1, max = 200) String name,
        @NotNull PromotionType type,
        @NotNull Map<String, Object> config,
        Map<String, Object> scope,
        @NotNull Instant dateFrom,
        @NotNull Instant dateTo,
        @NotNull @Size(min = 26, max = 26) String companyId,
        Boolean stackable,
        Boolean reasonRequired,
        @Size(max = 26) String segmentId,
        PromotionStatus status
    ) {
    }

    /**
     * US-014 — partial edit. Every field optional; {@code companyId} is
     * intentionally absent (a promotion stays with its company).
     */
    public record PromotionUpdateRequest(
        @Size(min = 1, max = 200) String name,
        PromotionType type,
        Map<String, Object> config,
        Map<String, Object> scope,
        Instant dateFrom,
        Instant dateTo,
        Boolean stackable,
        Boolean reasonRequired,
        @Size(max = 26) String segmentId,
        PromotionStatus status,
        Long version
    ) {
    }

    /** Status-only PATCH convenience path. */
    public record PromotionStatusUpdateRequest(
        @NotNull PromotionStatus status,
        Long version
    ) {
    }

    /** Read shape. {@code id} is the ULID publicId; {@code warnings} are non-blocking advisories. */
    public record PromotionResponse(
        String id,
        String companyId,
        String name,
        String type,
        Map<String, Object> config,
        Map<String, Object> scope,
        String segmentId,
        Instant dateFrom,
        Instant dateTo,
        boolean stackable,
        boolean reasonRequired,
        String status,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<String> warnings
    ) {
    }
}
