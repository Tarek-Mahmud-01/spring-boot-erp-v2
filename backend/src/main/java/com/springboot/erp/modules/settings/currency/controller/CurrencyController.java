package com.springboot.erp.modules.settings.currency.controller;

import com.springboot.erp.modules.settings.currency.dto.CurrencyDtos.CurrencyCreateRequest;
import com.springboot.erp.modules.settings.currency.dto.CurrencyDtos.CurrencyResponse;
import com.springboot.erp.modules.settings.currency.dto.CurrencyDtos.CurrencyUpdateRequest;
import com.springboot.erp.modules.settings.currency.service.CurrencyService;
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
 * ENT-018 Currency endpoints (ARCHITECTURE.md §2 — thin controller,
 * {@code @PreAuthorize} per endpoint, server-driven pagination). Business rules
 * live in {@link CurrencyService}.
 */
@RestController
@RequestMapping("/api/currencies")
public class CurrencyController {

    private final CurrencyService service;

    public CurrencyController(CurrencyService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('settings.currency.read')")
    public PageResponse<CurrencyResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 200, sort = "code") Pageable pageable) {
        return service.list(q, status, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('settings.currency.read')")
    public CurrencyResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('settings.currency.write')")
    public CurrencyResponse create(@Valid @RequestBody CurrencyCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('settings.currency.write')")
    public CurrencyResponse update(@PathVariable String publicId,
                                   @Valid @RequestBody CurrencyUpdateRequest request) {
        return service.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('settings.currency.write')")
    public void delete(@PathVariable String publicId) {
        service.delete(publicId);
    }

    @PostMapping("/{publicId}/set-default")
    @PreAuthorize("hasAuthority('settings.currency.write')")
    public CurrencyResponse setDefault(@PathVariable String publicId) {
        return service.setDefault(publicId);
    }
}
