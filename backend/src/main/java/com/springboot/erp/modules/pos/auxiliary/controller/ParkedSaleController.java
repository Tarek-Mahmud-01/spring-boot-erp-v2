package com.springboot.erp.modules.pos.auxiliary.controller;

import com.springboot.erp.modules.pos.auxiliary.domain.PosParkedSale;
import com.springboot.erp.modules.pos.auxiliary.dto.ParkedSaleDtos.ParkSaleRequest;
import com.springboot.erp.modules.pos.auxiliary.dto.ParkedSaleDtos.ParkedSaleResponse;
import com.springboot.erp.modules.pos.auxiliary.service.ParkedSaleService;
import com.springboot.erp.platform.web.PageResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PosParkedSale endpoints (US-035 FR-182..186) — thin controller, {@code @PreAuthorize} per
 * method. Since the OPEN cart itself lives in the (not-yet-ported) transactions sub-slice, the
 * park/resume endpoints here take the transaction's already-known snapshot as query parameters
 * rather than re-deriving it — the transactions slice's controller is expected to call through to
 * these once it exists; until then this is the addressable park-code surface.
 */
@RestController
@RequestMapping("/api/pos/parked-sales")
public class ParkedSaleController {

    private final ParkedSaleService service;

    public ParkedSaleController(ParkedSaleService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('pos.parked_sale.read')")
    public PageResponse<ParkedSaleResponse> list(
            @RequestParam(required = false) String locationId,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<PosParkedSale> page = service.listActive(locationId, pageable);
        return PageResponse.of(page, p -> service.toResponse(p, 0, 0, ""));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pos.parked_sale.read')")
    public ParkedSaleResponse get(@PathVariable String publicId) {
        PosParkedSale parked = service.get(publicId);
        return service.toResponse(parked, 0, 0, "");
    }

    /** US-035 FR-182 — park an OPEN cart under a new short code. */
    @PostMapping
    @PreAuthorize("hasAuthority('pos.parked_sale.write')")
    public ParkedSaleResponse park(
            @RequestParam @NotBlank String transactionId,
            @RequestParam @NotBlank String registerId,
            @RequestParam @NotBlank String locationId,
            @RequestParam(defaultValue = "0") int lineCount,
            @RequestParam(defaultValue = "0") @PositiveOrZero long totalAmount,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestBody(required = false) ParkSaleRequest request) {
        return service.park(transactionId, registerId, locationId, lineCount, totalAmount, currency, request);
    }

    /** US-035 FR-184/185 — resume a parked cart by its short code. */
    @PostMapping("/{parkCode}/resume")
    @PreAuthorize("hasAuthority('pos.parked_sale.write')")
    public ParkedSaleResponse resume(@PathVariable String parkCode) {
        PosParkedSale resumed = service.resume(parkCode);
        return service.toResponse(resumed, 0, 0, "");
    }
}
