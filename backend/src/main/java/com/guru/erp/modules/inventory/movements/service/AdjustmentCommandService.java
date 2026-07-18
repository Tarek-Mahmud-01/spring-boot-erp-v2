package com.guru.erp.modules.inventory.movements.service;

import com.guru.erp.modules.inventory.movements.domain.AdjustmentStatus;
import com.guru.erp.modules.inventory.movements.domain.StockAdjustment;
import com.guru.erp.modules.inventory.movements.domain.StockAdjustmentLine;
import com.guru.erp.modules.inventory.movements.dto.AdjustmentDtos.AdjustmentCreateRequest;
import com.guru.erp.modules.inventory.movements.dto.AdjustmentDtos.AdjustmentLineRequest;
import com.guru.erp.modules.inventory.movements.dto.AdjustmentDtos.AdjustmentResponse;
import com.guru.erp.modules.inventory.movements.dto.AdjustmentDtos.AdjustmentUpdateRequest;
import com.guru.erp.modules.inventory.movements.mapper.MovementMapper;
import com.guru.erp.modules.inventory.movements.repository.StockAdjustmentRepository;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.id.Ulid;
import com.guru.erp.platform.money.Money;
import com.guru.erp.platform.security.CurrentUser;
import com.guru.erp.platform.status.StateMachine;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-043 StockAdjustment (US-024 / FR-129–133). Ports the reference
 * create / update / approve / post / delete flows: header+lines aggregate, the
 * DRAFT → PENDING_APPROVAL/APPROVED → POSTED → REVERSED workflow via the platform
 * {@link StateMachine}, one audit row per mutation, and a ledger-movement outbox event on post
 * (handled by {@link AdjustmentPostingService}).
 */
@Service
public class AdjustmentCommandService {

    static final String AUDIT_ENTITY = "stock_adjustment";
    private static final String DEFAULT_CURRENCY = "USD";

    /** Reference ADJUSTMENT_TRANSITIONS. */
    private static final StateMachine<AdjustmentStatus> WORKFLOW = StateMachine.builder(AdjustmentStatus.class)
        .allow(AdjustmentStatus.DRAFT, AdjustmentStatus.PENDING_APPROVAL, AdjustmentStatus.APPROVED)
        .allow(AdjustmentStatus.PENDING_APPROVAL, AdjustmentStatus.APPROVED)
        .allow(AdjustmentStatus.APPROVED, AdjustmentStatus.POSTED)
        .allow(AdjustmentStatus.POSTED, AdjustmentStatus.REVERSED)
        .build();

    private final StockAdjustmentRepository repository;
    private final MovementMapper mapper;
    private final AuditService auditService;
    private final AdjustmentPostingService posting;
    private final CurrentUser currentUser;
    private final Clock clock = Clock.systemUTC();

    public AdjustmentCommandService(StockAdjustmentRepository repository, MovementMapper mapper,
                                    AuditService auditService, AdjustmentPostingService posting,
                                    CurrentUser currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.posting = posting;
        this.currentUser = currentUser;
    }

