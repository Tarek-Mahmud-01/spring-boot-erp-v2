package com.guru.erp.modules.product.catalog.controller;

import com.guru.erp.modules.product.catalog.dto.WeighedDtos.VariableMeasureResolveResponse;
import com.guru.erp.modules.product.catalog.service.ProductQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CR-001 (FR-284/FR-285) — stateless variable-measure barcode resolution. Decodes
 * a GS1 in-store price-embedded EAN-13 (prefix "21") and resolves it to the
 * weighed product by PLU, returning everything the POS needs to build a cart line.
 */
@RestController
@RequestMapping("/api/products/weighed")
public class WeighedController {

    private final ProductQueryService query;

    public WeighedController(ProductQueryService query) {
        this.query = query;
    }

    @GetMapping("/resolve-barcode")
    @PreAuthorize("hasAuthority('product.product.read')")
    public VariableMeasureResolveResponse resolve(@RequestParam String barcode) {
        return query.resolveVariableMeasure(barcode);
    }
}
