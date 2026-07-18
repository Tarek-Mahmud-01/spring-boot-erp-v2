package com.guru.erp.modules.inventory.counts.controller;

import com.guru.erp.modules.inventory.counts.dto.StockOpeningDtos.StockOpeningCreateRequest;
import com.guru.erp.modules.inventory.counts.dto.StockOpeningDtos.StockOpeningResponse;
import com.guru.erp.modules.inventory.counts.dto.StockOpeningDtos.StockOpeningUpdateRequest;
import com.guru.erp.modules.inventory.counts.service.StockOpeningService;
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
 * ENT-045 StockOpening endpoints (thin controller, @PreAuthorize on every
 * method). List / get / create / update / delete plus the {@code /post}
 * special action mirror the reference stock-opening view; business rules
 * delegate to {@link StockOpeningService}.
 */
@RestController
@RequestMapping("/api/inventory/stock-opening")
public class StockOpeningController {

    private final StockOpeningService service;

    public StockOpeningController(StockOpeningService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.opening.read')")
    public PageResponse<StockOpeningResponse> list(
            @RequestParam(required = false) String locationId,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(locationId, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.opening.read')")
    public StockOpeningResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory.opening.write')")
    public StockOpeningResponse create(@Valid @RequestBody StockOpeningCreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.opening.write')")
    public StockOpeningResponse update(@PathVariable String publicId,
                                       @Valid @RequestBody StockOpeningUpdateRequest request) {
        return service.update(publicId, request);
    }

    @PostMapping("/{publicId}/post")
    @PreAuthorize("hasAuthority('inventory.opening.write')")
    public StockOpeningResponse post(@PathVariable String publicId) {
        return service.post(publicId);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('inventory.opening.write')")
    public void delete(@PathVariable String publicId) {
        service.delete(publicId);
    }
}
