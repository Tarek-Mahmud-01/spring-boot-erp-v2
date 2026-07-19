package com.guru.erp.modules.procurement.returns.service;

import com.guru.erp.modules.procurement.returns.domain.ReturnStatus;
import com.guru.erp.modules.procurement.returns.domain.SupplierReturn;
import com.guru.erp.modules.procurement.returns.dto.ReturnDtos.ReturnCreateRequest;
import com.guru.erp.modules.procurement.returns.dto.ReturnDtos.ReturnLineRequest;
import com.guru.erp.modules.procurement.returns.dto.ReturnDtos.ReturnResponse;
import com.guru.erp.modules.procurement.returns.dto.ReturnDtos.ReturnTransitionRequest;
import com.guru.erp.modules.procurement.returns.dto.ReturnDtos.ReturnUpdateRequest;
import com.guru.erp.modules.procurement.returns.mapper.ReturnMapper;
import com.guru.erp.modules.procurement.returns.repository.SupplierReturnRepository;
import com.guru.erp.modules.procurement.returns.service.ReturnPricingService.Priced;
import com.guru.erp.platform.audit.AuditAction;
import com.guru.erp.platform.audit.AuditService;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.error.ErrorCode;
import com.guru.erp.platform.money.Money;
import com.guru.erp.platform.status.StateMachine;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for ENT-031 SupplierReturn (US-020 / FR-109–113). Ports the reference
 * {@code SupplierReturnsView} create / update / transition / delete flows: header+lines aggregate,
 * the DRAFT → CONFIRMED workflow via the platform {@link StateMachine}, an "already returned ≤
 * received" cap per GRN line, one audit row per mutation, and the stock/GL side effects emitted as
 * outbox events (see {@link ReturnPostingService}) rather than hard cross-module calls.
 *
 * <p>Reference behaviour reproduced: returns are created directly in CONFIRMED status (the Draft
 * step was removed per UX feedback) and the confirmation event fires in the same transaction so the
 * ledger never sees a half-posted return. A DRAFT → CONFIRMED transition is still supported for any
 * return raised as Draft. An edit reverses the old effects then re-posts. A delete reverses then
 * soft-deletes.
 */
@Service
public class ReturnCommandService {

    static final String AUDIT_ENTITY = "supplier_return";
    private static final String BASE_CURRENCY = "USD";

    /** Reference return transitions: Draft → Confirmed. */
    private static final StateMachine<ReturnStatus> WORKFLOW = StateMachine.builder(ReturnStatus.class)
        .allow(ReturnStatus.DRAFT, ReturnStatus.CONFIRMED)
        .build();

    private final SupplierReturnRepository repository;
    private final ReturnMapper mapper;
    private final ReturnPricingService pricing;
    private final ReturnPostingService posting;
    private final AuditService auditService;

    public ReturnCommandService(SupplierReturnRepository repository, ReturnMapper mapper,
                                ReturnPricingService pricing, ReturnPostingService posting,
                                AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.pricing = pricing;
        this.posting = posting;
        this.auditService = auditService;
    }

