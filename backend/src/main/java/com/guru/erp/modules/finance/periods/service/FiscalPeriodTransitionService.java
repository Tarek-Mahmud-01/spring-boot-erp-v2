package com.guru.erp.modules.finance.periods.service;

import com.guru.erp.modules.finance.periods.domain.FiscalPeriod;
import com.guru.erp.modules.finance.periods.domain.FiscalPeriodStatus;
import com.guru.erp.modules.finance.periods.dto.FiscalPeriodDtos.FiscalPeriodCreateRequest;
import com.guru.erp.modules.finance.periods.dto.FiscalPeriodDtos.FiscalPeriodResponse;
import com.guru.erp.modules.finance.periods.dto.FiscalPeriodDtos.FiscalPeriodTransitionRequest;
import com.guru.erp.modules.finance.periods.mapper.FiscalPeriodMapper;
import com.guru.erp.modules.finance.periods.repository.FiscalPeriodRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.security.CurrentUser;
import com.guru.erp.platform.status.StateMachine;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Period-end lifecycle transitions (reference {@code app.finance.views.fiscal_periods.
 * FiscalPeriodsView.transition_fiscal_period} / {@code create_fiscal_period}). Enforces:
 * <ul>
 *   <li>unique {@code periodCode} per company + no overlapping date range on create;</li>
 *   <li>the {@link StateMachine}-driven forward path plus sanctioned side-moves;</li>
 *   <li>a mandatory {@code reason} when reopening a CLOSED period or entering ADJUSTMENT;</li>
 *   <li>the FR-249 checklist gate on VALIDATING -&gt; PENDING_APPROVAL (delegates to
 *       {@link PeriodChecklistService}, which also resets the approval chain for a fresh round);</li>
 *   <li>the approval-chain gate on PENDING_APPROVAL -&gt; APPROVED (delegates to {@link PeriodApprovalService});</li>
 *   <li>the hard re-check gate on CLOSING -&gt; CLOSED, plus freezing a {@link PeriodSnapshotService}
 *       snapshot on that same transition.</li>
 * </ul>
 */
@Service
public class FiscalPeriodTransitionService {

    private static final String AUDIT_ENTITY = "fiscal_period";

    /** Reference {@code _ALLOWED_TRANSITIONS} in {@code app.finance.views.fiscal_periods}. */
    static final StateMachine<FiscalPeriodStatus> WORKFLOW = StateMachine.builder(FiscalPeriodStatus.class)
        .allow(FiscalPeriodStatus.DRAFT, FiscalPeriodStatus.OPEN)
        .allow(FiscalPeriodStatus.OPEN, FiscalPeriodStatus.PREPARING)
        .allow(FiscalPeriodStatus.PREPARING, FiscalPeriodStatus.RECONCILING, FiscalPeriodStatus.OPEN)
        .allow(FiscalPeriodStatus.RECONCILING, FiscalPeriodStatus.VALIDATING, FiscalPeriodStatus.PREPARING)
        .allow(FiscalPeriodStatus.VALIDATING, FiscalPeriodStatus.PENDING_APPROVAL, FiscalPeriodStatus.RECONCILING)
        .allow(FiscalPeriodStatus.PENDING_APPROVAL, FiscalPeriodStatus.APPROVED, FiscalPeriodStatus.RECONCILING)
        .allow(FiscalPeriodStatus.APPROVED, FiscalPeriodStatus.CLOSING, FiscalPeriodStatus.RECONCILING)
        .allow(FiscalPeriodStatus.CLOSING, FiscalPeriodStatus.CLOSED)
        .allow(FiscalPeriodStatus.CLOSED, FiscalPeriodStatus.ADJUSTMENT, FiscalPeriodStatus.ARCHIVED, FiscalPeriodStatus.OPEN)
        .allow(FiscalPeriodStatus.ADJUSTMENT, FiscalPeriodStatus.CLOSED)
        .build();

