package com.springboot.erp.modules.inventory.counts.service;

import com.springboot.erp.modules.inventory.counts.domain.CycleCountLine;
import com.springboot.erp.modules.inventory.counts.domain.CycleCountPlan;
import com.springboot.erp.modules.inventory.counts.domain.CycleCountScope;
import com.springboot.erp.modules.inventory.counts.domain.CycleCountStatus;
import com.springboot.erp.modules.inventory.counts.dto.CycleCountDtos.CycleCountCreateRequest;
import com.springboot.erp.modules.inventory.counts.dto.CycleCountDtos.CycleCountResponse;
import com.springboot.erp.modules.inventory.counts.dto.CycleCountDtos.LineCountRequest;
import com.springboot.erp.modules.inventory.counts.dto.CycleCountDtos.LineSecondPassRequest;
import com.springboot.erp.modules.inventory.counts.mapper.CountsMapper;
import com.springboot.erp.modules.inventory.counts.repository.CycleCountPlanRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import com.springboot.erp.platform.status.StateMachine;
import com.springboot.erp.platform.web.PageResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ENT-044 CycleCountPlan use-cases — the count lifecycle Draft -&gt; In Progress
 * -&gt; Completed -&gt; Approved (US-025 / FR-134-138), ported from
 * app.inventory.views.cycle_counts + _legacy.
 *
 * <p>On create the plan is opened, its lines are generated from the requested
 * scope's product list, then it is flipped to In Progress. First pass records
 * counts + variance and flags recounts; second pass reconciles flagged lines
 * and completes; approval computes accuracy and posts variance stock movements.
 *
 * <p>Cross-slice writes: approval's variance postings are emitted as
 * {@code inventory.cycle_count.approved} outbox events (documented payload with
 * per-line product/variance/location) rather than a hard dependency on the
 * ledger slice — same guidance as movements. The expected on-hand quantity per
 * line and the scope's product list also come from other slices; here they are
 * carried on the request/scopeConfig (the caller resolves them) so this slice
 * has no compile dependency on the catalog or ledger.
 */
@Service
public class CycleCountService {

    private static final String AUDIT_ENTITY = "cycle_count_plan";
    private static final String EVENT_APPROVED = "inventory.cycle_count.approved";
    private static final String NUMBER_PREFIX = "CC-";

    private static final StateMachine<CycleCountStatus> LIFECYCLE = StateMachine
        .builder(CycleCountStatus.class)
        .allow(CycleCountStatus.DRAFT, CycleCountStatus.IN_PROGRESS)
        .allow(CycleCountStatus.IN_PROGRESS, CycleCountStatus.COMPLETED)
        .allow(CycleCountStatus.COMPLETED, CycleCountStatus.APPROVED)
        .build();

    private final CycleCountPlanRepository repository;
    private final CountsMapper mapper;
    private final AuditService auditService;
    private final OutboxPublisher outbox;

