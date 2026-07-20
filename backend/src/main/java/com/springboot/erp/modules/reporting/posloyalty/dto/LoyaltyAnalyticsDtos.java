package com.springboot.erp.modules.reporting.posloyalty.dto;

import com.springboot.erp.platform.web.PageResponse;
import java.time.Instant;

/**
 * Wire DTOs for the loyalty-analytics half of the REPORTING "pos-loyalty" sub-slice (reference
 * {@code app.reports.schemas.loyalty_analytics}: RPT-028 Loyalty Liability &amp; Breakage, RPT-029
 * Points Expiry Forecast, RPT-030 Tier Distribution &amp; Migration, RPT-031 Customer RFM/LTV).
 * Records only (ARCHITECTURE.md §2). These four are inherently procedural (FIFO earn-lot replay,
 * population quintile RFM scoring, tier-migration comparison) — filtering is pushed to the
 * repositories, the roll-up/scoring runs once in the service layer, mirroring the reference.
 */
public final class LoyaltyAnalyticsDtos {

    private LoyaltyAnalyticsDtos() {
    }

    // -----------------------------------------------------------------
    // RPT-028 — Loyalty Liability & Breakage
    // -----------------------------------------------------------------

    public record LoyaltyLiabilityRow(
        String customerId,
        String customerName,
        String membershipId,
        String tierId,
        String tierName,
        long pointsBalance,
        long pointsEarned,
        long pointsRedeemed,
        long pointsExpired,
        long pointsReversed,
        long liabilityMinor,
        String currency,
        Instant lastActivityAt
    ) {
    }

    public record LoyaltyLiabilitySummary(
        String companyId,
        long members,
        long totalPointsBalance,
        long totalLiabilityMinor,
        long totalEarned,
        long totalExpired,
        double breakagePct,
        String currency
    ) {
    }

    public record LoyaltyLiabilityResponse(
        PageResponse<LoyaltyLiabilityRow> page,
        LoyaltyLiabilitySummary summary
    ) {
    }

    // -----------------------------------------------------------------
    // RPT-029 — Points Expiry Forecast (FIFO lot replay)
    // -----------------------------------------------------------------

    public record PointsExpiryRow(
        String customerId,
        String customerName,
        String membershipId,
        String tierId,
        String tierName,
        long pointsBalance,
        long expiring30,
        long expiring60,
        long expiring90,
        long overdue,
        java.time.LocalDate nextExpiryAt,
        long nextExpiryPoints,
        boolean neverExpires
    ) {
    }

    public record PointsExpirySummary(
        String companyId,
        long members,
        long totalPointsBalance,
        long expiring30,
        long expiring60,
        long expiring90,
        long overdue
    ) {
    }

    public record PointsExpiryResponse(PageResponse<PointsExpiryRow> page, PointsExpirySummary summary) {
    }

    // -----------------------------------------------------------------
    // RPT-030 — Tier Distribution & Migration
    // -----------------------------------------------------------------

    public record TierDistributionRow(
        String tierId,
        String tierCode,
        String tierName,
        long minSpendMinor,
        double earnMultiplier,
        String currency,
        long memberCount,
        long dueUpgrade,
        long dueDowngrade,
        long total12mSpendMinor,
        long avg12mSpendMinor
    ) {
    }

    public record TierDistributionSummary(
        String companyId,
        long totalMembers,
        long dueUpgrade,
        long dueDowngrade,
        long total12mSpendMinor,
        String currency
    ) {
    }

    public record TierDistributionResponse(
        PageResponse<TierDistributionRow> page,
        TierDistributionSummary summary
    ) {
    }

    // -----------------------------------------------------------------
    // RPT-031 — Customer RFM / Lifetime-Value Cohorts
    // -----------------------------------------------------------------

    public record CustomerRfmRow(
        String customerId,
        String customerName,
        String membershipId,
        String tierId,
        String tierName,
        long recencyDays,
        long frequency,
        long monetaryMinor,
        String currency,
        int rScore,
        int fScore,
        int mScore,
        String rfmSegment,
        Instant firstPurchaseAt,
        Instant lastPurchaseAt
    ) {
    }

    public record CustomerRfmSummary(
        String companyId,
        long scoredCustomers,
        long totalMonetaryMinor,
        long champions,
        double avgRecencyDays,
        String currency
    ) {
    }

    public record CustomerRfmResponse(PageResponse<CustomerRfmRow> page, CustomerRfmSummary summary) {
    }
}
