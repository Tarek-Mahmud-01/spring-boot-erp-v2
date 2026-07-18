package com.guru.erp.modules.inventory.counts.dto;

import com.guru.erp.modules.inventory.counts.domain.BarcodeRuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wire DTOs for ENT-046 BarcodeNomenclatureRule (records — ARCHITECTURE.md §2).
 * {@code measureScale} is {@link BigDecimal} (NUMERIC(10,6)); it is a scale
 * factor, not money, so BigDecimal (never double).
 */
public final class BarcodeRuleDtos {

    private BarcodeRuleDtos() {
    }

    /** POST /api/inventory/barcode-rules body. */
    public record BarcodeRuleCreateRequest(
        @NotBlank @Size(min = 26, max = 26) String companyId,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 4) String prefix,
        @NotNull BarcodeRuleType ruleType,
        @Positive int itemDigits,
        @Positive int measureDigits,
        @NotNull @Positive BigDecimal measureScale,
        Boolean isActive
    ) {
    }

    /** PATCH /api/inventory/barcode-rules/{id} — every field optional. */
    public record BarcodeRuleUpdateRequest(
        @Size(max = 100) String name,
        @Size(max = 4) String prefix,
        BarcodeRuleType ruleType,
        @Positive Integer itemDigits,
        @Positive Integer measureDigits,
        @Positive BigDecimal measureScale,
        Boolean isActive,
        Long version
    ) {
    }

    /** Read shape. {@code id} is the rule ULID. */
    public record BarcodeRuleResponse(
        String id,
        String companyId,
        String name,
        String prefix,
        BarcodeRuleType ruleType,
        int itemDigits,
        int measureDigits,
        BigDecimal measureScale,
        boolean isActive,
        long version,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}
