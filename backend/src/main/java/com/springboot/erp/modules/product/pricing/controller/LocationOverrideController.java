package com.springboot.erp.modules.product.pricing.controller;

import com.springboot.erp.modules.product.pricing.dto.PricingDtos.LocationOverrideResponse;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.LocationOverrideUpsertRequest;
import com.springboot.erp.modules.product.pricing.service.PricingCommandService;
import com.springboot.erp.modules.product.pricing.service.PricingQueryService;
import com.springboot.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-063 per-location price overrides (US-013). The upsert endpoint inserts or
 * replaces the live override for a (product, location, variant) triple; list is
 * scoped to a product; delete is a soft-delete. Thin controller under the
 * 150-line cap.
 */
@RestController
@RequestMapping("/api/product/location-overrides")
public class LocationOverrideController {

    private final PricingQueryService query;
    private final PricingCommandService command;

    public LocationOverrideController(PricingQueryService query, PricingCommandService command) {
        this.query = query;
        this.command = command;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('product.locationoverride.read')")
    public PageResponse<LocationOverrideResponse> list(
            @RequestParam String productId,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.listOverrides(productId, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('product.locationoverride.read')")
    public LocationOverrideResponse get(@PathVariable String publicId) {
        return query.getOverride(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product.locationoverride.write')")
    public LocationOverrideResponse upsert(@Valid @RequestBody LocationOverrideUpsertRequest request) {
        return command.upsertOverride(request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('product.locationoverride.write')")
    public void delete(@PathVariable String publicId) {
        command.deleteOverride(publicId);
    }
}
