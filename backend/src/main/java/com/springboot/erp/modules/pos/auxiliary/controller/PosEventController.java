package com.springboot.erp.modules.pos.auxiliary.controller;

import com.springboot.erp.modules.pos.auxiliary.dto.PosEventDtos.PosEventResponse;
import com.springboot.erp.modules.pos.auxiliary.dto.PosEventDtos.RecordEventRequest;
import com.springboot.erp.modules.pos.auxiliary.dto.PosEventDtos.ReviewEventRequest;
import com.springboot.erp.modules.pos.auxiliary.service.PosEventService;
import com.springboot.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
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
 * PosEvent endpoints (FR-AU-013 / FR-198 / FR-25.8) — the POS-domain operational timeline (age
 * verification, offline conflicts, manual discounts, peripheral failures, manager overrides), thin
 * controller delegating to {@link PosEventService}. Distinct from the platform audit_logs trail
 * (no controller of its own — read via {@code AuditLogRepository} directly where needed).
 */
@RestController
@RequestMapping("/api/pos/events")
public class PosEventController {

    private final PosEventService service;

    public PosEventController(PosEventService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('pos.event.read')")
    public PageResponse<PosEventResponse> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String registerId,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) Boolean needsReview,
            @PageableDefault(size = 50) Pageable pageable) {
        return service.list(type, registerId, transactionId, needsReview, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('pos.event.read')")
    public PosEventResponse get(@PathVariable String publicId) {
        return service.get(publicId);
    }

    /** Append one POS-domain event row (e.g. PERIPHERAL_FAILURE, AGE_VERIFICATION_REFUSED). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('pos.event.write')")
    public PosEventResponse record(@Valid @RequestBody RecordEventRequest request) {
        return service.record(request);
    }

    /** Mark an event reviewed (FR-25.8 store-manager review queue). */
    @PostMapping("/{publicId}/review")
    @PreAuthorize("hasAuthority('pos.event.write')")
    public PosEventResponse review(@PathVariable String publicId,
                                   @RequestBody(required = false) ReviewEventRequest request) {
        return service.review(publicId, request == null ? null : request.reviewedBy());
    }
}
