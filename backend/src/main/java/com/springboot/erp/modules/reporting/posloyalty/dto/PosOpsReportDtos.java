package com.springboot.erp.modules.reporting.posloyalty.dto;

import com.springboot.erp.platform.web.PageResponse;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Wire DTOs for the POS-operations half of the REPORTING "pos-loyalty" sub-slice (reference
 * {@code app.reports.schemas.pos_ops}: RPT-017 Discount Usage, RPT-018 POS Daily Sales Summary,
 * RPT-019 Till Variance, RPT-AU-004 Age Verification Refusals, RPT-032 New vs Returning Customers,
 * RPT-033 Suspended/Abandoned Carts). Records only (ARCHITECTURE.md §2). Money is always a plain
 * {@code long} minor-units amount paired with a sibling {@code currency} string, matching every
 * other ported reporting slice (dashboard / finance) — never a nested Money entity on the wire.
 */
public final class PosOpsReportDtos {

    private PosOpsReportDtos() {
    }

    // -----------------------------------------------------------------
    // RPT-017 — Discount Usage
    // -----------------------------------------------------------------

    public record DiscountRow(
        String txnId,
        String receiptNumber,
        Instant occurredAt,
        String cashierId,
        String cashierName,
        String locationId,
        String kind,
        long amountMinor,
        String reason,
        String managerApprovalName,
        String customerName,
        String currency
    ) {
    }

    public record DiscountSummary(
        String companyId,
        long count,
        long totalDiscountMinor,
        String currency
    ) {
    }

    // -----------------------------------------------------------------
    // RPT-018 — POS Daily Sales Summary
    // -----------------------------------------------------------------

    public record DailySalesRow(
        LocalDate date,
        String locationId,
        long receiptCount,
        long refundCount,
        long grossMinor,
        long refundMinor,
        long taxMinor,
        long cashMinor,
        long cardMinor,
        long storeCreditMinor,
        long otherMinor,
        long netMinor,
        String currency
    ) {
    }

    public record DailySalesSummary(
        String companyId,
        long days,
        long totalGrossMinor,
        long totalRefundMinor,
        long totalTaxMinor,
        long totalNetMinor,
        String currency
    ) {
    }

    // -----------------------------------------------------------------
    // RPT-019 — Till Variance
    // -----------------------------------------------------------------

    public record TillVarianceRow(
        String id,
        String status,
        long openingFloatMinor,
        Long expectedCashMinor,
        Long countedCashMinor,
        Long varianceMinor,
        String currency,
        Instant openedAt,
        Instant closedAt,
        String locationId,
        String cashierId,
        String cashierName,
        Double variancePct,
        String approvedByName,
        Instant approvedAt
    ) {
    }

    public record TillVarianceSummary(
        String companyId,
        long sessions,
        long totalVarianceMinor,
        String currency
    ) {
    }

    // -----------------------------------------------------------------
    // RPT-AU-004 — Age Verification Refusals
    // -----------------------------------------------------------------

    public record AgeRefusalRow(
        String id,
        Instant occurredAt,
        String actorId,
        String actorName,
        String productId,
        String sku,
        String reason,
        String idType,
        String locationId
    ) {
    }

    public record AgeRefusalSummary(
        String companyId,
        long refusals
    ) {
    }

    // -----------------------------------------------------------------
    // RPT-032 — New vs Returning Customers
    // -----------------------------------------------------------------

    public record NewVsReturningRow(
        LocalDate date,
        long newCustomers,
        long returningCustomers,
        long anonymousReceipts,
        long newRevenueMinor,
        long returningRevenueMinor,
        long anonymousRevenueMinor,
        String currency
    ) {
    }

    public record NewVsReturningSummary(
        String companyId,
        long days,
        long newCustomers,
        long returningCustomers,
        long anonymousReceipts,
        long newRevenueMinor,
        long returningRevenueMinor,
        long anonymousRevenueMinor,
        String currency
    ) {
    }

    // -----------------------------------------------------------------
    // RPT-033 — Suspended / Abandoned Carts
    // -----------------------------------------------------------------

    public record AbandonedCartRow(
        String id,
        String code,
        Instant parkedAt,
        String parkedBy,
        Instant expiresAt,
        long lineCount,
        long totalMinor,
        String currency,
        String status,
        long ageHours
    ) {
    }

    public record AbandonedCartSummary(
        String companyId,
        long carts,
        long active,
        long expired,
        long valueMinor,
        String currency
    ) {
    }

    // -----------------------------------------------------------------
    // Per-report response envelopes — pair a PageResponse of rows with a
    // sibling summary computed over the FULL filtered set (not just the
    // current page), mirroring the reference's ReportData(summary, rows, total).
    // -----------------------------------------------------------------

    public record DiscountResponse(PageResponse<DiscountRow> page, DiscountSummary summary) {
    }

    public record DailySalesResponse(PageResponse<DailySalesRow> page, DailySalesSummary summary) {
    }

    public record TillVarianceResponse(PageResponse<TillVarianceRow> page, TillVarianceSummary summary) {
    }

    public record AgeRefusalResponse(PageResponse<AgeRefusalRow> page, AgeRefusalSummary summary) {
    }

    public record NewVsReturningResponse(PageResponse<NewVsReturningRow> page, NewVsReturningSummary summary) {
    }

    public record AbandonedCartResponse(PageResponse<AbandonedCartRow> page, AbandonedCartSummary summary) {
    }
}
