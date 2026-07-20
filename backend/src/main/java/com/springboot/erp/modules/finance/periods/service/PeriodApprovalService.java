package com.springboot.erp.modules.finance.periods.service;

import com.springboot.erp.modules.finance.periods.domain.ApprovalStepStatus;
import com.springboot.erp.modules.finance.periods.domain.FiscalPeriod;
import com.springboot.erp.modules.finance.periods.domain.PeriodApprovalStep;
import com.springboot.erp.modules.finance.periods.dto.PeriodApprovalDtos.PeriodApprovalStepResponse;
import com.springboot.erp.modules.finance.periods.mapper.PeriodApprovalMapper;
import com.springboot.erp.modules.finance.periods.repository.PeriodApprovalStepRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.security.CurrentUser;
import com.springboot.erp.platform.status.StateMachine;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E-009 Phase 4 — the period-close approval chain (reference {@code PeriodApprovalView}). The
 * default chain (Finance Manager / Financial Controller / CFO — display labels only, gated on a
 * single {@code finance.period.close} permission, no segregation of duties) is seeded idempotently
 * per period. Steps are approved strictly in {@code sequence} order.
 */
@Service
public class PeriodApprovalService {

    private static final String AUDIT_ENTITY = "period_approval_step";

    /** Reference {@code DEFAULT_APPROVAL_CHAIN}. */
    private static final List<Map.Entry<Integer, String>> DEFAULT_CHAIN = List.of(
        Map.entry(1, "Finance Manager"),
        Map.entry(2, "Financial Controller"),
        Map.entry(3, "CFO"));

    /** PENDING -&gt; APPROVED|REJECTED. Both are effectively terminal (a fresh round resets to PENDING). */
    static final StateMachine<ApprovalStepStatus> WORKFLOW = StateMachine.builder(ApprovalStepStatus.class)
        .allow(ApprovalStepStatus.PENDING, ApprovalStepStatus.APPROVED, ApprovalStepStatus.REJECTED)
        .build();

    private final PeriodApprovalStepRepository repository;
    private final PeriodApprovalMapper mapper;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    public PeriodApprovalService(PeriodApprovalStepRepository repository, PeriodApprovalMapper mapper,
                                 AuditService auditService, CurrentUser currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    /** Idempotently create the default approval steps for a period (reference {@code _ensure_steps}). */
    @Transactional
    public List<PeriodApprovalStep> ensureSteps(FiscalPeriod period) {
        List<PeriodApprovalStep> existing = repository.findByFiscalPeriodIdOrderBySequenceAsc(period.getId());
        if (existing.size() == DEFAULT_CHAIN.size()) {
            return existing;
        }
        List<Integer> present = existing.stream().map(PeriodApprovalStep::getSequence).toList();
        for (Map.Entry<Integer, String> entry : DEFAULT_CHAIN) {
            if (!present.contains(entry.getKey())) {
                PeriodApprovalStep step = new PeriodApprovalStep();
                step.setFiscalPeriod(period);
                step.setSequence(entry.getKey());
                step.setLabel(entry.getValue());
                step.setStatus(ApprovalStepStatus.PENDING);
                repository.save(step);
            }
        }
        return repository.findByFiscalPeriodIdOrderBySequenceAsc(period.getId());
    }

    @Transactional
    public List<PeriodApprovalStepResponse> listApprovals(FiscalPeriod period) {
        return ensureSteps(period).stream().map(mapper::toResponse).toList();
    }

    /** Reset every step to PENDING — a fresh approval round (entering PENDING_APPROVAL again). */
    @Transactional
    public void resetSteps(FiscalPeriod period) {
        for (PeriodApprovalStep step : ensureSteps(period)) {
            Map<String, Object> before = Map.of(
                "sequence", step.getSequence(),
                "status", step.getStatus().name(),
                "approverUserId", String.valueOf(step.getApproverUserId()));
            step.setStatus(ApprovalStepStatus.PENDING);
            step.setApproverUserId(null);
            step.setDecidedAt(null);
            step.setComment(null);
            PeriodApprovalStep saved = repository.save(step);
            auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
                Map.of("sequence", saved.getSequence(), "status", saved.getStatus().name()));
        }
    }

    @Transactional
    public PeriodApprovalStepResponse approve(FiscalPeriod period, int sequence, String comment) {
        List<PeriodApprovalStep> steps = ensureSteps(period);
        PeriodApprovalStep step = findStep(steps, sequence);
        OptionalInt nextPending = steps.stream()
            .filter(s -> s.getStatus() == ApprovalStepStatus.PENDING)
            .mapToInt(PeriodApprovalStep::getSequence)
            .min();
        if (nextPending.isEmpty() || sequence != nextPending.getAsInt()) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Approval steps must be decided in order; next pending step is "
                    + (nextPending.isEmpty() ? "none" : nextPending.getAsInt()));
        }
        step.setStatus(WORKFLOW.transition(step.getStatus(), ApprovalStepStatus.APPROVED));
        step.setApproverUserId(actorId());
        step.setDecidedAt(Instant.now());
        step.setComment(comment);
        PeriodApprovalStep saved = repository.save(step);
        audit(period, saved, "approve");
        return mapper.toResponse(saved);
    }

    @Transactional
    public PeriodApprovalStepResponse reject(FiscalPeriod period, int sequence, String comment) {
        List<PeriodApprovalStep> steps = ensureSteps(period);
        PeriodApprovalStep step = findStep(steps, sequence);
        step.setStatus(WORKFLOW.transition(step.getStatus(), ApprovalStepStatus.REJECTED));
        step.setApproverUserId(actorId());
        step.setDecidedAt(Instant.now());
        step.setComment(comment);
        PeriodApprovalStep saved = repository.save(step);
        audit(period, saved, "reject");
        return mapper.toResponse(saved);
    }

    /** Gate: PENDING_APPROVAL -&gt; APPROVED requires every step APPROVED. */
    @Transactional
    public void assertApprovalsComplete(FiscalPeriod period) {
        List<Integer> pending = ensureSteps(period).stream()
            .filter(s -> s.getStatus() != ApprovalStepStatus.APPROVED)
            .map(PeriodApprovalStep::getSequence)
            .toList();
        if (!pending.isEmpty()) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Approval chain incomplete for '" + period.getPeriodCode() + "': pending steps " + pending);
        }
    }

    private PeriodApprovalStep findStep(List<PeriodApprovalStep> steps, int sequence) {
        return steps.stream().filter(s -> s.getSequence() == sequence).findFirst()
            .orElseThrow(() -> DomainException.notFound("PeriodApprovalStep", String.valueOf(sequence)));
    }

    private void audit(FiscalPeriod period, PeriodApprovalStep step, String action) {
        auditService.record(AUDIT_ENTITY, step.getPublicId(), AuditAction.UPDATE, null,
            Map.of("action", action, "sequence", step.getSequence(), "status", step.getStatus().name()));
    }

    private String actorId() {
        return currentUser.optional().map(p -> p.userPublicId()).orElse(null);
    }
}