    @Transactional
    public AdjustmentResponse create(AdjustmentCreateRequest req) {
        validateLines(req.lines());
        StockAdjustment a = new StockAdjustment();
        a.setNumber(generateNumber());
        a.setLocationId(req.locationId());
        a.setReason(req.reason().strip());
        a.setNotes(req.notes());
        a.setVarianceAccountId(req.varianceAccountId());
        a.setStatus(AdjustmentStatus.DRAFT);
        applyLines(a, req.lines());

        StockAdjustment saved = repository.save(a);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));

        // Auto-complete: approve then post, unless the threshold gate holds it in PENDING_APPROVAL.
        if (req.autoComplete() && !saved.isThresholdExceeded()) {
            approveInternal(saved);
            postInternal(saved);
        }
        return mapper.toResponse(saved);
    }

    @Transactional
    public AdjustmentResponse update(String publicId, AdjustmentUpdateRequest req) {
        StockAdjustment a = load(publicId);
        checkVersion(a, req.version());
        if (a.getStatus() != AdjustmentStatus.DRAFT) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Only a Draft adjustment can be edited; this one is " + a.getStatus().wire());
        }
        validateLines(req.lines());
        AdjustmentResponse before = mapper.toResponse(a);

        a.setLocationId(req.locationId());
        a.setReason(req.reason().strip());
        a.setNotes(req.notes());
        a.setVarianceAccountId(req.varianceAccountId());
        a.clearLines();
        applyLines(a, req.lines());

        StockAdjustment saved = repository.save(a);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /** FR-131 — approve: DRAFT/PENDING_APPROVAL → APPROVED, stamps approver + timestamp. */
    @Transactional
    public AdjustmentResponse approve(String publicId) {
        StockAdjustment a = load(publicId);
        approveInternal(a);
        return mapper.toResponse(a);
    }

    /** FR-132 — post: APPROVED → POSTED, writes ledger movements (via outbox) + a GL journal. */
    @Transactional
    public AdjustmentResponse post(String publicId) {
        StockAdjustment a = load(publicId);
        postInternal(a);
        return mapper.toResponse(a);
    }

    /** Delete: an unposted adjustment is soft-deleted; a POSTED one is reversed then soft-deleted. */
    @Transactional
    public void delete(String publicId) {
        StockAdjustment a = load(publicId);
        AdjustmentResponse before = mapper.toResponse(a);
        if (a.getStatus() == AdjustmentStatus.POSTED) {
            a.setStatus(WORKFLOW.transition(a.getStatus(), AdjustmentStatus.REVERSED));
            posting.emitReversed(a);
        }
        a.softDelete();
        repository.save(a);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    // --- internals -----------------------------------------------------------

    private void approveInternal(StockAdjustment a) {
        a.setStatus(WORKFLOW.transition(a.getStatus(), AdjustmentStatus.APPROVED));
        a.setApproverId(currentUser.optional().map(p -> p.userPublicId()).orElse(null));
        a.setApprovedAt(Instant.now(clock));
        repository.save(a);
        auditService.record(AUDIT_ENTITY, a.getPublicId(), AuditAction.UPDATE, null,
            mapper.toResponse(a));
    }

    private void postInternal(StockAdjustment a) {
        if (a.getVarianceAccountId() == null) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "variance_account_id is required to post an adjustment");
        }
        a.setStatus(WORKFLOW.transition(a.getStatus(), AdjustmentStatus.POSTED));
        a.setPostedAt(Instant.now(clock));
        repository.save(a);
        auditService.record(AUDIT_ENTITY, a.getPublicId(), AuditAction.UPDATE, null,
            mapper.toResponse(a));
        posting.emitPosted(a);
    }

    private void applyLines(StockAdjustment a, List<AdjustmentLineRequest> lines) {
        int lineNo = 1;
        for (AdjustmentLineRequest in : lines) {
            String ccy = in.unitCostCurrency() == null || in.unitCostCurrency().isBlank()
                ? DEFAULT_CURRENCY : in.unitCostCurrency().toUpperCase();
            StockAdjustmentLine line = new StockAdjustmentLine();
            line.setLineNo(lineNo++);
            line.setProductId(in.productId());
            line.setUomId(in.uomId());
            line.setVariantId(in.variantId());
            line.setWriteOffReason(in.writeOffReason());
            // qty_delta IS the variance. qty_counted is derived at post time from live on-hand;
            // stored as the delta here for audit continuity until the stock slice supplies on-hand.
            line.setQtyVariance(in.qtyDelta());
            line.setQtyCounted(in.qtyDelta().max(BigDecimal.ZERO));
            line.setQtyOnHandAtCount(BigDecimal.ZERO);
            line.setUnitCost(Money.ofMinor(in.unitCostAmount(), ccy));
            a.addLine(line);
        }
    }

    private void validateLines(List<AdjustmentLineRequest> lines) {
        Set<String> seen = new HashSet<>();
        for (AdjustmentLineRequest ln : lines) {
            // Reference _delta_non_zero — an adjustment must change stock.
            if (ln.qtyDelta() == null || ln.qtyDelta().signum() == 0) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED,
                    "qty_delta must be non-zero");
            }
            // Reference _no_duplicate_products.
            if (!seen.add(ln.productId())) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED,
                    "Each product can appear only once in an adjustment: " + ln.productId());
            }
        }
    }

    private String generateNumber() {
        String number;
        do {
            number = "ADJ-" + Ulid.next().substring(16);
        } while (repository.existsByNumber(number));
        return number;
    }

    private StockAdjustment load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("StockAdjustment", publicId));
    }

    private void checkVersion(StockAdjustment a, Long requestVersion) {
        if (requestVersion != null && requestVersion != a.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }
}
