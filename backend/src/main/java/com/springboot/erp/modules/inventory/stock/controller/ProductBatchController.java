package com.springboot.erp.modules.inventory.stock.controller;

import com.springboot.erp.modules.inventory.stock.dto.ProductBatchDtos.BatchCreateRequest;
import com.springboot.erp.modules.inventory.stock.dto.ProductBatchDtos.BatchResponse;
import com.springboot.erp.modules.inventory.stock.dto.ProductBatchDtos.BatchUpdateRequest;
import com.springboot.erp.modules.inventory.stock.service.ProductBatchService;
import com.springboot.erp.platform.web.PageResponse;
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
 * ENT-045 ProductBatch endpoints (thin controller). Standard list / get / create
 * / update / delete plus a barcode look-up used at POS scan time. ULIDs only.
 */
@RestController
@RequestMapping("/api/inventory/batches")
public class ProductBatchController {

    private final ProductBatchService service;

    public ProductBatchController(ProductBatchService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.batch.read')")
    public PageResponse<BatchResponse> list(@RequestParam(required = false) String productId,
                                            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(productId, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.batch.read')")
    public BatchResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    @GetMapping("/by-barcode/{barcode}")
    @PreAuthorize("hasAuthority('inventory.batch.read')")
    public BatchResponse getByBarcode(@PathVariable String barcode) {
        return service.getByBarcode(barcode);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory.batch.write')")
    public BatchResponse create(@Valid @RequestBody BatchCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.batch.write')")
    public BatchResponse update(@PathVariable String publicId,
                                @Valid @RequestBody BatchUpdateRequest request) {
        return service.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('inventory.batch.write')")
    public void delete(@PathVariable String publicId) {
        service.delete(publicId);
    }
}
