package com.springboot.erp.modules.reporting.dashboard.controller;

import com.springboot.erp.modules.reporting.dashboard.dto.DashboardDtos.DashboardSummaryResponse;
import com.springboot.erp.modules.reporting.dashboard.dto.DashboardDtos.SalesByProductRow;
import com.springboot.erp.modules.reporting.dashboard.dto.DashboardDtos.SalesByProductSummary;
import com.springboot.erp.modules.reporting.dashboard.dto.DashboardDtos.TransactionRow;
import com.springboot.erp.modules.reporting.dashboard.dto.DashboardDtos.TransactionsSummary;
import com.springboot.erp.modules.reporting.dashboard.service.DashboardQueryService;
import com.springboot.erp.modules.reporting.dashboard.service.SalesReportQueryService;
import com.springboot.erp.platform.web.PageResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REPORTING "dashboard" sub-slice — read-only, cross-cutting reporting layer (reference
 * {@code app.reports.views.dashboard} / {@code views.sales} / {@code views.pos_txn}). Thin
 * controller: every method just validates paging/params and delegates to a query service that
 * builds the QueryDSL aggregation. No mutations, no audit rows.
 */
@RestController
@RequestMapping("/api/reports")
public class DashboardReportController {

    private final DashboardQueryService dashboardQuery;
    private final SalesReportQueryService salesQuery;

    public DashboardReportController(DashboardQueryService dashboardQuery, SalesReportQueryService salesQuery) {
        this.dashboardQuery = dashboardQuery;
        this.salesQuery = salesQuery;
    }

    /** Executive dashboard summary — KPI tiles for today/week/month + top products (single object, not paged). */
    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasAuthority('reporting.dashboard.read')")
    public DashboardSummaryResponse dashboardSummary(@RequestParam(required = false) String locationId) {
        return dashboardQuery.summary(locationId);
    }

    /** RPT-015 — Sales by Product, paged, gross-descending. */
    @GetMapping("/sales/by-product")
    @PreAuthorize("hasAuthority('reporting.sales.read')")
    public PageResponse<SalesByProductRow> salesByProduct(
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @PageableDefault(size = 50) Pageable pageable) {
        return salesQuery.salesByProduct(locationId, fromDate, toDate, pageable);
    }

    /** Roll-up totals (product_count/qty/gross/tax/net) matching the {@code by-product} filter set. */
    @GetMapping("/sales/by-product/summary")
    @PreAuthorize("hasAuthority('reporting.sales.read')")
    public SalesByProductSummary salesByProductSummary(
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        return salesQuery.salesByProductSummary(locationId, fromDate, toDate);
    }

    /** RPT-AU-TXN — POS transaction register (header + lines), newest-first. */
    @GetMapping("/pos/transactions")
    @PreAuthorize("hasAuthority('reporting.pos_txn.read')")
    public PageResponse<TransactionRow> posTransactions(
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String registerId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @PageableDefault(size = 50) Pageable pageable) {
        return salesQuery.transactions(locationId, registerId, fromDate, toDate, pageable);
    }

    /** Summary block (count/currency/truncated) for the same POS transaction register filter set. */
    @GetMapping("/pos/transactions/summary")
    @PreAuthorize("hasAuthority('reporting.pos_txn.read')")
    public TransactionsSummary posTransactionsSummary(
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String registerId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @PageableDefault(size = 50) Pageable pageable) {
        return salesQuery.transactionsSummary(locationId, registerId, fromDate, toDate, pageable);
    }
}
