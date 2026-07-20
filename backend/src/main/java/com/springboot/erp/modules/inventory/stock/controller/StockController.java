package com.springboot.erp.modules.inventory.stock.controller;

import com.springboot.erp.modules.inventory.stock.domain.MovementType;
import com.springboot.erp.modules.inventory.stock.dto.StockAvailabilityDtos.StockCheckRequest;
import com.springboot.erp.modules.inventory.stock.dto.StockAvailabilityDtos.StockCheckResponse;
import com.springboot.erp.modules.inventory.stock.dto.StockLedgerDtos.LedgerEntryResponse;
import com.springboot.erp.modules.inventory.stock.dto.StockLedgerDtos.LedgerPostRequest;
import com.springboot.erp.modules.inventory.stock.dto.StockOnHandDtos.BatchSohRequest;
import com.springboot.erp.modules.inventory.stock.dto.StockOnHandDtos.ProductSohSummary;
import com.springboot.erp.modules.inventory.stock.dto.StockOnHandDtos.StockOnHandResponse;
import com.springboot.erp.modules.inventory.stock.service.StockPostingService;
import com.springboot.erp.modules.inventory.stock.service.StockQueryService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Core stock-engine endpoints (ARCHITECTURE.md §2 — thin controller,
 * {@code @PreAuthorize} on every method). Read-only projections over the ledger
 * (on-hand, availability, entries) plus the append-only movement post. All
 * business logic lives in the query / posting services; ULIDs only.
 */
@RestController
@RequestMapping("/api/inventory")
public class StockController {

    private final StockQueryService queryService;
    private final StockPostingService postingService;

    public StockController(StockQueryService queryService, StockPostingService postingService) {
        this.queryService = queryService;
        this.postingService = postingService;
    }

    /** FR-115 — on-hand buckets grouped by (product, variant, location, status). */
    @GetMapping("/stock")
    @PreAuthorize("hasAuthority('inventory.stock.read')")
    public List<StockOnHandResponse> stockOnHand(@RequestParam(required = false) String locationId,
                                                 @RequestParam(required = false) String productId,
                                                 @RequestParam(required = false) String variantId) {
        return queryService.stockOnHand(locationId, productId, variantId);
    }

    /** Total SOH across all locations for one product (optionally a single variant). */
    @GetMapping("/stock/product/{productId}")
    @PreAuthorize("hasAuthority('inventory.stock.read')")
    public ProductSohSummary productSummary(@PathVariable String productId,
                                            @RequestParam(required = false) String variantId) {
        return queryService.productSohSummary(productId, variantId);
    }

    /** Bulk SOH summary for many products in one request (PO/PR line display). */
    @PostMapping("/stock/summary")
    @PreAuthorize("hasAuthority('inventory.stock.read')")
    public List<ProductSohSummary> batchSummary(@Valid @RequestBody BatchSohRequest request) {
        return queryService.batchSohSummary(request);
    }

    /** Bulk availability check used by every "add line" dialog before submit. */
    @PostMapping("/stock/check")
    @PreAuthorize("hasAuthority('inventory.stock.read')")
    public StockCheckResponse checkAvailability(@Valid @RequestBody StockCheckRequest request) {
        return queryService.checkAvailability(request);
    }

    /** FR-117 — paginated, filtered, date-windowed ledger listing. */
    @GetMapping("/ledger")
    @PreAuthorize("hasAuthority('inventory.ledger.read')")
    public List<LedgerEntryResponse> listLedger(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String variantId,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) MovementType movementType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "50") int limit) {
        return queryService.listLedger(productId, variantId, locationId, movementType,
            fromDate, toDate, skip, limit);
    }

    /** FR-114 / FR-118 — append one movement row to the ledger. */
    @PostMapping("/ledger")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory.ledger.write')")
    public LedgerEntryResponse postMovement(@Valid @RequestBody LedgerPostRequest request) {
        return postingService.post(request);
    }
}
