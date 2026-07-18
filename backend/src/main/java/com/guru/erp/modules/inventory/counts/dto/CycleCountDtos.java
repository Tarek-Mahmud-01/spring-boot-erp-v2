package com.guru.erp.modules.inventory.counts.dto;

import com.guru.erp.modules.inventory.counts.domain.CycleCountScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Wire DTOs for ENT-044 CycleCountPlan / ENT-044a CycleCountLine (records —
 * ARCHITECTURE.md §2). Bean-validation mirrors the reference Pydantic
 * constraints; lifecycle rules live in the service. Quantities are
 * {@link BigDecimal} to match the NUMERIC(18,6) columns (never double).
 */
public final class CycleCountDtos {

    private CycleCountDtos() {
    }

    /** POST /api/inventory/cycle-counts — opens a plan and generates its lines. */
    public record CycleCountCreateRequest(
        @NotBlank @Size(min = 26, max = 26) String locationId,
        CycleCountScope scope,
        Map<String, Object> scopeConfig,
        @NotNull Instant plannedDate,
        @Size(max = 2000) String notes
    ) {
    }

    /** One first-pass count entry (references a line by its ULID). */
    public record LineCountRequest(
        @NotBlank @Size(min = 26, max = 26) String lineId,
        @NotNull @PositiveOrZero BigDecimal qtyFirstPass
    ) {
    }

    /** One second-pass (recount) entry. */
    public record LineSecondPassRequest(
        @NotBlank @Size(min = 26, max = 26) String lineId,
        @NotNull @PositiveOrZero BigDecimal qtySecondPass
    ) {
    }

    /** Read shape for a single counted line. {@code id} is the line's ULID. */
    public record CycleCountLineResponse(
        String id,
        int lineNo,
        String productId,
        BigDecimal qtyExpected,
        BigDecimal qtyFirstPass,
        BigDecimal qtySecondPass,
        BigDecimal variance,
        boolean requiresRecount
    ) {
    }

    /** Read shape for a plan header + its lines. {@code id} is the plan ULID. */
    public record CycleCountResponse(
        String id,
        String number,
        String locationId,
        CycleCountScope scope,
        Map<String, Object> scopeConfig,
        Instant plannedDate,
        String status,
        BigDecimal accuracyPct,
        String notes,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<CycleCountLineResponse> lines
    ) {
    }
}
