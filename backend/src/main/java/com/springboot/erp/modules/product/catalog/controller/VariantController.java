package com.springboot.erp.modules.product.catalog.controller;

import com.springboot.erp.modules.product.catalog.dto.VariantDtos.VariantResponse;
import com.springboot.erp.modules.product.catalog.dto.VariantDtos.VariantUpdateRequest;
import com.springboot.erp.modules.product.catalog.dto.VariantDtos.VariantsCreateRequest;
import com.springboot.erp.modules.product.catalog.service.VariantService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ENT-011a ProductVariant endpoints (FR-047), nested under a product. */
@RestController
@RequestMapping("/api/products/{productId}/variants")
public class VariantController {

    private final VariantService service;

    public VariantController(VariantService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('product.variant.read')")
    public List<VariantResponse> list(@PathVariable String productId) {
        return service.list(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product.variant.write')")
    public List<VariantResponse> create(@PathVariable String productId,
                                        @Valid @RequestBody VariantsCreateRequest request) {
        return service.create(productId, request);
    }

    @PatchMapping("/{variantId}")
    @PreAuthorize("hasAuthority('product.variant.write')")
    public VariantResponse update(@PathVariable String productId, @PathVariable String variantId,
                                  @Valid @RequestBody VariantUpdateRequest request) {
        return service.update(productId, variantId, request);
    }

    @DeleteMapping("/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('product.variant.write')")
    public void delete(@PathVariable String productId, @PathVariable String variantId) {
        service.delete(productId, variantId);
    }
}
