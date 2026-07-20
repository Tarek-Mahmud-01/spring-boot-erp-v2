package com.springboot.erp.modules.inventory.movements.service;

import com.springboot.erp.modules.inventory.movements.domain.StockTransfer;
import com.springboot.erp.modules.inventory.movements.domain.StockTransferLine;
import com.springboot.erp.modules.inventory.movements.domain.TransferStatus;
import com.springboot.erp.modules.inventory.movements.dto.TransferDtos.TransferReceiveLineRequest;
import com.springboot.erp.modules.inventory.movements.dto.TransferDtos.TransferReceiveRequest;
import com.springboot.erp.modules.inventory.movements.dto.TransferDtos.TransferResponse;
import com.springboot.erp.modules.inventory.movements.mapper.MovementMapper;
import com.springboot.erp.modules.inventory.movements.repository.StockTransferRepository;
import com.springboot.erp.platform.audit.AuditAction;
import com.springboot.erp.platform.audit.AuditService;
import com.springboot.erp.platform.error.DomainException;
import com.springboot.erp.platform.error.ErrorCode;
import com.springboot.erp.platform.status.StateMachine;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Status-workflow use-cases for ENT-042 StockTransfer (AC-023-1/2): confirm (stocks out of source),
 * receive (lands stock at the destination), and one-shot complete. Split out of
 * {@link TransferCommandService} to keep each service focused. Uses the platform
 * {@link StateMachine} (reference TRANSFER_TRANSITIONS) and emits ledger movements via the outbox
 * (see {@link TransferPostingService}). One audit row per transition.
 */
@Service
public class TransferWorkflowService {

    static final StateMachine<TransferStatus> WORKFLOW = StateMachine.builder(TransferStatus.class)
        .allow(TransferStatus.DRAFT, TransferStatus.APPROVED)
        .allow(TransferStatus.APPROVED, TransferStatus.PARTIALLY_COMPLETE, TransferStatus.COMPLETE)
        .allow(TransferStatus.PARTIALLY_COMPLETE, TransferStatus.COMPLETE)
        .build();

    private final StockTransferRepository repository;
    private final MovementMapper mapper;
    private final AuditService auditService;
    private final TransferPostingService posting;
    private final Clock clock = Clock.systemUTC();

    public TransferWorkflowService(StockTransferRepository repository, MovementMapper mapper,
                                   AuditService auditService, TransferPostingService posting) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.posting = posting;
    }

    @Transactional
    public TransferResponse confirm(String publicId) {
        StockTransfer t = load(publicId);
        confirmInternal(t);
        return mapper.toResponse(t);
    }

    @Transactional
    public TransferResponse receive(String publicId, TransferReceiveRequest req) {
        StockTransfer t = load(publicId);
        if (t.getStatus() != TransferStatus.APPROVED
            && t.getStatus() != TransferStatus.PARTIALLY_COMPLETE) {
            throw new DomainException(ErrorCode.CONFLICT,
                "Transfer must be Approved before it can be received");
        }
        TransferResponse before = mapper.toResponse(t);
        applyReceipts(t, req.lines());

        boolean fullyReceived = t.getLines().stream().allMatch(l ->
            l.getQtyReceived().add(l.getQtyShort()).add(l.getQtyDamaged())
                .compareTo(l.getQtySent()) >= 0);
        TransferStatus target = fullyReceived
            ? TransferStatus.COMPLETE : TransferStatus.PARTIALLY_COMPLETE;
        t.setStatus(WORKFLOW.transition(t.getStatus(), target));
        if (target == TransferStatus.COMPLETE) {
            t.setReceivedAt(Instant.now(clock));
        }
        repository.save(t);
        auditService.record(TransferCommandService.AUDIT_ENTITY, t.getPublicId(),
            AuditAction.UPDATE, before, mapper.toResponse(t));
        posting.emitReceived(t);
        return mapper.toResponse(t);
    }

    @Transactional
    public TransferResponse complete(String publicId) {
        StockTransfer t = load(publicId);
        if (t.getStatus() == TransferStatus.DRAFT) {
            confirmInternal(t);
        }
        receiveCleanInternal(t);
        return mapper.toResponse(t);
    }

    /** Confirm: DRAFT → APPROVED, stamps confirmed_at, emits TRANSFER_OUT. */
    void confirmInternal(StockTransfer t) {
        t.setStatus(WORKFLOW.transition(t.getStatus(), TransferStatus.APPROVED));
        t.setConfirmedAt(Instant.now(clock));
        repository.save(t);
        auditService.record(TransferCommandService.AUDIT_ENTITY, t.getPublicId(),
            AuditAction.UPDATE, null, mapper.toResponse(t));
        posting.emitConfirmed(t);
    }

    /** Clean receive used by auto-complete/complete: every sent qty arrives intact. */
    void receiveCleanInternal(StockTransfer t) {
        for (StockTransferLine l : t.getLines()) {
            l.setQtyReceived(l.getQtySent());
            l.setQtyShort(BigDecimal.ZERO);
            l.setQtyDamaged(BigDecimal.ZERO);
        }
        t.setStatus(WORKFLOW.transition(t.getStatus(), TransferStatus.COMPLETE));
        t.setReceivedAt(Instant.now(clock));
        repository.save(t);
        auditService.record(TransferCommandService.AUDIT_ENTITY, t.getPublicId(),
            AuditAction.UPDATE, null, mapper.toResponse(t));
        posting.emitReceived(t);
    }

    private void applyReceipts(StockTransfer t, List<TransferReceiveLineRequest> receipts) {
        Map<String, StockTransferLine> byPid = t.getLines().stream()
            .collect(Collectors.toMap(StockTransferLine::getPublicId, l -> l));
        for (TransferReceiveLineRequest r : receipts) {
            StockTransferLine line = byPid.get(r.lineId());
            if (line == null) {
                throw DomainException.notFound("StockTransferLine", r.lineId());
            }
            line.setQtyReceived(nz(r.qtyReceived()));
            line.setQtyShort(nz(r.qtyShort()));
            line.setQtyDamaged(nz(r.qtyDamaged()));
            line.setDiscrepancyReason(r.discrepancyReason());
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private StockTransfer load(String publicId) {
        return repository.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("StockTransfer", publicId));
    }
}
