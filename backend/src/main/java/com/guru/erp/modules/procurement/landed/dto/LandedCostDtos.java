package com.guru.erp.modules.procurement.landed.dto;

import com.guru.erp.modules.procurement.landed.domain.AllocationBasis;
import com.guru.erp.modules.procurement.landed.domain.LandedCostChargeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Wire DTOs for ENT-032 LandedCost (records — ARCHITECTURE.md §2). Mirrors the reference Pydantic
 * shapes: a multi-line charge invoice targeting either a GRN set OR a PO set (never both), an
 * allocation basis, and an optional per-line qty override map ({grnLineId|poLineId → qty}) for
 * partial allocation. Money is minor units + ISO-4217 currency. Cross-slice ids are ULID
 * {@code char(26)} strings.
 */
public final class LandedCostDtos {

    private LandedCostDtos() {
    }

    /** One charge line of a create/update payload (Freight, Customs, Insurance, …). */
    public record LandedCostChargeLineRequest(
        @NotNull LandedCostChargeType chargeType,
        @Positive long amount
    ) {
    }

    /**
     * POST /api/procurement/landed-costs body. Exactly one of {@code grnIds} / {@code poIds} carries
     * targets (validated in the service → LANDED_COST_AMBIGUOUS_SOURCE when both are set). {@code
     * allocatedAt} is the charge's accounting date (required — never invented). {@code
     * lineQtyOverrides} keys are target line ULIDs.
     */
    public record LandedCostCreateRequest(
        List<@Size(min = 26, max = 26) String> grnIds,
        List<@Size(min = 26, max = 26) String> poIds,
        @NotEmpty @Valid List<LandedCostChargeLineRequest> lines,
        @Size(min = 26, max = 26) String supplierId,
        @Size(max = 3) String currency,
        @NotNull AllocationBasis allocationBasis,
        @NotNull Instant allocatedAt,
        Map<String, BigDecimal> lineQtyOverrides
    ) {
    }

    /**
     * PATCH /api/procurement/landed-costs/{id} body — non-financial edits. Changing amount / currency
     * re-runs FX + reallocation (reference behaviour); changing the basis is rejected
     * (LANDED_COST_BASIS_IMMUTABLE) — delete and recreate instead.
     */
    public record LandedCostUpdateRequest(
        LandedCostChargeType chargeType,
        @Size(min = 26, max = 26) String supplierId,
        @Size(min = 26, max = 26) String grnId,
        Long amount,
        @Size(max = 3) String currency,
        AllocationBasis allocationBasis,
        Instant allocatedAt,
        Long version
    ) {
    }

    /** One allocation line read shape. */
    public record LandedCostAllocationResponse(
        String id,
        String grnLineId,
        String poLineId,
        long allocatedAmount,
        String allocatedCurrency,
        BigDecimal allocQty
    ) {
    }

    /** Landed cost read shape. {@code id} is the ULID public id. */
    public record LandedCostResponse(
        String id,
        String invoiceNumber,
        String grnId,
        String poId,
        String supplierId,
        String chargeType,
        long amount,
        String currency,
        BigDecimal exchangeRate,
        long baseAmount,
        String baseCurrency,
        String allocationBasis,
        String status,
        Instant allocatedAt,
        Instant appliedAt,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<LandedCostAllocationResponse> allocations
    ) {
    }

    /** Aggregated one-row-per-invoice list item (reference invoice grouping). */
    public record LandedCostInvoiceResponse(
        String id,
        String invoiceNumber,
        String grnId,
        String supplierId,
        String chargeType,
        long amount,
        String currency,
        String allocationBasis,
        Instant allocatedAt,
        int lineCount
    ) {
    }
}
