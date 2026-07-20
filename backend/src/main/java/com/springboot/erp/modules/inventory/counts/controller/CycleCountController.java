package com.springboot.erp.modules.inventory.counts.controller;

import com.springboot.erp.modules.inventory.counts.dto.CycleCountDtos.CycleCountCreateRequest;
import com.springboot.erp.modules.inventory.counts.dto.CycleCountDtos.CycleCountResponse;
import com.springboot.erp.modules.inventory.counts.dto.CycleCountDtos.LineCountRequest;
import com.springboot.erp.modules.inventory.counts.dto.CycleCountDtos.LineSecondPassRequest;
import com.springboot.erp.modules.inventory.counts.service.CycleCountService;
import com.springboot.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENT-044 CycleCountPlan endpoints (thin controller, @PreAuthorize on every
 * method). List / get / create plus the lifecycle sub-actions
 * (first pass / second pass / approve) mirror the reference cycle-count view;
 * all business rules delegate to {@link CycleCountService}.
 */
@RestController
@RequestMapping("/api/inventory/cycle-counts")
public class CycleCountController {

    private final CycleCountService service;

    public CycleCountController(CycleCountService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.cyclecount.read')")
    public PageResponse<CycleCountResponse> list(
            @RequestParam(required = false) String locationId,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(locationId, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.cyclecount.read')")
    public CycleCountResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory.cyclecount.write')")
    public CycleCountResponse create(@Valid @RequestBody CycleCountCreateRequest request) {
        return service.create(request);
    }

    @PostMapping("/{publicId}/first-pass")
    @PreAuthorize("hasAuthority('inventory.cyclecount.write')")
    public CycleCountResponse submitFirstPass(@PathVariable String publicId,
                                              @Valid @RequestBody List<LineCountRequest> counts) {
        return service.submitFirstPass(publicId, counts);
    }

    @PostMapping("/{publicId}/second-pass")
    @PreAuthorize("hasAuthority('inventory.cyclecount.write')")
    public CycleCountResponse submitSecondPass(@PathVariable String publicId,
                                               @Valid @RequestBody List<LineSecondPassRequest> counts) {
        return service.submitSecondPass(publicId, counts);
    }

    @PostMapping("/{publicId}/approve")
    @PreAuthorize("hasAuthority('inventory.cyclecount.write')")
    public CycleCountResponse approve(@PathVariable String publicId) {
        return service.approve(publicId);
    }
}
