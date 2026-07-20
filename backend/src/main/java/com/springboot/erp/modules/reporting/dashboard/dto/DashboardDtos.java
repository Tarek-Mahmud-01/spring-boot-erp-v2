package com.springboot.erp.modules.reporting.dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * DTOs for the REPORTING "dashboard" sub-slice (reference {@code app.reports.schemas.dashboard},
 * {@code app.reports.schemas.sales}, {@code app.reports.schemas.pos_txn}).
 *
 * <p>Money is always {@code long} minor units + an ISO-4217 currency code (never {@code double} /
 * {@code BigDecimal}-then-rounded). The executive dashboard summary is a single-object report (not
 * a list), so it is returned as a plain record rather than {@code PageResponse}, per the platform
 * convention.
 */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    // ------------------------------------------------------------------
    // Executive dashboard summary (reference: repositories/dashboard.py,
    // scoped to what this codebase has actually ported: pos.transactions +
    // finance.gl + product.catalog. Procurement/CRM/audit rollups from the
    // reference are intentionally NOT reproduced — those slices are not yet
    // ported, and inventing numbers for them would mislead the KPI tiles).
    // ------------------------------------------------------------------

    /** One period bucket ("today" / "week" / "month") of the KPI tile block. */
    public record PeriodBlock(
        long revenueMinor,
        double revenueDeltaPct,
        int txnCount,
        int txnDeltaPct,
        long avgBasketMinor,
        String topProductName,
        long topProductRevenueMinor,
        List<SeriesPoint> series,
        String currency
    ) {
    }

    public record SeriesPoint(LocalDate date, long valueMinor) {
    }

    public record TopProductRow(
        String productId,
        String sku,
        String name,
        double qtySold,
        long revenueMinor,
        String currency
    ) {
    }

    public record DashboardSummaryResponse(
        String currency,
        Instant generatedAt,
        PeriodBlock today,
        PeriodBlock week,
        PeriodBlock month,
        List<TopProductRow> topProducts,
        long unpostedJournalCount,
        long productCount
    ) {
    }

    // ------------------------------------------------------------------
    // RPT-015 Sales by Product (reference: repositories/sales.py::sales_by_product)
    // ------------------------------------------------------------------

    public record SalesByProductRow(
        String productId,
        String sku,
        String name,
        double qtySold,
        long grossMinor,
        long taxMinor,
        long netMinor,
        String currency,
        long receiptCount,
        Instant lastSoldAt
    ) {
    }

    public record SalesByProductSummary(
        long productCount,
        double totalQty,
        long totalGrossMinor,
        long totalTaxMinor,
        long totalNetMinor,
        String currency
    ) {
    }

    // ------------------------------------------------------------------
    // RPT-AU-TXN POS transaction register (reference: repositories/pos_txn.py)
    // ------------------------------------------------------------------

    public record TxnLineRow(
        String id,
        String productId,
        String sku,
        String name,
        double qty,
        long unitPriceMinor,
        long discountMinor,
        long taxMinor,
        long lineNetMinor,
        String currency
    ) {
    }

    public record TransactionRow(
        String id,
        String receiptNumber,
        String type,
        String status,
        String locationId,
        String registerId,
        String cashierId,
        String customerId,
        List<TxnLineRow> lines,
        long subtotalMinor,
        long taxMinor,
        long totalMinor,
        String currency,
        Instant occurredAt
    ) {
    }

    public record TransactionsSummary(
        long count,
        String currency,
        boolean truncated
    ) {
    }
}
