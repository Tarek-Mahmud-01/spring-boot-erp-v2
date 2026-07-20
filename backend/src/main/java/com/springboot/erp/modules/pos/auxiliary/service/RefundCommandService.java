package com.springboot.erp.modules.pos.auxiliary.service;

import com.springboot.erp.modules.pos.auxiliary.domain.ManagerApprovalMethod;
import com.springboot.erp.modules.pos.auxiliary.domain.PosRefund;
import com.springboot.erp.modules.pos.auxiliary.domain.RefundMode;
import com.springboot.erp.modules.pos.auxiliary.domain.RefundPricedFrom;
import com.springboot.erp.modules.pos.auxiliary.dto.RefundDtos.RefundNoReceiptRequest;
import com.springboot.erp.modules.pos.auxiliary.dto.RefundDtos.RefundReceiptLinkedRequest;
import com.springboot.erp.modules.pos.auxiliary.dto.RefundDtos.RefundResponse;
import com.springboot.erp.modules.pos.auxiliary.mapper.PosAuxMapper;
import com.springboot.erp.modules.pos.auxiliary.repository.PosRefundRepository;
import com.springboot.erp.modules.pos.registers.service.ManagerStepUpAuthorizer;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.money.Money;
import com.springboot.erp.platform.outbox.OutboxPublisher;
import com.springboot.erp.platform.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side use-cases for PosRefund (US-034 FR-177..180) — port of the reference
 * {@code refund_receipt_linked} / {@code refund_no_receipt} / {@code refund_no_receipt_online} /
 * {@code _finalise_refund} views.
 *
 * <p>The refund's own REFUND-type transaction, its lines, and the payout tender are owned by the
 * (not-yet-ported) PosTransaction aggregate in another sub-slice — per the vertical-slice rule this
 * service never writes those tables directly. The caller supplies the already-built REFUND
 * transaction's identity + totals (mirrors the reference's {@code _new_refund_txn} /
 * {@code _add_refund_line} / {@code _finalise_refund} tail, which this service's outbox event
 * asks the transactions slice + inventory/GL consumers to apply): this service owns only the
 * {@link PosRefund} metadata row, the manager step-up gate, the audit trail, and the
 * {@code pos.refund.created} outbox event that fans out the inventory restock + GL reversal.
 */
@Service
public class RefundCommandService {

    static final String AUDIT_ENTITY = "pos_refund";
    static final String EVENT_REFUND_CREATED = "pos.refund.created";
    static final String AGGREGATE = "pos_refund";

    private final PosRefundRepository repository;
    private final PosAuxMapper mapper;
    private final AuditService auditService;
    private final OutboxPublisher outbox;
    private final ManagerStepUpAuthorizer managerAuthorizer;
    private final CurrentUser currentUser;
    private final Clock clock = Clock.systemUTC();

    public RefundCommandService(PosRefundRepository repository, PosAuxMapper mapper,
                                AuditService auditService, OutboxPublisher outbox,
                                ManagerStepUpAuthorizer managerAuthorizer, CurrentUser currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.outbox = outbox;
        this.managerAuthorizer = managerAuthorizer;
        this.currentUser = currentUser;
    }

