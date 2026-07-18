package com.guru.erp.modules.product.catalog.controller;

import com.guru.erp.modules.product.catalog.dto.LifecycleDtos.LifecycleTransitionRequest;
import com.guru.erp.modules.product.catalog.dto.ProductDtos.ProductCreateRequest;
import com.guru.erp.modules.product.catalog.dto.ProductDtos.ProductResponse;
import com.guru.erp.modules.product.catalog.dto.ProductDtos.ProductUpdateRequest;
import com.guru.erp.modules.product.catalog.service.ProductCommandService;
import com.guru.erp.modules.product.catalog.service.ProductLifecycleService;
import com.guru.erp.modules.product.catalog.service.ProductQueryService;
import com.guru.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
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
 * ENT-011 Product endpoints — thin controller, {@code @PreAuthorize} per method.
 * Business rules live in the command/query services. Includes the FR-070
 * lifecycle transition as a thin special endpoint.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductCommandService command;
    private final ProductQueryService query;
    private final ProductLifecycleService lifecycle;

    public ProductController(ProductCommandService command, ProductQueryService query,
                             ProductLifecycleService lifecycle) {
        this.command = command;
        this.query = query;
        this.lifecycle = lifecycle;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('product.product.read')")
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String lifecycleState,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 50, sort = "name") Pageable pageable) {
        return query.list(q, lifecycleState, active, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('product.product.read')")
    public ProductResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product.product.write')")
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('product.product.write')")
    public ProductResponse update(@PathVariable String publicId,
                                  @Valid @RequestBody ProductUpdateRequest request) {
        return command.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('product.product.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    /** FR-070/074 — move the product through its lifecycle state machine. */
    @PostMapping("/{publicId}/lifecycle")
    @PreAuthorize("hasAuthority('product.product.write')")
    public ProductResponse transitionLifecycle(@PathVariable String publicId,
                                               @Valid @RequestBody LifecycleTransitionRequest request) {
        return lifecycle.transition(publicId, request);
    }
}
