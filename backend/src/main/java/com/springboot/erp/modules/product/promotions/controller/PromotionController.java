package com.springboot.erp.modules.product.promotions.controller;

import com.springboot.erp.modules.product.promotions.dto.PromotionDtos.PromotionCreateRequest;
import com.springboot.erp.modules.product.promotions.dto.PromotionDtos.PromotionResponse;
import com.springboot.erp.modules.product.promotions.dto.PromotionDtos.PromotionStatusUpdateRequest;
import com.springboot.erp.modules.product.promotions.dto.PromotionDtos.PromotionUpdateRequest;
import com.springboot.erp.modules.product.promotions.service.PromotionService;
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
 * ENT-014 Promotion endpoints (thin controller — business rules in
 * {@link PromotionService}). Server-driven pagination, {@code @PreAuthorize} per
 * endpoint.
 */
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService service;

    public PromotionController(PromotionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('product.promotion.read')")
    public PageResponse<PromotionResponse> list(
            @RequestParam(required = false) String companyId,
            @PageableDefault(size = 50, sort = "dateFrom") Pageable pageable) {
        return service.list(companyId, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('product.promotion.read')")
    public PromotionResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product.promotion.write')")
    public PromotionResponse create(@Valid @RequestBody PromotionCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('product.promotion.write')")
    public PromotionResponse update(@PathVariable String publicId,
                                    @Valid @RequestBody PromotionUpdateRequest request) {
        return service.update(publicId, request);
    }

    @PatchMapping("/{publicId}/status")
    @PreAuthorize("hasAuthority('product.promotion.write')")
    public PromotionResponse updateStatus(@PathVariable String publicId,
                                          @Valid @RequestBody PromotionStatusUpdateRequest request) {
        return service.updateStatus(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('product.promotion.write')")
    public void delete(@PathVariable String publicId) {
        service.delete(publicId);
    }
}
