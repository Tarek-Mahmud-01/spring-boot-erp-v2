package com.springboot.erp.modules.reporting.invproc.controller;

import com.springboot.erp.modules.inventory.movements.domain.TransferStatus;
import com.springboot.erp.modules.inventory.stock.domain.MovementType;
import com.springboot.erp.modules.reporting.invproc.dto.InvProcReportDtos.ProductLedgerResponse;
import com.springboot.erp.modules.reporting.invproc.dto.InvProcReportDtos.StockSummaryResponse;
import com.springboot.erp.modules.reporting.invproc.dto.InvProcReportDtos.TransferRegisterResponse;
import com.springboot.erp.modules.reporting.invproc.service.ProductLedgerQueryService;
import com.springboot.erp.modules.reporting.invproc.service.StockSummaryQueryService;
import com.springboot.erp.modules.reporting.invproc.service.TransferRegisterQueryService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
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
 * Inventory reports (reference {@code app.reports.views.inventory}) — Stock Summary, Product
 * Ledger, Transfer Register. Thin: every endpoint parses its query params, delegates the
 * join/aggregation to a QueryDSL-backed query service, and wraps the (page, summary) result pair
 * into one response record. Gated by {@code reporting.inventory.read}.
 */
@RestController
@RequestMapping("/api/reports/inventory")
public class InventoryReportController {

    private final StockSummaryQueryService stockSummaryService;
    private final ProductLedgerQueryService productLedgerService;
    private final TransferRegisterQueryService transferRegisterService;

    public InventoryReportController(StockSummaryQueryService stockSummaryService,
                                      ProductLedgerQueryService productLedgerService,
                                      TransferRegisterQueryService transferRegisterService) {
        this.stockSummaryService = stockSummaryService;
        this.productLedgerService = productLedgerService;
        this.transferRegisterService = transferRegisterService;
    }

    /** RPT-E005-STOCK-SUM — on-hand qty + value per (product, variant, location), all movement types. */
    @GetMapping("/stock-summary")
    @PreAuthorize("hasAuthority('reporting.inventory.read')")
    public StockSummaryResponse stockSummary(
        @RequestParam(required = false) String locationId,
        @RequestParam(required = false) String productId,
        @RequestParam(required = false) String variantId,
        @RequestParam(defaultValue = "false") boolean onlyInStock,
        @RequestParam(required = false) LocalDate asOfDate,
        @RequestParam(required = false) LocalDate inFrom,
        @RequestParam(required = false) LocalDate inTo,
        @RequestParam(defaultValue = "USD") String currency,
        @PageableDefault(size = 50) Pageable pageable
    ) {
        var result = stockSummaryService.run(locationId, productId, variantId, onlyInStock, asOfDate, inFrom, inTo, currency, pageable);
        return new StockSummaryResponse(pageOf(result.rows(), result.total(), pageable), result.summary());
    }

    /** RPT-E005-PROD-LEDGER — chronological stock_ledger rows for one product with running qty. */
    @GetMapping("/product-ledger")
    @PreAuthorize("hasAuthority('reporting.inventory.read')")
    public ProductLedgerResponse productLedger(
        @RequestParam String productId,
        @RequestParam(required = false) String variantId,
        @RequestParam(required = false) String locationId,
        @RequestParam(required = false) LocalDate fromDate,
        @RequestParam(required = false) LocalDate toDate,
        @RequestParam(required = false) String movementType,
        @RequestParam(defaultValue = "USD") String currency,
        @PageableDefault(size = 50) Pageable pageable
    ) {
        var result = productLedgerService.run(productId, variantId, locationId, fromDate, toDate,
            parseMovementType(movementType), currency, pageable);
        return new ProductLedgerResponse(pageOf(result.rows(), result.total(), pageable), result.summary());
    }

    /** RPT-E005-TRANSFER — variant-aware stock transfer register with backend-owned status tone. */
    @GetMapping("/transfer-register")
    @PreAuthorize("hasAuthority('reporting.inventory.read')")
    public TransferRegisterResponse transferRegister(
        @RequestParam(required = false) String fromLocationId,
        @RequestParam(required = false) String toLocationId,
        @RequestParam(required = false) String productId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) LocalDate fromDate,
        @RequestParam(required = false) LocalDate toDate,
        @PageableDefault(size = 50) Pageable pageable
    ) {
        TransferStatus st = status == null || status.isBlank() ? null : TransferStatus.fromWire(status);
        var result = transferRegisterService.run(fromLocationId, toLocationId, productId, st, fromDate, toDate, pageable);
        return new TransferRegisterResponse(pageOf(result.rows(), result.total(), pageable), result.summary());
    }

    private static <R> PageResponse<R> pageOf(java.util.List<R> rows, long total, Pageable pageable) {
        return PageResponse.of(new PageImpl<>(rows, pageable, total));
    }

    private static MovementType parseMovementType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return MovementType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED, "Unknown movementType: " + raw);
        }
    }
}
