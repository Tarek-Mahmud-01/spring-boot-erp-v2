package com.springboot.erp.modules.finance.periods.controller;

import com.springboot.erp.modules.finance.periods.domain.FiscalPeriodStatus;
import com.springboot.erp.modules.finance.periods.dto.FiscalPeriodDtos.FiscalPeriodCreateRequest;
import com.springboot.erp.modules.finance.periods.dto.FiscalPeriodDtos.FiscalPeriodResponse;
import com.springboot.erp.modules.finance.periods.dto.FiscalPeriodDtos.FiscalPeriodTransitionRequest;
import com.springboot.erp.modules.finance.periods.service.FiscalPeriodQueryService;
import com.springboot.erp.modules.finance.periods.service.FiscalPeriodTransitionService;
import com.springboot.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
 * ENT-015 FiscalPeriod endpoints (reference {@code app.finance.views.fiscal_periods} URL group) —
 * thin controller, business rules live in {@link FiscalPeriodQueryService} /
 * {@link FiscalPeriodTransitionService}.
 */
@RestController
@RequestMapping("/api/finance/fiscal-periods")
public class FiscalPeriodController {

    private final FiscalPeriodQueryService queryService;
    private final FiscalPeriodTransitionService transitionService;

    public FiscalPeriodController(FiscalPeriodQueryService queryService, FiscalPeriodTransitionService transitionService) {
        this.queryService = queryService;
        this.transitionService = transitionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance.period.read')")
    public PageResponse<FiscalPeriodResponse> list(
            @RequestParam String companyId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) FiscalPeriodStatus status,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @PageableDefault(size = 50) Pageable pageable) {
        return queryService.list(companyId, q, status, dateFrom, dateTo, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('finance.period.read')")
    public FiscalPeriodResponse get(@PathVariable String publicId) {
        return queryService.get(publicId);
    }

    /** Manual creation of a period with a custom date range (status OPEN). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('finance.period.write')")
    public FiscalPeriodResponse create(@Valid @RequestBody FiscalPeriodCreateRequest request) {
        return transitionService.create(request);
    }

    /** FR-225/FR-250 — the full period-end lifecycle transition (also drives the FR-249 checklist
     *  and E-009 Phase 4 approval gates internally). */
    @PostMapping("/{publicId}/transition")
    @PreAuthorize("hasAuthority('finance.period.write')")
    public FiscalPeriodResponse transition(@PathVariable String publicId,
                                           @Valid @RequestBody FiscalPeriodTransitionRequest request) {
        return transitionService.transition(publicId, request);
    }
}
