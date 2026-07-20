package com.springboot.erp.modules.settings.taxcode.controller;

import com.springboot.erp.modules.settings.taxcode.dto.TaxCodeDtos.TaxCodeCreateRequest;
import com.springboot.erp.modules.settings.taxcode.dto.TaxCodeDtos.TaxCodeResponse;
import com.springboot.erp.modules.settings.taxcode.dto.TaxCodeDtos.TaxCodeUpdateRequest;
import com.springboot.erp.modules.settings.taxcode.service.TaxCodeService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENT-007 Tax Code endpoints, mounted at {@code /api/tax-codes}. Thin controller
 * (ARCHITECTURE.md §2): {@code @PreAuthorize} on every route, delegates all
 * business rules to {@link TaxCodeService}. Clients only ever see the ULID
 * public id, never the internal bigint.
 */
@RestController
@RequestMapping("/api/tax-codes")
public class TaxCodeController {

    private final TaxCodeService service;

    public TaxCodeController(TaxCodeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('settings.taxcode.read')")
    public PageResponse<TaxCodeResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('settings.taxcode.read')")
    public TaxCodeResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('settings.taxcode.write')")
    public TaxCodeResponse create(@Valid @RequestBody TaxCodeCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('settings.taxcode.write')")
    public TaxCodeResponse update(@PathVariable String publicId,
                                  @Valid @RequestBody TaxCodeUpdateRequest request) {
        return service.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('settings.taxcode.write')")
    public void delete(@PathVariable String publicId) {
        service.delete(publicId);
    }
}
