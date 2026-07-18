package com.guru.erp.modules.product.promotions.controller;

import com.guru.erp.modules.product.promotions.dto.LifecycleDtos.LifecycleTransitionRequest;
import com.guru.erp.modules.product.promotions.dto.LifecycleDtos.LifecycleTransitionResponse;
import com.guru.erp.modules.product.promotions.service.LifecycleService;
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
 * US-015 / FR-070..074 product lifecycle endpoints (thin controller — rules in
 * {@link LifecycleService}). The transition ledger is read per product and
 * appended via the special {@code POST /transitions} endpoint.
 */
@RestController
@RequestMapping("/api/product-lifecycle")
public class LifecycleController {

    private final LifecycleService service;

    public LifecycleController(LifecycleService service) {
        this.service = service;
    }

    /** FR-074 — read the append-only transition ledger for one product. */
    @GetMapping("/{productId}/transitions")
    @PreAuthorize("hasAuthority('product.lifecycle.read')")
    public PageResponse<LifecycleTransitionResponse> list(
            @PathVariable String productId,
            @PageableDefault(size = 100) Pageable pageable) {
        return service.list(productId, pageable);
    }

    /** FR-070 — validate and record a lifecycle transition. */
    @PostMapping("/transitions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product.lifecycle.write')")
    public LifecycleTransitionResponse transition(@Valid @RequestBody LifecycleTransitionRequest request) {
        return service.transition(request);
    }
}
