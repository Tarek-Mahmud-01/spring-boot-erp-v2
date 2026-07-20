package com.springboot.erp.modules.reporting.invproc.controller;

import com.springboot.erp.modules.procurement.bills.domain.BillStatus;
import com.springboot.erp.modules.procurement.orders.domain.PoStatus;
import com.springboot.erp.modules.reporting.invproc.dto.InvProcReportDtos.PoAgingResponse;
import com.springboot.erp.modules.reporting.invproc.dto.InvProcReportDtos.SupplierSpendResponse;
import com.springboot.erp.modules.reporting.invproc.dto.InvProcReportDtos.SupplierSummaryResponse;
import com.springboot.erp.modules.reporting.invproc.service.PoAgingQueryService;
import com.springboot.erp.modules.reporting.invproc.service.SupplierSpendQueryService;
import com.springboot.erp.modules.reporting.invproc.service.SupplierSummaryQueryService;
import com.springboot.erp.platform.web.PageResponse;
import java.time.LocalDate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Procurement reports (reference {@code app.reports.views.procurement}) — Supplier Summary,
 * PO/GRN/Bill Aging, Supplier Spend. Thin: every endpoint parses its query params, delegates the
 * join/aggregation to a QueryDSL-backed query service, and wraps the (page, summary) result pair
 * into one response record. Gated by {@code reporting.procurement.read}.
 */
@RestController
@RequestMapping("/api/reports/procurement")
public class ProcurementReportController {

    private final SupplierSummaryQueryService supplierSummaryService;
    private final PoAgingQueryService poAgingService;
    private final SupplierSpendQueryService supplierSpendService;

    public ProcurementReportController(SupplierSummaryQueryService supplierSummaryService,
                                        PoAgingQueryService poAgingService,
                                        SupplierSpendQueryService supplierSpendService) {
        this.supplierSummaryService = supplierSummaryService;
        this.poAgingService = poAgingService;
        this.supplierSpendService = supplierSpendService;
    }

    /** RPT-E004-SUP-SUMMARY — aggregated AP payable per supplier. */
    @GetMapping("/supplier-summary")
    @PreAuthorize("hasAuthority('reporting.procurement.read')")
    public SupplierSummaryResponse supplierSummary(
        @RequestParam(required = false) LocalDate fromDate,
        @RequestParam(required = false) LocalDate toDate,
        @RequestParam(defaultValue = "false") boolean onlyOpen,
        @RequestParam(defaultValue = "USD") String currency,
        @PageableDefault(size = 50) Pageable pageable
    ) {
        var result = supplierSummaryService.run(fromDate, toDate, onlyOpen, currency, pageable);
        return new SupplierSummaryResponse(pageOf(result.rows(), result.total(), pageable), result.summary());
    }

    /** RPT-E004-PO-AGING — PO/GRN/Bill status + due-date aging bucket per Purchase Order. */
    @GetMapping("/po-aging")
    @PreAuthorize("hasAuthority('reporting.procurement.read')")
    public PoAgingResponse poAging(
        @RequestParam(required = false) String supplierId,
        @RequestParam(required = false) String poStatus,
        @RequestParam(required = false) String billStatus,
        @RequestParam(required = false) LocalDate fromDate,
        @RequestParam(required = false) LocalDate toDate,
        @RequestParam(defaultValue = "USD") String currency,
        @PageableDefault(size = 50) Pageable pageable
    ) {
        PoStatus ps = poStatus == null || poStatus.isBlank() ? null : PoStatus.fromWire(poStatus);
        BillStatus bs = billStatus == null || billStatus.isBlank() ? null : BillStatus.fromWire(billStatus);
        var result = poAgingService.run(supplierId, ps, bs, fromDate, toDate, currency, pageable);
        return new PoAgingResponse(pageOf(result.rows(), result.total(), pageable), result.summary());
    }

    /** RPT-E004-SUPPLIER-SPEND — Σ purchase order line value per supplier over an optional window. */
    @GetMapping("/supplier-spend")
    @PreAuthorize("hasAuthority('reporting.procurement.read')")
    public SupplierSpendResponse supplierSpend(
        @RequestParam(required = false) LocalDate fromDate,
        @RequestParam(required = false) LocalDate toDate,
        @RequestParam(defaultValue = "USD") String currency,
        @PageableDefault(size = 50) Pageable pageable
    ) {
        var result = supplierSpendService.run(fromDate, toDate, currency, pageable);
        return new SupplierSpendResponse(pageOf(result.rows(), result.total(), pageable), result.summary());
    }

    private static <R> PageResponse<R> pageOf(java.util.List<R> rows, long total, Pageable pageable) {
        return PageResponse.of(new PageImpl<>(rows, pageable, total));
    }
}
