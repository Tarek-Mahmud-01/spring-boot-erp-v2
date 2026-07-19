package com.guru.erp.modules.finance.periods.controller;

import com.guru.erp.modules.finance.periods.domain.BasPeriodStatus;
import com.guru.erp.modules.finance.periods.dto.BasDtos.BasPeriodCreateRequest;
import com.guru.erp.modules.finance.periods.dto.BasDtos.BasPeriodResponse;
import com.guru.erp.modules.finance.periods.dto.BasDtos.BasPeriodTransitionRequest;
import com.guru.erp.modules.finance.periods.dto.BasDtos.BasReportSnapshotResponse;
import com.guru.erp.modules.finance.periods.service.BasPeriodService;
import com.guru.erp.modules.finance.periods.service.BasReportGenerationService;
import com.guru.erp.platform.web.PageResponse;
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
 * ENT-AU-001/002 BAS (Business Activity Statement) endpoints — CRUD + lifecycle for
 * {@code BasPeriod}, plus report generation and history (reference {@code BasPeriodListView} /
 * {@code BasPeriodTransitionView} / {@code BasPeriodGenerateView}).
 */
@RestController
@RequestMapping("/api/finance/bas-periods")
public class BasPeriodController {

    private final BasPeriodService periods;
    private final BasReportGenerationService reports;

    public BasPeriodController(BasPeriodService periods, BasReportGenerationService reports) {
        this.periods = periods;
        this.reports = reports;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance.bas.read')")
    public PageResponse<BasPeriodResponse> list(
            @RequestParam String companyId,
            @RequestParam(required = false) BasPeriodStatus status,
            @PageableDefault(size = 50) Pageable pageable) {
        return periods.list(companyId, status, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('finance.bas.read')")
    public BasPeriodResponse get(@PathVariable String publicId) {
        return periods.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('finance.bas.write')")
    public BasPeriodResponse create(@Valid @RequestBody BasPeriodCreateRequest request) {
        return periods.create(request);
    }

    /** FR-AU-023 — OPEN -&gt; LODGED -&gt; FROZEN (+ sanctioned un-lodge/unfreeze overrides). */
    @PostMapping("/{publicId}/transition")
    @PreAuthorize("hasAuthority('finance.bas.write')")
    public BasPeriodResponse transition(@PathVariable String publicId,
                                        @Valid @RequestBody BasPeriodTransitionRequest request) {
        return periods.transition(publicId, request);
    }

    /** FR-AU-020 — run the box computation and persist a new versioned snapshot. */
    @PostMapping("/{publicId}/generate")
    @PreAuthorize("hasAuthority('finance.bas.generate')")
    public BasReportSnapshotResponse generate(@PathVariable String publicId) {
        return reports.generate(periods.require(publicId));
    }

    @GetMapping("/{publicId}/reports")
    @PreAuthorize("hasAuthority('finance.bas.read')")
    public PageResponse<BasReportSnapshotResponse> reports(@PathVariable String publicId) {
        List<BasReportSnapshotResponse> rows = reports.listReports(periods.require(publicId));
        return new PageResponse<>(rows, 0, rows.size(), rows.size(), 1);
    }
}