    /**
     * US-034 FR-177 — refund selected lines against the original receipt. The caller has already
     * validated the original transaction is a COMPLETED SALE, resolved/validated the refund payment
     * method (reference {@code _check_refund_method}), and built the REFUND transaction + its
     * lines; this call persists the {@link PosRefund} metadata and fans out the posting event.
     */
    @Transactional
    public RefundResponse refundReceiptLinked(RefundReceiptLinkedRequest request,
                                              RefundPostingContext ctx) {
        if (repository.findByTransactionId(ctx.refundTransactionId()).isPresent()) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "A refund already exists for transaction " + ctx.refundTransactionId());
        }
        PosRefund refund = finalise(ctx, RefundMode.RECEIPT_LINKED, RefundPricedFrom.ORIGINAL,
            request.originalTransactionId(), request.reason(), null, null);
        return mapper.toResponse(refund);
    }

    /**
     * US-034 FR-178 — refund with no original receipt, gated by a VERIFIED manager approval. When
     * {@code request.managerUsername()}/{@code managerPassword()} are supplied, the manager is
     * authenticated live and must hold {@code pos.refund.no_receipt}; when omitted (offline-replay
     * path) the acting user is treated as the (already-permission-checked) approver, matching the
     * reference's {@code manager is None -> approver = actor} fallback.
     */
    @Transactional
    public RefundResponse refundNoReceipt(RefundNoReceiptRequest request, RefundPostingContext ctx,
                                          boolean requireOnlineManagerStepUp) {
        if (repository.findByTransactionId(ctx.refundTransactionId()).isPresent()) {
            throw new DomainException(ErrorCode.DUPLICATE,
                "A refund already exists for transaction " + ctx.refundTransactionId());
        }
        String approverId;
        ManagerApprovalMethod method;
        if (request.managerUsername() != null && request.managerPassword() != null) {
            approverId = managerAuthorizer.authorize(
                request.managerUsername(), request.managerPassword(), PERMISSION_REFUND_NO_RECEIPT);
            method = ManagerApprovalMethod.PASSWORD;
        } else if (requireOnlineManagerStepUp) {
            throw new DomainException(ErrorCode.VALIDATION_FAILED,
                "Manager credentials are required to approve a no-receipt refund");
        } else {
            approverId = currentUser.optional().map(p -> p.userPublicId()).orElse(null);
            method = ctx.offline() ? ManagerApprovalMethod.OFFLINE_OPERATOR : null;
        }
        RefundPricedFrom pricedFrom = ctx.usedLowest30d()
            ? RefundPricedFrom.LOWEST_30D : RefundPricedFrom.MANAGER_ENTERED;
        PosRefund refund = finalise(ctx, RefundMode.NO_RECEIPT, pricedFrom, null, request.reason(),
            approverId, method);
        return mapper.toResponse(refund);
    }

    private static final String PERMISSION_REFUND_NO_RECEIPT = "pos.refund.no_receipt";

    private PosRefund finalise(RefundPostingContext ctx, RefundMode mode, RefundPricedFrom pricedFrom,
                               String originalTransactionId, String reason, String managerId,
                               ManagerApprovalMethod managerMethod) {
        Instant now = Instant.now(clock);
        PosRefund refund = new PosRefund();
        refund.setTransactionId(ctx.refundTransactionId());
        refund.setOriginalTransactionId(originalTransactionId);
        refund.setMode(mode);
        refund.setPricedFrom(pricedFrom);
        refund.setManagerApprovalBy(managerId);
        refund.setManagerApprovalAt(managerId != null ? now : null);
        refund.setManagerApprovalMethod(managerId != null ? managerMethod : null);
        refund.setTotalRefund(Money.ofMinor(ctx.totalRefundAmount(), ctx.currency()));
        refund.setReason(reason);

        PosRefund saved = repository.save(refund);

        Map<String, Object> payload = buildPayload(saved, ctx);
        auditService.record(AUDIT_ENTITY, saved.getPublicId(), AuditAction.CREATE, null, payload);
        outbox.publish(AGGREGATE, saved.getPublicId(), EVENT_REFUND_CREATED, payload);
        return saved;
    }

    private Map<String, Object> buildPayload(PosRefund refund, RefundPostingContext ctx) {
        return Map.ofEntries(
            Map.entry("refundId", refund.getPublicId()),
            Map.entry("transactionId", ctx.refundTransactionId()),
            Map.entry("originalTransactionId", refund.getOriginalTransactionId() == null ? "" : refund.getOriginalTransactionId()),
            Map.entry("mode", refund.getMode().name()),
            Map.entry("totalRefundAmount", ctx.totalRefundAmount()),
            Map.entry("subtotalRefundAmount", ctx.subtotalRefundAmount()),
            Map.entry("currency", ctx.currency()),
            Map.entry("managerApprovalBy", refund.getManagerApprovalBy() == null ? "" : refund.getManagerApprovalBy()),
            Map.entry("managerApprovalMethod", refund.getManagerApprovalMethod() == null ? "" : refund.getManagerApprovalMethod().name()),
            Map.entry("customerPublicId", ctx.customerPublicId() == null ? "" : ctx.customerPublicId()),
            Map.entry("locationPublicId", ctx.locationId()),
            Map.entry("registerPublicId", ctx.registerId()),
            Map.entry("cashierPublicId", ctx.cashierId() == null ? "" : ctx.cashierId()),
            Map.entry("entryDate", now().toString()),
            Map.entry("lines", ctx.lines() == null ? List.of() : ctx.lines()));
    }

    private Instant now() {
        return Instant.now(clock);
    }

    /**
     * Everything the (not-yet-ported) PosTransaction/lines aggregate has already computed for the
     * REFUND transaction by the time this service is called: totals, party ids, and the line-level
     * detail the outbox payload fans out to inventory/GL. Assembled by the caller (the transactions
     * slice's refund workflow, once built) — mirrors the reference's {@code _finalise_refund} tail
     * inputs.
     */
    public record RefundPostingContext(
        String refundTransactionId,
        String registerId,
        String locationId,
        String cashierId,
        String customerPublicId,
        long totalRefundAmount,
        long subtotalRefundAmount,
        String currency,
        boolean offline,
        boolean usedLowest30d,
        List<Map<String, Object>> lines
    ) {
    }
}
