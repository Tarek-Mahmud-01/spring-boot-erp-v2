package com.guru.erp.modules.crm.loyalty.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** DTOs for the per-company loyalty program config (reference US-039 / FR-205-207). */
public final class LoyaltyConfigDtos {

    private LoyaltyConfigDtos() {
    }

    public record EarnRule(
        @DecimalMin(value = "0.0", inclusive = false) double currencyUnitPerPoint,
        List<String> eligibleCategoryIds
    ) {
    }

    public record RedemptionRule(
        @Min(1) int pointsPerCurrencyUnit,
        @Min(0) int minBalanceForRedemption
    ) {
    }

    public record LoyaltyTier(
        String id,
        @NotBlank @Size(max = 24) String code,
        @NotBlank @Size(max = 60) String name,
        @Min(0) long minSpendAmount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @DecimalMin(value = "0.0", inclusive = false) double earnMultiplier
    ) {
    }

    /** US-039 — create or update the company's single loyalty program. */
    public record LoyaltyProgramUpsertRequest(
        @NotBlank @Size(min = 26, max = 26) String companyId,
        @Size(min = 1, max = 120) String name,
        Boolean active,
        @Size(min = 3, max = 3) String currency,
        @Valid EarnRule earn,
        @Valid RedemptionRule redeem,
        @Positive Integer expiryMonths
    ) {
    }

    /** FR-206 — add or edit a loyalty tier. {@code id} null = create. */
    public record TierUpsertRequest(
        String id,
        @NotBlank @Size(max = 24) String code,
        @NotBlank @Size(max = 60) String name,
        @Min(0) long minSpendAmount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @DecimalMin(value = "0.0", inclusive = false) double earnMultiplier
    ) {
    }

    public record LoyaltyProgramResponse(
        String id,
        String companyId,
        String name,
        boolean active,
        String currency,
        EarnRule earn,
        RedemptionRule redeem,
        Integer expiryMonths,
        List<LoyaltyTier> tiers,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) {
    }
}
