package com.guru.erp.modules.finance.periods.controller;

import com.guru.erp.modules.finance.periods.domain.FiscalPeriod;
import com.guru.erp.modules.finance.periods.dto.PeriodApprovalDtos.PeriodApprovalDecisionRequest;
import com.guru.erp.modules.finance.periods.dto.PeriodApprovalDtos.PeriodApprovalStepResponse;
import com.guru.erp.modules.finance.periods.service.FiscalPeriodQueryService;
import com.guru.erp.modules.finance.periods.service.PeriodApprovalService;
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
 * E-009 Phase 4 period-close approval chain endpoints (reference {@code PeriodApprovalListView} /
 * {@code PeriodApprovalApproveView} / {@code PeriodApprovalRejectView}). Gated on
 * {@code finance.period.close} — any holder may decide any step (no segregation of duties).
 */
@RestController
@RequestMapping("/api/finance/fiscal-periods/{periodId}/approvals")
public class PeriodApprovalController {

    private final FiscalPeriodQueryService periods;
    private final PeriodApprovalService approvals;

    public PeriodApprovalController(FiscalPeriodQueryService periods, PeriodApprovalService approvals) {
        this.periods = periods;
        this.approvals = approvals;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('finance.period.read')")
    public PageResponse<PeriodApprovalStepResponse> list(@PathVariable String periodId) {
        List<PeriodApprovalStepResponse> steps = approvals.listApprovals(period(periodId));
        return new PageResponse<>(steps, 0, steps.size(), steps.size(), 1);
    }

    @PostMapping("/{sequence}/approve")
    @PreAuthorize("hasAuthority('finance.period.close')")
    public PeriodApprovalStepResponse approve(@PathVariable String periodId, @PathVariable int sequence,
                                              @Valid @RequestBody PeriodApprovalDecisionRequest request) {
        return approvals.approve(period(periodId), sequence, request.comment());
    }

    @PostMapping("/{sequence}/reject")
    @PreAuthorize("hasAuthority('finance.period.close')")
    public PeriodApprovalStepResponse reject(@PathVariable String periodId, @PathVariable int sequence,
                                             @Valid @RequestBody PeriodApprovalDecisionRequest request) {
        return approvals.reject(period(periodId), sequence, request.comment());
    }

    private FiscalPeriod period(String periodId) {
        return periods.require(periodId);
    }
}
