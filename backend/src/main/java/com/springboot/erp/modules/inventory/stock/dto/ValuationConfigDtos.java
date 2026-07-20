package com.springboot.erp.modules.inventory.stock.dto;

import com.springboot.erp.modules.inventory.stock.domain.ValuationMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Wire DTOs for {@link com.springboot.erp.modules.inventory.stock.domain.InventoryValuationConfig}
 * (reference {@code ValuationConfigSetRequest} / {@code ValuationConfigResponse}).
 * {@code companyId} is the company's ULID public id.
 */
public final class ValuationConfigDtos {

    private ValuationConfigDtos() {
    }

    /** PUT /api/inventory/valuation-config body — set (or create) the method. */
    public record ValuationConfigSetRequest(
        @NotBlank @Size(min = 26, max = 26) String companyId,
        @NotNull ValuationMethod method,
        Long version
    ) {
    }

    public record ValuationConfigResponse(
        String id,
        String companyId,
        ValuationMethod method,
        boolean locked,
        long version,
        Instant updatedAt
    ) {
    }
}
