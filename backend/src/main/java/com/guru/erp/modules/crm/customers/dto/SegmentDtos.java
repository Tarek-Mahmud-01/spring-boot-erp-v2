package com.guru.erp.modules.crm.customers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Wire DTOs for ENT-073 CustomerSegment + ENT-073a CustomerSegmentMember
 * (records — ARCHITECTURE.md §2). Mirrors the reference Pydantic schemas
 * ({@code SegmentCreateRequest}/{@code SegmentUpdateRequest}/
 * {@code SegmentResponse}/{@code SegmentRule}).
 */
public final class SegmentDtos {

    private SegmentDtos() {
    }

    /** One dynamic-segment rule: {@code field op value}, optionally windowed. */
    public record SegmentRule(
        @NotBlank String field,
        @NotBlank String op,
        @NotBlank String value,
        Integer windowDays,
        @Size(min = 3, max = 3) String currency
    ) {
    }

    /** POST /api/crm/segments body. */
    public record SegmentCreateRequest(
        @NotNull @Size(min = 26, max = 26) String companyId,
        @NotBlank @Size(min = 1, max = 40) String code,
        @NotBlank @Size(min = 1, max = 120) String name,
        String description,
        String mode,
        List<SegmentRule> rules,
        List<String> memberIds,
        String ruleLogic
    ) {
    }

    /** PATCH /api/crm/segments/{id} body — partial update. */
    public record SegmentUpdateRequest(
        @Size(min = 1, max = 120) String name,
        String description,
        List<SegmentRule> rules,
        List<String> memberIds,
        List<String> linkedPromotionIds,
        String ruleLogic,
        Long version
    ) {
    }

    /** POST /api/crm/segments/{id}/members body — add/remove a batch of customers. */
    public record SegmentMemberChangeRequest(
        @NotNull List<String> customerIds
    ) {
    }

    /** Segment read shape. {@code id} is the ULID public id. */
    public record SegmentResponse(
        String id,
        String companyId,
        String code,
        String name,
        String description,
        String mode,
        List<Map<String, Object>> rules,
        String ruleLogic,
        List<String> memberIds,
        Instant refreshedAt,
        List<String> linkedPromotionIds,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
