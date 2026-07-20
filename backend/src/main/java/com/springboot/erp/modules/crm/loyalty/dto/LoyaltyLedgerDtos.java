package com.springboot.erp.modules.crm.loyalty.dto;

import com.springboot.erp.modules.crm.loyalty.domain.LoyaltyMovementType;
import com.springboot.erp.modules.crm.loyalty.dto.LoyaltyConfigDtos.LoyaltyTier;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** DTOs for the loyalty points mechanics: balance, ledger, earn/redeem/reverse (reference US-039). */
public final class LoyaltyLedgerDtos {

    private LoyaltyLedgerDtos() {
    }

    public record LoyaltyBalanceResponse(
        String customerId,
        long totalPoints,
        Instant nextExpiryAt,
        long nextExpiryPoints,
        LoyaltyTier tier
    ) {
    }

    public record LedgerEntryResponse(
        String id,
        String customerId,
        LoyaltyMovementType kind,
        long points,
        Instant occurredAt,
        Instant expiresAt,
        String referenceId,
        String description
    ) {
    }

    /** Eligible spend in minor currency units (reference AC-039-1). */
    public record LoyaltyEarnRequest(
        @PositiveOrZero long amount,
        @NotNull @Size(min = 3, max = 3) String currency,
        @Size(max = 26) String sourceTransactionId,
        List<String> categoryIds
    ) {
    }

    public record LoyaltyRedeemRequest(
        @Min(1) int points,
        @Size(max = 26) String sourceTransactionId
    ) {
    }

    public record LoyaltyRedeemResponse(
        String customerId,
        int pointsRedeemed,
        long discountAmount,
        String currency,
        LoyaltyBalanceResponse balance
    ) {
    }

    /** FR-208 — reverse points earned on a (now refunded) sale. */
    public record LoyaltyReverseRequest(
        @NotNull @Size(min = 1, max = 26) String sourceTransactionId
    ) {
    }
}
