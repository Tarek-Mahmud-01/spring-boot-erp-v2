package com.guru.erp.modules.inventory.stock.controller;

import com.guru.erp.modules.inventory.stock.dto.ValuationConfigDtos.ValuationConfigResponse;
import com.guru.erp.modules.inventory.stock.dto.ValuationConfigDtos.ValuationConfigSetRequest;
import com.guru.erp.modules.inventory.stock.service.ValuationConfigService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENT-041 InventoryValuationConfig endpoints (thin controller). GET the company's
 * config; PUT sets (or creates) the valuation method — rejected once the method
 * has locked. ULID company id only.
 */
@RestController
@RequestMapping("/api/inventory/valuation-config")
public class ValuationConfigController {

    private final ValuationConfigService service;

    public ValuationConfigController(ValuationConfigService service) {
        this.service = service;
    }

    @GetMapping("/{companyId}")
    @PreAuthorize("hasAuthority('inventory.valuation.read')")
    public ValuationConfigResponse get(@PathVariable String companyId) {
        return service.get(companyId);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('inventory.valuation.write')")
    public ValuationConfigResponse set(@Valid @RequestBody ValuationConfigSetRequest request) {
        return service.set(request);
    }
}
