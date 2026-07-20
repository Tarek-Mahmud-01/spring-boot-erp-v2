package com.springboot.erp.modules.product.pricing.controller;

import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListCreateRequest;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListItemPatchRequest;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListItemResponse;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListItemUpsertRequest;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListResponse;
import com.springboot.erp.modules.product.pricing.dto.PricingDtos.PriceListUpdateRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENT-013 PriceList + ENT-013a PriceListItem endpoints (US-013). Thin controller
 * delegating to the pricing services; business rules live there. Standard CRUD
 * plus the reference nested item endpoints (list/upsert/patch/delete items on a
 * list). Under the 150-line cap.
 */
@RestController
@RequestMapping("/api/product/price-lists")
public class PriceListController {

    private final PricingQueryService query;
    private final PricingCommandService command;

    public PriceListController(PricingQueryService query, PricingCommandService command) {
        this.query = query;
        this.command = command;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('product.pricelist.read')")
    public PageResponse<PriceListResponse> list(
            @RequestParam(required = false) String companyId,
            @PageableDefault(size = 50, sort = "name") Pageable pageable) {
        return query.listPriceLists(companyId, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('product.pricelist.read')")
    public PriceListResponse get(@PathVariable String publicId) {
        return query.getPriceList(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product.pricelist.write')")
    public PriceListResponse create(@Valid @RequestBody PriceListCreateRequest request) {
        return command.createPriceList(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('product.pricelist.write')")
    public PriceListResponse update(@PathVariable String publicId,
                                    @Valid @RequestBody PriceListUpdateRequest request) {
        return command.updatePriceList(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('product.pricelist.write')")
    public void delete(@PathVariable String publicId) {
        command.deletePriceList(publicId);
    }

    // --- nested price list items ---

    @GetMapping("/{publicId}/items")
    @PreAuthorize("hasAuthority('product.pricelist.read')")
    public PageResponse<PriceListItemResponse> listItems(
            @PathVariable String publicId,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.listItems(publicId, pageable);
    }

    @PostMapping("/{publicId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product.pricelist.write')")
    public PriceListItemResponse upsertItem(@PathVariable String publicId,
                                            @Valid @RequestBody PriceListItemUpsertRequest request) {
        return command.upsertItem(publicId, request);
    }

    @GetMapping("/items/{itemPublicId}")
    @PreAuthorize("hasAuthority('product.pricelist.read')")
    public PriceListItemResponse getItem(@PathVariable String itemPublicId) {
        return query.getItem(itemPublicId);
    }

    @PatchMapping("/items/{itemPublicId}")
    @PreAuthorize("hasAuthority('product.pricelist.write')")
    public PriceListItemResponse patchItem(@PathVariable String itemPublicId,
                                           @Valid @RequestBody PriceListItemPatchRequest request) {
        return command.patchItem(itemPublicId, request);
    }

    @DeleteMapping("/items/{itemPublicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('product.pricelist.write')")
    public void deleteItem(@PathVariable String itemPublicId) {
        command.deleteItem(itemPublicId);
    }
}
