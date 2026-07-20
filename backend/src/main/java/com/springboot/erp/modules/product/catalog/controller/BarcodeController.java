package com.springboot.erp.modules.product.catalog.controller;

import com.springboot.erp.modules.product.catalog.dto.BarcodeDtos.BarcodeCreateRequest;
import com.springboot.erp.modules.product.catalog.dto.BarcodeDtos.BarcodeResponse;
import com.springboot.erp.modules.product.catalog.service.BarcodeService;
import com.springboot.erp.modules.product.catalog.service.ProductQueryService;
import jakarta.validation.Valid;
import java.util.List;
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
 * ENT-011b ProductBarcode endpoints (FR-056–059). List/create are nested under
 * a product; set-primary/delete/lookup act on a barcode directly. The barcode
 * lookup is a thin special endpoint delegating to the query service.
 */
@RestController
@RequestMapping("/api/products")
public class BarcodeController {

    private final BarcodeService service;
    private final ProductQueryService query;

    public BarcodeController(BarcodeService service, ProductQueryService query) {
        this.service = service;
        this.query = query;
    }

    @GetMapping("/{productId}/barcodes")
    @PreAuthorize("hasAuthority('product.barcode.read')")
    public List<BarcodeResponse> list(@PathVariable String productId) {
        return service.list(productId);
    }

    @PostMapping("/{productId}/barcodes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product.barcode.write')")
    public BarcodeResponse create(@PathVariable String productId,
                                  @Valid @RequestBody BarcodeCreateRequest request) {
        return service.create(productId, request);
    }

    @PostMapping("/barcodes/{barcodeId}/primary")
    @PreAuthorize("hasAuthority('product.barcode.write')")
    public BarcodeResponse setPrimary(@PathVariable String barcodeId) {
        return service.setPrimary(barcodeId);
    }

    @DeleteMapping("/barcodes/{barcodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('product.barcode.write')")
    public void delete(@PathVariable String barcodeId) {
        service.delete(barcodeId);
    }

    /** FR-163 — resolve a scanned exact barcode to its product/variant. */
    @GetMapping("/barcodes/lookup")
    @PreAuthorize("hasAuthority('product.barcode.read')")
    public BarcodeResponse lookup(@RequestParam String barcode) {
        return query.lookupByBarcode(barcode);
    }
}
