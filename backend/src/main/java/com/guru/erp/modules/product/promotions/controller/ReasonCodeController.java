package com.guru.erp.modules.product.promotions.controller;

import com.guru.erp.modules.product.promotions.dto.ReasonCodeDtos.ReasonCodeCreateRequest;
import com.guru.erp.modules.product.promotions.dto.ReasonCodeDtos.ReasonCodeResponse;
import com.guru.erp.modules.product.promotions.dto.ReasonCodeDtos.ReasonCodeUpdateRequest;
import com.guru.erp.modules.product.promotions.service.ReasonCodeService;
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
 * FR-068 reason-code master endpoints (thin controller — rules in
 * {@link ReasonCodeService}).
 */
@RestController
@RequestMapping("/api/reason-codes")
public class ReasonCodeController {

    private final ReasonCodeService service;

    public ReasonCodeController(ReasonCodeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('product.reasoncode.read')")
    public PageResponse<ReasonCodeResponse> list(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @PageableDefault(size = 100, sort = "code") Pageable pageable) {
        return service.list(activeOnly, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('product.reasoncode.read')")
    public ReasonCodeResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product.reasoncode.write')")
    public ReasonCodeResponse create(@Valid @RequestBody ReasonCodeCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('product.reasoncode.write')")
    public ReasonCodeResponse update(@PathVariable String publicId,
                                     @Valid @RequestBody ReasonCodeUpdateRequest request) {
        return service.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('product.reasoncode.write')")
    public void delete(@PathVariable String publicId) {
        service.delete(publicId);
    }
}
