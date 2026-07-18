package com.guru.erp.modules.product.pricing.controller;

import com.guru.erp.modules.product.pricing.dto.PricingDtos.PriceHistoryResponse;
import com.guru.erp.modules.product.pricing.dto.PricingDtos.ScheduledPriceChangeRequest;
import com.guru.erp.modules.product.pricing.service.PricingCommandService;
import com.guru.erp.modules.product.pricing.service.PricingQueryService;
import com.guru.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-064 append-only price history + FR-062 scheduled base-price change (US-013).
 * History is read-only (a ledger); the special {@code POST /scheduled-change}
 * endpoint appends a future-dated sell-price row without mutating the base
 * product row. Under the 150-line cap.
 */
@RestController
@RequestMapping("/api/product/price-history")
public class PriceHistoryController {

    private final PricingQueryService query;
    private final PricingCommandService command;

    public PriceHistoryController(PricingQueryService query, PricingCommandService command) {
        this.query = query;
        this.command = command;
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAuthority('product.pricehistory.read')")
    public PageResponse<PriceHistoryResponse> list(
            @PathVariable String productId,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.listPriceHistory(productId, pageable);
    }

    @PostMapping("/scheduled-change")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product.pricehistory.write')")
    public PriceHistoryResponse scheduleChange(@Valid @RequestBody ScheduledPriceChangeRequest request) {
        return command.scheduleBasePriceChange(request);
    }
}