    public CycleCountService(CycleCountPlanRepository repository, CountsMapper mapper,
                             AuditService auditService, OutboxPublisher outbox) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public PageResponse<CycleCountResponse> list(String locationId, Pageable pageable) {
        var page = locationId == null
            ? repository.findAllByOrderByCreatedAtDesc(pageable)
            : repository.findByLocationIdOrderByCreatedAtDesc(locationId, pageable);
        return PageResponse.of(page, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CycleCountResponse get(String publicId) {
        return mapper.toResponse(load(publicId));
    }

    @Transactional
    public CycleCountResponse create(CycleCountCreateRequest req) {
        CycleCountScope scope = req.scope() == null ? CycleCountScope.ALL : req.scope();
        List<String> productIds = CycleCountScopeResolver.productIds(scope, req.scopeConfig());
        if (productIds.isEmpty()) {
            // Reference CycleCountNoLinesError — a plan must count something.
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "No products found for the specified count scope.");
        }

        CycleCountPlan plan = new CycleCountPlan();
        plan.setNumber(nextNumber());
        plan.setLocationId(req.locationId());
        plan.setScope(scope);
        plan.setScopeConfig(req.scopeConfig());
        plan.setPlannedDate(req.plannedDate());
        plan.setNotes(req.notes());
        plan.setStatus(CycleCountStatus.DRAFT);

        int lineNo = 1;
        for (String productId : productIds) {
            CycleCountLine line = new CycleCountLine();
            line.setLineNo(lineNo++);
            line.setProductId(productId);
            // Expected on-hand is resolved by the caller and carried in scopeConfig
            // (see class doc); default to zero when absent so the count still opens.
            line.setQtyExpected(CycleCountScopeResolver.expectedFor(req.scopeConfig(), productId));
            line.setVariance(BigDecimal.ZERO);
            plan.addLine(line);
        }

        // Draft -> In Progress once lines exist (reference flips it immediately).
        plan.setStatus(LIFECYCLE.transition(CycleCountStatus.DRAFT, CycleCountStatus.IN_PROGRESS));
        CycleCountPlan saved = repository.save(plan);

        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public CycleCountResponse submitFirstPass(String publicId, List<LineCountRequest> counts) {
        CycleCountPlan plan = load(publicId);
        if (plan.getStatus() != CycleCountStatus.IN_PROGRESS) {
            throw new DomainException(ErrorCode.ILLEGAL_STATE_TRANSITION,
                "Cannot submit counts for a plan with status '" + plan.getStatus().wire() + "'.");
        }
        CycleCountResponse before = mapper.toResponse(plan);
        Map<String, BigDecimal> byLine = index(counts, LineCountRequest::lineId, LineCountRequest::qtyFirstPass);
        for (CycleCountLine line : plan.getLines()) {
            BigDecimal qty = byLine.get(line.getPublicId());
            if (qty != null) {
                BigDecimal variance = qty.subtract(line.getQtyExpected());
                line.setQtyFirstPass(qty);
                line.setVariance(variance);
                line.setRequiresRecount(variance.signum() != 0);
            }
        }
        CycleCountPlan saved = repository.save(plan);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public CycleCountResponse submitSecondPass(String publicId, List<LineSecondPassRequest> counts) {
        CycleCountPlan plan = load(publicId);
        if (plan.getStatus() != CycleCountStatus.IN_PROGRESS) {
            throw new DomainException(ErrorCode.ILLEGAL_STATE_TRANSITION,
                "Second pass requires a plan in 'In Progress'; current: '" + plan.getStatus().wire() + "'.");
        }
        CycleCountResponse before = mapper.toResponse(plan);
        Map<String, BigDecimal> byLine =
            index(counts, LineSecondPassRequest::lineId, LineSecondPassRequest::qtySecondPass);
        for (CycleCountLine line : plan.getLines()) {
            BigDecimal qty = byLine.get(line.getPublicId());
            // Only recount lines flagged in the first pass (reference guard).
            if (qty != null && line.isRequiresRecount()) {
                line.setQtySecondPass(qty);
                line.setVariance(qty.subtract(line.getQtyExpected()));
            }
        }
        plan.setStatus(LIFECYCLE.transition(CycleCountStatus.IN_PROGRESS, CycleCountStatus.COMPLETED));
        CycleCountPlan saved = repository.save(plan);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    @Transactional
    public CycleCountResponse approve(String publicId) {
        CycleCountPlan plan = load(publicId);
        if (plan.getStatus() != CycleCountStatus.COMPLETED) {
            throw new DomainException(ErrorCode.ILLEGAL_STATE_TRANSITION,
                "Cycle count must be in 'Completed' to approve.");
        }
        // Every line must have been counted in the first pass (reference QA C11).
        boolean uncounted = plan.getLines().stream().anyMatch(l -> l.getQtyFirstPass() == null);
        if (uncounted) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Every line must be counted in the first pass before approval.");
        }
        CycleCountResponse before = mapper.toResponse(plan);

        int total = plan.getLines().size();
        int zeroVariance = 0;
        for (CycleCountLine line : plan.getLines()) {
            if (line.getVariance().signum() == 0) {
                zeroVariance++;
                continue;
            }
            // Post the variance as a stock movement via the outbox — the ledger
            // slice consumes this rather than a hard cross-slice call.
            outbox.publish(AUDIT_ENTITY, plan.getPublicId(), EVENT_APPROVED, Map.of(
                "planId", plan.getPublicId(),
                "number", plan.getNumber(),
                "locationId", plan.getLocationId(),
                "productId", line.getProductId(),
                "variance", line.getVariance().toPlainString(),
                "sourceDocType", "CYCLE_COUNT"));
        }
        BigDecimal accuracy = total == 0 ? BigDecimal.ZERO
            : BigDecimal.valueOf(zeroVariance)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_EVEN);
        plan.setAccuracyPct(accuracy);
        plan.setStatus(LIFECYCLE.transition(CycleCountStatus.COMPLETED, CycleCountStatus.APPROVED));

        CycleCountPlan saved = repository.save(plan);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    // --- helpers -----------------------------------------------------------

    private CycleCountPlan load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("CycleCountPlan", publicId));
    }

    private String nextNumber() {
        long seq = repository.countByNumberStartingWith(NUMBER_PREFIX) + 1;
        return "%s%06d".formatted(NUMBER_PREFIX, seq);
    }

    private static <T> Map<String, BigDecimal> index(
            List<T> items,
            java.util.function.Function<T, String> keyFn,
            java.util.function.Function<T, BigDecimal> valFn) {
        return items.stream().collect(java.util.stream.Collectors.toMap(keyFn, valFn, (a, b) -> b));
    }
}