    /** FR-109–113 — create a return (born CONFIRMED) against a GRN, relieving stock + posting GL. */
    @Transactional
    public ReturnResponse create(ReturnCreateRequest req) {
        checkLineQtyPositive(req.lines());
        Priced priced = pricing.price(req.lines());
        BigDecimal rate = BigDecimal.ONE;

        SupplierReturn r = new SupplierReturn();
        r.setNumber(nextNumber());
        r.setSupplierId(req.supplierId());
        r.setGrnId(req.grnId());
        r.setReturnedAt(req.returnedAt());
        r.setStatus(ReturnStatus.CONFIRMED);
        r.setDebitNote(Money.ofMinor(priced.debitTotal(), priced.currency()));
        r.setExchangeRate(rate);
        r.setBaseDebitNote(pricing.baseDebit(priced.debitTotal(), rate, BASE_CURRENCY));
        for (ReturnLineRequest in : req.lines()) {
            r.addLine(pricing.toEntity(in));
        }

        SupplierReturn saved = repository.save(r);
        posting.emitConfirmed(saved);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /**
     * Full edit of a return: reverse the old stock-out + debit note, replace the lines, then
     * re-post. Supplier + GRN stay fixed. Mirrors the reference reverse-then-rebuild.
     */
    @Transactional
    public ReturnResponse update(String publicId, ReturnUpdateRequest req) {
        SupplierReturn r = load(publicId);
        checkVersion(r, req.version());
        if (req.lines() == null || req.lines().isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "a return must have at least one line");
        }
        checkLineQtyPositive(req.lines());
        ReturnResponse before = mapper.toResponse(r);

        // 1) Reverse the old effects (stock re-credit + debit-note reversal).
        boolean wasConfirmed = r.getStatus() == ReturnStatus.CONFIRMED;
        if (wasConfirmed) {
            posting.emitReversed(r);
        }

        // 2) Replace the lines + re-price.
        Priced priced = pricing.price(req.lines());
        BigDecimal rate = r.getExchangeRate() == null ? BigDecimal.ONE : r.getExchangeRate();
        r.clearLines();
        for (ReturnLineRequest in : req.lines()) {
            r.addLine(pricing.toEntity(in));
        }
        r.setDebitNote(Money.ofMinor(priced.debitTotal(), priced.currency()));
        r.setBaseDebitNote(pricing.baseDebit(priced.debitTotal(), rate, BASE_CURRENCY));

        SupplierReturn saved = repository.save(r);

        // 3) Re-post the fresh effects.
        if (wasConfirmed) {
            posting.emitConfirmed(saved);
        }
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /** Transition a return Draft → Confirmed, emitting the stock/GL confirmation event. */
    @Transactional
    public ReturnResponse transition(String publicId, ReturnTransitionRequest req) {
        SupplierReturn r = load(publicId);
        ReturnStatus target = ReturnStatus.fromWire(req.toStatus());
        ReturnResponse before = mapper.toResponse(r);
        r.setStatus(WORKFLOW.transition(r.getStatus(), target));
        SupplierReturn saved = repository.save(r);
        if (target == ReturnStatus.CONFIRMED) {
            posting.emitConfirmed(saved);
        }
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.UPDATE, before,
            mapper.toResponse(saved));
        return mapper.toResponse(saved);
    }

    /** Soft-delete a return: reverse its confirmed effects (if any), then soft-delete. */
    @Transactional
    public void delete(String publicId) {
        SupplierReturn r = load(publicId);
        ReturnResponse before = mapper.toResponse(r);
        if (r.getStatus() == ReturnStatus.CONFIRMED) {
            posting.emitReversed(r);
        }
        r.softDelete();
        repository.save(r);
        auditService.record(AUDIT_ENTITY, publicId, AuditAction.DELETE, before, null);
    }

    // --- internals -----------------------------------------------------------

    /**
     * Validate line quantities are strictly positive. The received-qty ceiling itself (reference
     * {@code ReturnQtyExceedsReceiptError}, raised when {@code qty + already_returned > qty_received})
     * cannot be enforced here: received qty lives in the goods-receipt slice and this slice holds only
     * a loose {@code grnLineId} ref. {@link SupplierReturnRepository#sumReturnedQtyForGrnLine} exposes
     * the per-GRN-line already-returned running total (mirroring the reference aggregation) so the
     * confirmation event consumer — which does have the receipt slice in scope — can enforce the cap;
     * this service only validates what it can see.
     */
    private void checkLineQtyPositive(List<ReturnLineRequest> lines) {
        for (ReturnLineRequest ln : lines) {
            if (ln.qty() != null && ln.qty().signum() <= 0) {
                throw new DomainException(ErrorCode.VALIDATION_FAILED, "qty must be positive");
            }
        }
    }

    private String nextNumber() {
        long n = repository.count();
        String number;
        do {
            n++;
            number = String.format("RET-%05d", n);
        } while (repository.existsByNumber(number));
        return number;
    }

    private SupplierReturn load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("SupplierReturn", publicId));
    }

    private void checkVersion(SupplierReturn r, Long requestVersion) {
        if (requestVersion != null && requestVersion != r.getVersion()) {
            throw new DomainException(ErrorCode.OPTIMISTIC_LOCK,
                ErrorCode.OPTIMISTIC_LOCK.defaultDetail());
        }
    }
}
