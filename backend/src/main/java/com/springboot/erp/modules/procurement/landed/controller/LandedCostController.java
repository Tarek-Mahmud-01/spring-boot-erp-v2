package com.springboot.erp.modules.procurement.landed.controller;

import com.springboot.erp.modules.procurement.landed.dto.LandedCostDtos.LandedCostCreateRequest;
import com.springboot.erp.modules.procurement.landed.dto.LandedCostDtos.LandedCostInvoiceResponse;
import com.springboot.erp.modules.procurement.landed.dto.LandedCostDtos.LandedCostResponse;
import com.springboot.erp.modules.procurement.landed.dto.LandedCostDtos.LandedCostUpdateRequest;
import com.springboot.erp.modules.procurement.landed.service.LandedCostCommandService;
import com.springboot.erp.modules.procurement.landed.service.LandedCostQueryService;
import com.springboot.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENT-032 LandedCost endpoints — thin controller, {@code @PreAuthorize} per method. Business rules
 * live in the command / query services. The {@code apply} sub-action capitalises the cost into stock
 * (emits the revaluation outbox event).
 */
@RestController
@RequestMapping("/api/procurement/landed-costs")
public class LandedCostController {

    private final LandedCostCommandService command;
    private final LandedCostQueryService query;

    public LandedCostController(LandedCostCommandService command, LandedCostQueryService query) {
        this.command = command;
        this.query = query;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('procurement.landed_cost.read')")
    public PageResponse<LandedCostResponse> list(
            @RequestParam(required = false) String grnId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.list(grnId, search, pageable);
    }

    /** Aggregated one-row-per-invoice list scoped to a GRN (index / GRN detail tab). */
    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('procurement.landed_cost.read')")
    public List<LandedCostInvoiceResponse> invoicesForGrn(@RequestParam String grnId) {
        return query.listInvoicesForGrn(grnId);
    }

    @GetMapping("/by-invoice/{invoiceNumber}")
    @PreAuthorize("hasAuthority('procurement.landed_cost.read')")
    public List<LandedCostResponse> byInvoice(@PathVariable String invoiceNumber) {
        return query.byInvoice(invoiceNumber);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.landed_cost.read')")
    public LandedCostResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('procurement.landed_cost.write')")
    public LandedCostResponse create(@Valid @RequestBody LandedCostCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('procurement.landed_cost.write')")
    public LandedCostResponse update(@PathVariable String publicId,
                                     @Valid @RequestBody LandedCostUpdateRequest request) {
        return command.update(publicId, request);
    }

    /** FR-108 — apply (capitalise) the landed cost: revalues stock via an outbox event. */
    @PostMapping("/{publicId}/apply")
    @PreAuthorize("hasAuthority('procurement.landed_cost.write')")
    public LandedCostResponse apply(@PathVariable String publicId) {
        return command.apply(publicId);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('procurement.landed_cost.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }
}
