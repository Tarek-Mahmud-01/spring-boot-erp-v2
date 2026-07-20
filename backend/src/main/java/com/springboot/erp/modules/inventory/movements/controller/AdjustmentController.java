package com.springboot.erp.modules.inventory.movements.controller;

import com.springboot.erp.modules.inventory.movements.dto.AdjustmentDtos.AdjustmentCreateRequest;
import com.springboot.erp.modules.inventory.movements.dto.AdjustmentDtos.AdjustmentResponse;
import com.springboot.erp.modules.inventory.movements.dto.AdjustmentDtos.AdjustmentUpdateRequest;
import com.springboot.erp.modules.inventory.movements.service.AdjustmentCommandService;
import com.springboot.erp.modules.inventory.movements.service.AdjustmentQueryService;
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
 * ENT-043 StockAdjustment endpoints — thin controller, {@code @PreAuthorize} per method. Business
 * rules live in the command/query services. Special endpoints (approve / post) map the reference
 * adjustment sub-actions.
 */
@RestController
@RequestMapping("/api/inventory/adjustments")
public class AdjustmentController {

    private final AdjustmentCommandService command;
    private final AdjustmentQueryService query;

    public AdjustmentController(AdjustmentCommandService command, AdjustmentQueryService query) {
        this.command = command;
        this.query = query;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.adjustment.read')")
    public PageResponse<AdjustmentResponse> list(
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        return query.list(locationId, status, search, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.adjustment.read')")
    public AdjustmentResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory.adjustment.write')")
    public AdjustmentResponse create(@Valid @RequestBody AdjustmentCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.adjustment.write')")
    public AdjustmentResponse update(@PathVariable String publicId,
                                     @Valid @RequestBody AdjustmentUpdateRequest request) {
        return command.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('inventory.adjustment.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    /** FR-131 — approve the adjustment (Draft/Pending → Approved). */
    @PostMapping("/{publicId}/approve")
    @PreAuthorize("hasAuthority('inventory.adjustment.write')")
    public AdjustmentResponse approve(@PathVariable String publicId) {
        return command.approve(publicId);
    }

    /** FR-132 — post the adjustment (Approved → Posted), writing ledger movements + a GL journal. */
    @PostMapping("/{publicId}/post")
    @PreAuthorize("hasAuthority('inventory.adjustment.write')")
    public AdjustmentResponse post(@PathVariable String publicId) {
        return command.post(publicId);
    }
}
