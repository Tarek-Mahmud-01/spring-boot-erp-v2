package com.guru.erp.modules.reporting.invproc.dto;

import com.guru.erp.platform.web.PageResponse;
import java.time.Instant;

/**
 * Wire DTOs for the "inv-proc" reporting sub-slice — Inventory (reference
 * {@code app.reports.schemas.inventory}) and Procurement (reference
 * {@code app.reports.schemas.procurement}) reports. Records only (ARCHITECTURE.md §2).
 * Every money field is a plain {@code long} minor-units amount paired with a sibling
 * {@code currency} string — never a nested Money entity on the wire, matching every other
 * ported reporting slice (see {@code reporting.finance.dto.FinanceReportDtos}).
 *
 * <p>Every list-style report (Stock Summary, Product Ledger, Transfer Register, Supplier Summary,
 * PO/GRN/Bill Aging, Supplier Spend) returns ONE {@code XxxResponse(PageResponse<Row> page, Summary
 * summary)} wrapper per call — mirrors {@code reporting.posloyalty.dto.PosOpsReportDtos}'s
 * {@code TillVarianceResponse} / {@code DiscountResponse} shape — so a single query service call
 * computes the page and the full-filtered-set rollup together instead of two round trips. This
 * slice has no single-shape (non-paginated) report.
 */
public final class InvProcReportDtos {

    private InvProcReportDtos() {
    }

    // -----------------------------------------------------------------
    // Stock Summary (RPT-E005-STOCK-SUM) — on-hand qty + value per
    // (product, variant, location), rolled up across EVERY movement type
    // (RECEIPT / SALE / RETURN / TRANSFER_IN / TRANSFER_OUT / ADJUSTMENT /
    // WRITE_OFF / RESERVE) via a single SUM(qty_signed) GROUP BY.
    // -----------------------------------------------------------------

    public record StockSummaryRow(
        String productId,
        String variantId,
        String locationId,
        double onHandQty,
        double openingQty,
        double inQty,
        double outQty,
        long avgUnitCost,
        long totalValue,
        String currency
    ) {
    }

    public record StockSummarySummary(
        String currency,
        int productCount,
        double totalQty,
        double openingQty,
        double inQty,
        double outQty,
        long totalValue
    ) {
    }

    public record StockSummaryResponse(PageResponse<StockSummaryRow> page, StockSummarySummary summary) {
    }

    // -----------------------------------------------------------------
    // Product Ledger (RPT-E005-PROD-LEDGER) — chronological stock_ledger
    // rows for one product with a running qty. IN_TRANSIT staging rows
    // (the internal TRANSFER_IN parking pair) are hidden unless the caller
    // explicitly filters to that movement type.
    // -----------------------------------------------------------------

    public record ProductLedgerRow(
        String ledgerId,
        Instant occurredAt,
        String movementType,
        String locationId,
        String variantId,
        double qtySigned,
        long unitCostAmount,
        String unitCostCurrency,
        String sourceDocType,
        String sourceDocId,
        double runningQty,
        String notes
    ) {
    }

    public record ProductLedgerSummary(
        String productId,
        String currency,
        double openingQty,
        double periodInQty,
        double periodOutQty,
        double closingQty,
        long closingValue,
        long lineCount
    ) {
    }

    public record ProductLedgerResponse(PageResponse<ProductLedgerRow> page, ProductLedgerSummary summary) {
    }

    // -----------------------------------------------------------------
    // Transfer Register (RPT-E005-TRANSFER) — variant-aware stock
    // transfer lines with a backend-owned status + StatusPill tone.
    // -----------------------------------------------------------------

    public record TransferLineRow(
        Instant date,
        String transferNumber,
        String status,
        String displayStatusTone,
        String fromLocationId,
        String toLocationId,
        String productId,
        String variantId,
        double qtySent,
        double qtyReceived
    ) {
    }

    public record TransferSummary(
        long totalRows,
        double totalQtySent,
        double totalQtyReceived
    ) {
    }

    public record TransferRegisterResponse(PageResponse<TransferLineRow> page, TransferSummary summary) {
    }

    // -----------------------------------------------------------------
    // Supplier Summary (RPT-E004-SUP-SUMMARY) — aggregated AP payable
    // per supplier: Σ(credit-debit) over posted JournalLines tagged
    // holderType=SUPPLIER, plus the supplier's opening balance.
    // -----------------------------------------------------------------

    public record SupplierSummaryRow(
        String supplierId,
        String supplierCode,
        String supplierName,
        long totalDebit,
        long totalCredit,
        long balance,
        String currency
    ) {
    }

    public record SupplierSummarySummary(
        String currency,
        int supplierCount,
        long totalDebit,
        long totalCredit,
        long netPayable
    ) {
    }

    public record SupplierSummaryResponse(PageResponse<SupplierSummaryRow> page, SupplierSummarySummary summary) {
    }

    // -----------------------------------------------------------------
    // PO / GRN / Bill Aging (RPT-E004-PO-AGING) — one row per Purchase
    // Order with its latest linked GoodsReceipt and SupplierBill status,
    // plus a due-date aging bucket computed from the bill due date.
    // -----------------------------------------------------------------

    public record PoAgingRow(
        String poId,
        String poNumber,
        String poStatus,
        String supplierId,
        Instant poDate,
        String grnId,
        String grnNumber,
        String grnStatus,
        Instant receivedAt,
        String billId,
        String billNumber,
        String billStatus,
        Instant billDueDate,
        long billTotalAmount,
        String currency,
        long daysOverdue,
        String agingBucket
    ) {
    }

    public record PoAgingSummary(
        String currency,
        long totalRows,
        long totalOutstanding,
        long current,
        long overdue1To30,
        long overdue31To60,
        long overdue61To90,
        long overdue90Plus
    ) {
    }

    public record PoAgingResponse(PageResponse<PoAgingRow> page, PoAgingSummary summary) {
    }

    // -----------------------------------------------------------------
    // Supplier Spend (RPT-E004-SUPPLIER-SPEND) — Σ purchase order line
    // value per supplier over an optional [fromDate, toDate] window.
    // -----------------------------------------------------------------

    public record SupplierSpendRow(
        String supplierId,
        String supplierCode,
        String supplierName,
        long poCount,
        long totalOrderedValue,
        String currency
    ) {
    }

    public record SupplierSpendSummary(
        String currency,
        int supplierCount,
        long totalSpend
    ) {
    }

    public record SupplierSpendResponse(PageResponse<SupplierSpendRow> page, SupplierSpendSummary summary) {
    }
}
