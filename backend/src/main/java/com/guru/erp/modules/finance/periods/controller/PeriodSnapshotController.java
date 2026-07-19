package com.guru.erp.modules.finance.periods.controller;

import com.guru.erp.modules.finance.periods.dto.PeriodSnapshotDtos.PeriodSnapshotResponse;
import com.guru.erp.modules.finance.periods.service.FiscalPeriodQueryService;
import com.guru.erp.modules.finance.periods.service.PeriodSnapshotService;
import com.guru.erp.platform.web.PageResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * E-009 Phase 5 read-only frozen snapshot history (reference {@code PeriodSnapshotListView}).
 * Snapshots are created only as a side effect of the CLOSING -&gt; CLOSED transition (see
 * {@code FiscalPeriodTransitionService}) — there is no write endpoint here.
 */
@RestController
@RequestMapping("/api/finance/fiscal-periods/{periodId}/snapshots")
public class PeriodSnapshotController {

    private final FiscalPeriodQueryService periods;
    private final PeriodSnapshotService snapshots;

    public PeriodSnapshotController(FiscalPeriodQueryService periods, PeriodSnapshotService snapshots) {
        this.periods = periods;
        this.snapshots = snapshots;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance.period.read')")
    public PageResponse<PeriodSnapshotResponse> list(@PathVariable String periodId) {
        List<PeriodSnapshotResponse> rows = snapshots.listSnapshots(periods.require(periodId));
        return new PageResponse<>(rows, 0, rows.size(), rows.size(), 1);
    }
}
