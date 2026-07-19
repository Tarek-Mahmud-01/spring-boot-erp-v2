package com.guru.erp.modules.finance.periods.controller;

import com.guru.erp.modules.finance.periods.domain.ChecklistItemKey;
import com.guru.erp.modules.finance.periods.domain.FiscalPeriod;
import com.guru.erp.modules.finance.periods.dto.PeriodChecklistDtos.PeriodChecklistItemResponse;
import com.guru.erp.modules.finance.periods.dto.PeriodChecklistDtos.PeriodChecklistOwnerRequest;
import com.guru.erp.modules.finance.periods.service.FiscalPeriodQueryService;
import com.guru.erp.modules.finance.periods.service.PeriodChecklistService;
import com.guru.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-249 period-close checklist endpoints (reference {@code PeriodChecklistListView} /
 * {@code PeriodChecklistRunView} / {@code PeriodChecklistOwnerView} / {@code PeriodChecklistSignOffView}).
 */
@RestController
@RequestMapping("/api/finance/fiscal-periods/{periodId}/checklist")
public class PeriodChecklistController {

    private final FiscalPeriodQueryService periods;
    private final PeriodChecklistService checklist;

    public PeriodChecklistController(FiscalPeriodQueryService periods, PeriodChecklistService checklist) {
        this.periods = periods;
        this.checklist = checklist;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance.period.read')")
    public PageResponse<PeriodChecklistItemResponse> list(@PathVariable String periodId) {
        return toPageResponse(checklist.listChecklist(period(periodId)));
    }

    /** Re-evaluate every item's automated check against live data. */
    @PostMapping("/run")
    @PreAuthorize("hasAuthority('finance.period.write')")
    public PageResponse<PeriodChecklistItemResponse> run(@PathVariable String periodId) {
        return toPageResponse(checklist.runChecks(period(periodId)));
    }

    @PostMapping("/{itemKey}/owner")
    @PreAuthorize("hasAuthority('finance.period.write')")
    public PeriodChecklistItemResponse setOwner(@PathVariable String periodId, @PathVariable ChecklistItemKey itemKey,
                                                @Valid @RequestBody PeriodChecklistOwnerRequest request) {
        return checklist.setOwner(period(periodId), itemKey, request.ownerUserId());
    }

    @PostMapping("/{itemKey}/sign-off")
    @PreAuthorize("hasAuthority('finance.period.write')")
    public PeriodChecklistItemResponse signOff(@PathVariable String periodId, @PathVariable ChecklistItemKey itemKey) {
        return checklist.signOff(period(periodId), itemKey);
    }

    private FiscalPeriod period(String periodId) {
        return periods.require(periodId);
    }

    /** Checklist rows are a small, fixed-size in-memory list (six items) — not a paged repository
     *  query — so it is wrapped as a single full page to keep every list response envelope-shaped. */
    private static PageResponse<PeriodChecklistItemResponse> toPageResponse(List<PeriodChecklistItemResponse> items) {
        return new PageResponse<>(items, 0, items.size(), items.size(), 1);
    }
}