    private final FiscalPeriodRepository repository;
    private final FiscalPeriodQueryService queryService;
    private final FiscalPeriodMapper mapper;
    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final PeriodChecklistService checklistService;
    private final PeriodApprovalService approvalService;
    private final PeriodSnapshotService snapshotService;

    public FiscalPeriodTransitionService(FiscalPeriodRepository repository, FiscalPeriodQueryService queryService,
                                         FiscalPeriodMapper mapper, AuditService auditService, CurrentUser currentUser,
                                         PeriodChecklistService checklistService, PeriodApprovalService approvalService,
                                         PeriodSnapshotService snapshotService) {
        this.repository = repository;
        this.queryService = queryService;
        this.mapper = mapper;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.checklistService = checklistService;
        this.approvalService = approvalService;
        this.snapshotService = snapshotService;
    }

    @Transactional
    public FiscalPeriodResponse create(FiscalPeriodCreateRequest req) {
        if (repository.existsByCompanyIdAndPeriodCode(req.companyId(), req.periodCode())) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "A fiscal period with code '" + req.periodCode() + "' already exists for this company");
        }
        List<FiscalPeriod> overlap = repository.findOverlapping(req.companyId(), req.dateFrom(), req.dateTo());
        if (!overlap.isEmpty()) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Date range overlaps existing fiscal period '" + overlap.get(0).getPeriodCode() + "'");
        }
        FiscalPeriod period = new FiscalPeriod();
        period.setCompanyId(req.companyId());
        period.setPeriodCode(req.periodCode());
        period.setDateFrom(req.dateFrom());
        period.setDateTo(req.dateTo());
        period.setStatus(FiscalPeriodStatus.OPEN);
        FiscalPeriod saved = repository.save(period);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public FiscalPeriodResponse transition(String publicId, FiscalPeriodTransitionRequest req) {
        FiscalPeriod period = queryService.require(publicId);
        FiscalPeriodStatus from = period.getStatus();
        FiscalPeriodStatus to = req.newStatus();
        if (from == to) {
            return mapper.toResponse(period);
        }

        // Phase-6: a reason is mandatory when re-opening a CLOSED period or entering ADJUSTMENT —
        // audited exceptions to the normal forward flow.
        boolean reasonRequired = (from == FiscalPeriodStatus.CLOSED && to == FiscalPeriodStatus.OPEN)
            || (from == FiscalPeriodStatus.CLOSED && to == FiscalPeriodStatus.ADJUSTMENT);
        if (reasonRequired && (req.reason() == null || req.reason().isBlank())) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "A reason is required to move from " + from + " to " + to);
        }

        WORKFLOW.transition(from, to);

        // FR-249 gate: VALIDATING -> PENDING_APPROVAL requires a freshly-passed + signed-off
        // checklist; entering PENDING_APPROVAL also resets the approval chain for a fresh round.
        if (from == FiscalPeriodStatus.VALIDATING && to == FiscalPeriodStatus.PENDING_APPROVAL) {
            checklistService.assertChecklistComplete(period);
            approvalService.resetSteps(period);
        }
        // Phase-4 gate: PENDING_APPROVAL -> APPROVED requires every approval step APPROVED.
        if (from == FiscalPeriodStatus.PENDING_APPROVAL && to == FiscalPeriodStatus.APPROVED) {
            approvalService.assertApprovalsComplete(period);
        }
        // Strict hard-close gate: even after approval, re-run the required checks live.
        if (from == FiscalPeriodStatus.CLOSING && to == FiscalPeriodStatus.CLOSED) {
            checklistService.assertDataCleanForClose(period);
        }

        FiscalPeriodResponse before = mapper.toResponse(period);
        period.setStatus(to);
        FiscalPeriod saved = repository.save(period);

        // Phase-5: freeze an immutable financial snapshot on CLOSING -> CLOSED.
        if (from == FiscalPeriodStatus.CLOSING && to == FiscalPeriodStatus.CLOSED) {
            snapshotService.createSnapshot(saved, actorId());
        }

        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before, mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    private String actorId() {
        return currentUser.optional().map(p -> p.userPublicId()).orElse(null);
    }
}
